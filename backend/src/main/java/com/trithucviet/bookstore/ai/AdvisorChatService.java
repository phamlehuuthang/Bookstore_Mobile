package com.trithucviet.bookstore.ai;

import com.trithucviet.bookstore.config.AiConfig;
import com.trithucviet.bookstore.dto.request.ChatRequest;
import com.trithucviet.bookstore.dto.response.BookDto;
import com.trithucviet.bookstore.dto.response.ChatResponse;
import com.trithucviet.bookstore.dto.response.VoucherDto;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Bộ điều phối Trợ lý Mua sắm AI đa năng (Bước 10 nâng cấp — 10 Agentic Tools +
 * Auto-Order 1 bước).
 *
 * Hỗ trợ đầy đủ trên cả 2 tầng:
 * 1. Tầng chính: Google Gemini REST API + Multi-Tool Function Calling (cascade
 * đa model tự phục hồi).
 * 2. Tầng dự phòng thông minh: Smart DB Fallback xử lý đồng nhất 100% nghiệp vụ
 * đặt sách ngay, thanh toán giỏ,
 * tự săn voucher hời nhất, quản lý giỏ/yêu thích, tra cứu/hủy đơn, đề xuất cá
 * nhân hóa, Flash Sale, combo & review.
 */
@Service
public class AdvisorChatService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorChatService.class);
    private static final int MAX_TOOL_ROUNDS = 5;
    private static final Pattern BUDGET_K_PATTERN = Pattern
            .compile("(?:duoi|<=|tam|khoang|gia|ngan sach|toi da|khong qua|re)?\\s*(\\d{1,4})\\s*(?:k|ngan|nghin|ng)\\b");
    private static final Pattern BUDGET_VND_PATTERN = Pattern
            .compile("(?:duoi|<=|tam|khoang|gia|ngan sach|toi da|khong qua)\\s*(\\d{1,3})[.,](\\d{3})(?:\\s*(?:d|dong|vnd))?");
    private static final Pattern BUDGET_PLAIN_VND_PATTERN = Pattern
            .compile("(?:duoi|<=|tam|khoang|gia|ngan sach|toi da|khong qua)\\s*(\\d{5,7})\\s*(?:d|dong|vnd)?\\b");
    private static final Pattern BUDGET_TRIEU_PATTERN = Pattern
            .compile("(?:duoi|<=|tam|khoang|gia|ngan sach|toi da|khong qua)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:trieu|tr)\\b");
    private static final Pattern BOOK_ID_TAG_PATTERN = Pattern.compile("\\[ID:\\s*(sach_\\w+)\\]|(sach_\\w+)");
    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("(DH-[A-Z0-9]{4,12})", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOUCHER_CODE_PATTERN = Pattern
            .compile("\\b(CHAOBAN20|SACHHAY15|FREESHIP30|CONGNGHE50|[A-Z]{4,12}\\d{2,4})\\b");
    private static final Pattern BOLD_TITLE_PATTERN = Pattern.compile("\\*\\*([^*]{3,90})\\*\\*");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+)\\s*(?:cuon|quyen)");

    private final AiConfig aiConfig;
    private final RestClient geminiRestClient;
    private final BookstoreAiTools aiTools;

    public AdvisorChatService(
            AiConfig aiConfig,
            @Qualifier("geminiRestClient") RestClient geminiRestClient,
            BookstoreAiTools aiTools) {
        this.aiConfig = aiConfig;
        this.geminiRestClient = geminiRestClient;
        this.aiTools = aiTools;
    }

    public ChatResponse ask(String nullableNguoiDungId, ChatRequest request) {
        if (aiConfig.isConfigured()) {
            for (String candidateModel : aiConfig.getCandidateChatModels()) {
                try {
                    ChatResponse geminiRes = askWithGeminiModel(candidateModel, nullableNguoiDungId, request);
                    if (geminiRes != null) {
                        return geminiRes;
                    }
                } catch (Exception ex) {
                    log.warn("Model Gemini '{}' tạm thời không phản hồi ({}), thử model kế tiếp...",
                            candidateModel, ex.getMessage());
                }
            }
            log.warn("Tất cả model Gemini đều tạm bận, chuyển sang Smart DB Fallback.");
        }
        return handleSmartFallback(nullableNguoiDungId, request);
    }

    // =========================================================================
    // TẦNG 1: GOOGLE GEMINI REST API + 10 AGENTIC FUNCTION CALLING TOOLS
    // =========================================================================

    @SuppressWarnings("unchecked")
    private ChatResponse askWithGeminiModel(String modelName, String nullableNguoiDungId, ChatRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (request.history() != null) {
            int start = Math.max(0, request.history().size() - 8);
            for (int i = start; i < request.history().size(); i++) {
                ChatRequest.HistoryMessage h = request.history().get(i);
                if (h == null || h.text() == null || h.text().isBlank())
                    continue;
                String role = "USER".equalsIgnoreCase(h.sender()) ? "user" : "model";
                contents.add(Map.of("role", role, "parts", List.of(Map.of("text", h.text()))));
            }
        }

        String userText = request.message().trim();
        String recentBookRef = extractPendingBookIdFromHistory(request.history());
        if (recentBookRef == null) {
            recentBookRef = extractRecentBookTitleFromHistory(request.history());
        }
        String contextHint = "[Ngữ cảnh hệ thống: khách "
                + (nullableNguoiDungId != null && !nullableNguoiDungId.isBlank() ? "ĐÃ đăng nhập"
                        : "CHƯA đăng nhập (khách vãng lai)")
                + (recentBookRef != null ? ", sách gần nhất trong hội thoại: " + recentBookRef : "")
                + "]\nTin nhắn của khách: " + userText;
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", contextHint))));

        List<BookDto> collectedBooks = new ArrayList<>();
        boolean cartUpdated = false;
        boolean wishlistUpdated = false;
        boolean notificationsUpdated = false;
        String createdOrderCode = null;

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("system_instruction", Map.of("parts", List.of(Map.of("text", AiConfig.SYSTEM_PROMPT))));
            body.put("contents", contents);
            body.put("tools", List.of(Map.of("functionDeclarations", buildGeminiFunctionDeclarations())));
            if (round == MAX_TOOL_ROUNDS - 1) {
                body.put("tool_config", Map.of("function_calling_config", Map.of("mode", "NONE")));
            }

            Map<String, Object> response = geminiRestClient.post()
                    .uri("/models/{model}:generateContent?key={key}", modelName, aiConfig.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> modelContent = extractFirstCandidateContent(response);
            if (modelContent == null) {
                return null;
            }
            contents.add(modelContent);

            List<Map<String, Object>> parts = (List<Map<String, Object>>) modelContent.get("parts");
            if (parts == null || parts.isEmpty()) {
                return null;
            }

            List<Map<String, Object>> functionResponseParts = new ArrayList<>();
            StringBuilder textReply = new StringBuilder();

            for (Map<String, Object> part : parts) {
                boolean isInternalThought = Boolean.TRUE.equals(part.get("thought"));
                if (!isInternalThought && part.get("text") instanceof String txt && !txt.isBlank()) {
                    if (!textReply.isEmpty())
                        textReply.append('\n');
                    textReply.append(txt.trim());
                }
                if (part.get("functionCall") instanceof Map<?, ?> fcRaw) {
                    String fnName = String.valueOf(fcRaw.get("name"));
                    Map<String, Object> args = fcRaw.get("args") instanceof Map<?, ?> m
                            ? (Map<String, Object>) m
                            : Map.of();

                    Map<String, Object> toolOutput = executeTool(
                            fnName,
                            args,
                            nullableNguoiDungId,
                            collectedBooks);

                    if (Boolean.TRUE.equals(toolOutput.get("cartMutated"))
                            || Boolean.TRUE.equals(toolOutput.get("fromCart"))) {
                        cartUpdated = true;
                    }
                    if (Boolean.TRUE.equals(toolOutput.get("wishlistMutated"))) {
                        wishlistUpdated = true;
                    }
                    if ("placeOrderOrCheckoutForCustomer".equals(fnName)
                            && Boolean.TRUE.equals(toolOutput.get("success"))) {
                        createdOrderCode = String.valueOf(toolOutput.get("orderCode"));
                        notificationsUpdated = true;
                    }
                    if ("lookupOrCancelCustomerOrder".equals(fnName)
                            && Boolean.TRUE.equals(toolOutput.get("cancelledOrder"))) {
                        notificationsUpdated = true;
                    }

                    Map<String, Object> fnRespPayload = new LinkedHashMap<>();
                    if (fcRaw.get("id") != null) {
                        fnRespPayload.put("id", String.valueOf(fcRaw.get("id")));
                    }
                    fnRespPayload.put("name", fnName);
                    fnRespPayload.put("response", toolOutput);

                    functionResponseParts.add(Map.of("functionResponse", fnRespPayload));
                }
            }

            if (functionResponseParts.isEmpty()) {
                String finalReply = !textReply.isEmpty()
                        ? textReply.toString()
                        : "Mình đã thực hiện yêu cầu cho bạn. Bạn cần mình hỗ trợ gì thêm không?";
                List<BookDto> finalBooks = deduplicateBooks(collectedBooks);
                List<String> chips = buildDynamicChips(finalBooks, cartUpdated, createdOrderCode);
                return new ChatResponse(
                        finalReply,
                        finalBooks,
                        cartUpdated,
                        wishlistUpdated,
                        notificationsUpdated,
                        createdOrderCode,
                        chips,
                        "GEMINI");
            }

            contents.add(Map.of("role", "user", "parts", functionResponseParts));
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeTool(
            String fnName,
            Map<String, Object> args,
            String nullableNguoiDungId,
            List<BookDto> collectedBooks) {
        return switch (fnName) {
            case "searchBooksBySemanticOrAuthor" -> {
                String query = optStr(args, "query", "");
                BigDecimal maxPrice = parseOptionalDecimal(args.get("maxPrice"));
                int limit = parseOptionalInt(args.get("limit"), 4);
                List<BookDto> books = aiTools.searchBooksBySemanticOrAuthor(query, maxPrice, limit);
                collectedBooks.addAll(books);
                yield Map.of("count", books.size(), "books", compactBookList(books));
            }
            case "getFlashSaleOrBestsellerBooks" -> {
                String mode = optStr(args, "mode", "FLASH_SALE");
                int limit = parseOptionalInt(args.get("limit"), 4);
                List<BookDto> books = aiTools.getFlashSaleOrBestsellerBooks(mode, limit);
                collectedBooks.addAll(books);
                yield Map.of("mode", mode, "count", books.size(), "books", compactBookList(books));
            }
            case "getPersonalizedRecommendations" -> {
                Map<String, Object> rec = aiTools.getPersonalizedRecommendations(nullableNguoiDungId);
                if (rec.get("books") instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof BookDto b)
                            collectedBooks.add(b);
                    }
                }
                Map<String, Object> clean = new LinkedHashMap<>(rec);
                clean.put("books", compactBookList((List<BookDto>) rec.getOrDefault("books", List.of())));
                yield clean;
            }
            case "suggestBookComboByBudget" -> {
                String topic = optStr(args, "topicOrQuery", "");
                BigDecimal budget = parseOptionalDecimal(args.get("budgetVnd"));
                int count = parseOptionalInt(args.get("itemCount"), 3);
                Map<String, Object> combo = aiTools.suggestBookComboByBudget(topic, budget, count);
                List<BookDto> books = (List<BookDto>) combo.getOrDefault("books", List.of());
                collectedBooks.addAll(books);
                Map<String, Object> clean = new LinkedHashMap<>(combo);
                clean.put("books", compactBookList(books));
                yield clean;
            }
            case "compareBooksOrReadReviews" -> {
                String q1 = optStr(args, "bookQuery1", "");
                String q2 = optStr(args, "bookQuery2", "");
                Map<String, Object> cmp = aiTools.compareBooksOrReadReviews(q1, q2);
                List<BookDto> books = (List<BookDto>) cmp.getOrDefault("books", List.of());
                collectedBooks.addAll(books);
                Map<String, Object> clean = new LinkedHashMap<>(cmp);
                clean.remove("books");
                yield clean;
            }
            case "getActiveVouchersAndDefaultAddress" ->
                aiTools.getActiveVouchersAndDefaultAddress(nullableNguoiDungId);
            case "manageCustomerCart" -> {
                String action = optStr(args, "action", "VIEW");
                String sachId = optStr(args, "sachIdOrTitle", "");
                int qty = parseOptionalInt(args.get("soLuong"), 1);
                Map<String, Object> res = new LinkedHashMap<>(
                        aiTools.manageCustomerCart(nullableNguoiDungId, action, sachId, qty));
                if (res.remove("book") instanceof BookDto b) {
                    collectedBooks.add(b);
                }
                yield res;
            }
            case "manageCustomerWishlist" -> {
                String action = optStr(args, "action", "VIEW");
                String sachId = optStr(args, "sachIdOrTitle", "");
                Map<String, Object> res = new LinkedHashMap<>(
                        aiTools.manageCustomerWishlist(nullableNguoiDungId, action, sachId));
                if (res.remove("book") instanceof BookDto b) {
                    collectedBooks.add(b);
                }
                if (res.remove("books") instanceof List<?> bl) {
                    for (Object o : bl) {
                        if (o instanceof BookDto b)
                            collectedBooks.add(b);
                    }
                }
                yield res;
            }
            case "placeOrderOrCheckoutForCustomer" -> {
                String mode = optStr(args, "mode", "DIRECT_BOOK");
                String sachId = optStr(args, "sachIdOrTitle", "");
                int qty = parseOptionalInt(args.get("soLuong"), 1);
                String voucherCode = optStr(args, "voucherCode", "");
                Map<String, Object> res = new LinkedHashMap<>(
                        aiTools.placeOrderOrCheckoutForCustomer(nullableNguoiDungId, mode, sachId, qty, voucherCode));
                if (res.remove("book") instanceof BookDto b) {
                    collectedBooks.add(b);
                }
                yield res;
            }
            case "lookupOrCancelCustomerOrder" -> {
                String action = optStr(args, "action", "LIST_RECENT");
                String orderRef = optStr(args, "orderCodeOrId", "");
                String reason = optStr(args, "cancelReason", "");
                yield aiTools.lookupOrCancelCustomerOrder(nullableNguoiDungId, action, orderRef, reason);
            }
            default -> Map.of("error", "Không tìm thấy công cụ: " + fnName);
        };
    }

    private List<Map<String, Object>> compactBookList(List<BookDto> books) {
        return books.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sachId", b.id());
            m.put("tieuDe", b.tieuDe());
            m.put("tenTacGia", b.tenTacGia());
            m.put("tenDanhMuc", b.tenDanhMuc());
            m.put("giaApDungVnd", b.donGiaApDung());
            m.put("giaGocVnd", b.giaGoc());
            m.put("phanTramGiam", b.phanTramGiam());
            m.put("dangFlashSale", b.dangFlashSale());
            m.put("tonKho", b.tonKho());
            m.put("daBan", b.daBan());
            m.put("diemDanhGia", b.diemDanhGiaTb());
            m.put("tomTatNgan", b.tomTatNgan());
            return m;
        }).toList();
    }

    private List<Map<String, Object>> buildGeminiFunctionDeclarations() {
        return List.of(
                Map.of(
                        "name", "searchBooksBySemanticOrAuthor",
                        "description",
                        "Tìm sách thật còn tồn kho theo chủ đề tiếng Việt, tên sách, tên tác giả hoặc ngân sách tối đa (VND).",
                        "parameters", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "query",
                                        Map.of("type", "STRING", "description",
                                                "Chủ đề (tâm lý, lập trình, kinh tế, trinh thám...), tên sách hoặc tác giả"),
                                        "maxPrice",
                                        Map.of("type", "NUMBER", "description",
                                                "Ngân sách tối đa tính theo VND, ví dụ 150000"),
                                        "limit",
                                        Map.of("type", "INTEGER", "description", "Số lượng sách tối đa (mặc định 4)")),
                                "required", List.of("query"))),
                Map.of(
                        "name", "getFlashSaleOrBestsellerBooks",
                        "description",
                        "Lấy danh sách sách đang giảm giá sâu Flash Sale hoặc Top sách bán chạy nhất cửa hàng.",
                        "parameters", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "mode", Map.of("type", "STRING", "description", "FLASH_SALE hoặc BESTSELLER"),
                                        "limit",
                                        Map.of("type", "INTEGER", "description", "Số lượng tối đa (mặc định 4)")),
                                "required", List.of("mode"))),
                Map.of(
                        "name", "getPersonalizedRecommendations",
                        "description",
                        "Gợi ý sách cá nhân hóa theo gu đọc riêng của khách hàng dựa trên lịch sử hành vi xem, yêu thích, mua hàng.",
                        "parameters", Map.of("type", "OBJECT", "properties", Map.of())),
                Map.of(
                        "name", "suggestBookComboByBudget",
                        "description",
                        "Ghép combo 2-3 cuốn sách tối ưu trong tầm ngân sách khách đưa ra (hoặc vừa đủ Freeship 300.000đ / làm quà tặng).",
                        "parameters", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "topicOrQuery",
                                        Map.of("type", "STRING", "description", "Chủ đề hoặc sở thích của người nhận"),
                                        "budgetVnd",
                                        Map.of("type", "NUMBER", "description",
                                                "Tổng ngân sách tối đa (VND), ví dụ 350000"),
                                        "itemCount",
                                        Map.of("type", "INTEGER", "description", "Số cuốn trong combo (2 hoặc 3)")),
                                "required", List.of("topicOrQuery"))),
                Map.of(
                        "name", "compareBooksOrReadReviews",
                        "description",
                        "So sánh chi tiết 2 cuốn sách hoặc đọc đánh giá/nhận xét thực tế của độc giả từ CSDL.",
                        "parameters", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "bookQuery1",
                                        Map.of("type", "STRING", "description",
                                                "Mã sachId hoặc tên cuốn sách thứ nhất"),
                                        "bookQuery2",
                                        Map.of("type", "STRING", "description",
                                                "Mã sachId hoặc tên cuốn sách thứ hai (để trống nếu chỉ xem review 1 cuốn)")),
                                "required", List.of("bookQuery1"))),
                Map.of(
                        "name", "getActiveVouchersAndDefaultAddress",
                        "description",
                        "Lấy danh sách mã giảm giá đang mở, trạng thái đăng nhập và địa chỉ nhận hàng mặc định của khách.",
                        "parameters", Map.of("type", "OBJECT", "properties", Map.of())),
                Map.of(
                        "name", "manageCustomerCart",
                        "description",
                        "Xem giỏ hàng (kèm số tiền còn thiếu để Freeship 300k), thêm sách, cập nhật số lượng hoặc xóa sách khỏi giỏ.",
                        "parameters", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "action",
                                        Map.of("type", "STRING", "description", "VIEW | ADD | UPDATE_QTY | REMOVE"),
                                        "sachIdOrTitle",
                                        Map.of("type", "STRING", "description",
                                                "Mã sachId hoặc tên sách (bắt buộc khi ADD / UPDATE_QTY / REMOVE)"),
                                        "soLuong",
                                        Map.of("type", "INTEGER", "description", "Số lượng sách (mặc định 1)")),
                                "required", List.of("action"))),
                Map.of(
                        "name", "manageCustomerWishlist",
                        "description", "Xem Tủ sách yêu thích hoặc thả tim / bỏ tim một cuốn sách cho khách hàng.",
                        "parameters", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "action", Map.of("type", "STRING", "description", "VIEW | TOGGLE"),
                                        "sachIdOrTitle",
                                        Map.of("type", "STRING", "description", "Mã sachId hoặc tên sách khi TOGGLE")),
                                "required", List.of("action"))),
                Map.of(
                        "name", "placeOrderOrCheckoutForCustomer",
                        "description",
                        "TỰ ĐỘNG ĐẶT ĐƠN HÀNG COD NGAY LẬP TỨC khi khách yêu cầu đặt 1 cuốn sách (DIRECT_BOOK) hoặc đặt thanh toán toàn bộ giỏ hàng (CART_CHECKOUT), tự động áp mã giảm giá tốt nhất!",
                        "parameters", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "mode",
                                        Map.of("type", "STRING", "description",
                                                "DIRECT_BOOK (đặt riêng 1 đầu sách) hoặc CART_CHECKOUT (đặt các sách trong giỏ)"),
                                        "sachIdOrTitle",
                                        Map.of("type", "STRING", "description",
                                                "Mã sachId (vd sach_004) hoặc tên sách cần đặt khi mode = DIRECT_BOOK"),
                                        "soLuong",
                                        Map.of("type", "INTEGER", "description", "Số lượng đặt mua (mặc định 1)"),
                                        "voucherCode",
                                        Map.of("type", "STRING", "description",
                                                "Mã giảm giá cụ thể nếu khách yêu cầu, hoặc để trống/AUTO để tự săn mã tốt nhất")),
                                "required", List.of("mode"))),
                Map.of(
                        "name", "lookupOrCancelCustomerOrder",
                        "description",
                        "Kiểm tra danh sách đơn hàng gần đây, xem chi tiết hành trình đơn DH-... hoặc HỦY đơn hàng đang chờ xác nhận.",
                        "parameters", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "action",
                                        Map.of("type", "STRING", "description", "LIST_RECENT | DETAIL | CANCEL"),
                                        "orderCodeOrId",
                                        Map.of("type", "STRING", "description",
                                                "Mã đơn hàng, ví dụ DH-12AB34 (để trống khi LIST_RECENT hoặc hủy đơn mới nhất)"),
                                        "cancelReason",
                                        Map.of("type", "STRING", "description", "Lý do hủy đơn khi action = CANCEL")),
                                "required", List.of("action"))));
    }

    // =========================================================================
    // TẦNG 2: SMART DB FALLBACK ĐỒNG NHẤT 100% NGHIỆP VỤ TỰ ĐẶT ĐƠN & 10 CÔNG CỤ
    // =========================================================================

    @SuppressWarnings("unchecked")
    private ChatResponse handleSmartFallback(String nullableNguoiDungId, ChatRequest request) {
        String raw = request.message().trim();
        String norm = normalizeText(raw);

        // 1. Tra cứu hoặc Hủy đơn hàng
        if (containsAny(norm, "huy don", "huy giup", "huy ma don")) {
            String orderCode = extractOrderCode(raw, request.history());
            Map<String, Object> res = aiTools.lookupOrCancelCustomerOrder(nullableNguoiDungId, "CANCEL", orderCode,
                    "Khách yêu cầu hủy qua Trợ lý AI");
            boolean ok = Boolean.TRUE.equals(res.get("success"));
            return new ChatResponse(
                    String.valueOf(res.get("message")),
                    List.of(),
                    false,
                    false,
                    ok,
                    null,
                    List.of("🔥 Sách đang Flash Sale", "💡 Gợi ý sách hợp gu của tôi"),
                    "SMART_FALLBACK");
        }
        if (containsAny(norm, "kiem tra don", "don hang cua toi", "don hang gan nhat", "tra cuu don", "hanh trinh don")
                || ORDER_CODE_PATTERN.matcher(raw).find()) {
            String orderCode = extractOrderCode(raw, null);
            Map<String, Object> res = aiTools.lookupOrCancelCustomerOrder(
                    nullableNguoiDungId,
                    orderCode != null ? "DETAIL" : "LIST_RECENT",
                    orderCode,
                    null);
            if (!Boolean.TRUE.equals(res.get("success"))) {
                return new ChatResponse(String.valueOf(res.get("message")), List.of(), false, null, "SMART_FALLBACK");
            }
            if (res.get("order") instanceof Map<?, ?> rawSingle) {
                Map<String, Object> single = (Map<String, Object>) rawSingle;
                String reply = "📦 **Thông tin đơn hàng " + single.get("orderCode") + ":**\n"
                        + "• **Trạng thái:** `" + single.get("status") + "`\n"
                        + "• **Sản phẩm:** " + single.get("bookTitles") + "\n"
                        + "• **Tổng thanh toán:** **" + formatVnd((BigDecimal) single.get("totalAmountVnd")) + "**\n"
                        + "• **Cập nhật mới nhất:** " + single.getOrDefault("latestMilestone", "Đang xử lý");
                return new ChatResponse(reply, List.of(), false, false, false, null,
                        List.of("❌ Hủy đơn " + single.get("orderCode"), "🔥 Sách đang Flash Sale"), "SMART_FALLBACK");
            }
            List<Map<String, Object>> recent = (List<Map<String, Object>>) res.getOrDefault("recentOrders", List.of());
            if (recent.isEmpty()) {
                return new ChatResponse(String.valueOf(res.get("message")), List.of(), false, null, "SMART_FALLBACK");
            }
            StringBuilder sb = new StringBuilder("📦 **Các đơn hàng gần nhất của bạn:**\n");
            for (Map<String, Object> o : recent) {
                sb.append("• **").append(o.get("orderCode")).append("** (`").append(o.get("status")).append("`) — ")
                        .append(formatVnd((BigDecimal) o.get("totalAmountVnd"))).append(" — ")
                        .append(o.get("bookTitles")).append('\n');
            }
            return new ChatResponse(sb.toString().trim(), List.of(), false, false, false, null,
                    List.of("💡 Gợi ý sách hợp gu của tôi", "🎟️ Xem mã giảm giá đang mở"), "SMART_FALLBACK");
        }

        // 2. Thanh toán toàn bộ giỏ hàng qua Chat
        if (containsAny(norm, "thanh toan gio hang", "dat gio hang", "dat het gio", "chot gio hang")) {
            String vCode = extractVoucherCode(raw);
            Map<String, Object> orderRes = aiTools.placeOrderOrCheckoutForCustomer(nullableNguoiDungId, "CART_CHECKOUT",
                    null, 1, vCode);
            return buildFallbackOrderChatResponse(orderRes);
        }

        // 3. Tự động đặt sách ngay khi khách yêu cầu (1 bước + tự săn Voucher tốt
        // nhất!)
        if (isOrderIntent(norm) || isConfirmationIntent(norm)) {
            if (nullableNguoiDungId == null || nullableNguoiDungId.isBlank()) {
                return new ChatResponse(
                        "Bạn vui lòng đăng nhập tài khoản khách hàng trước để mình tự động kiểm tra địa chỉ mặc định, săn mã giảm giá tốt nhất và đặt đơn ngay cho bạn nhé!",
                        List.of(),
                        false,
                        null,
                        "SMART_FALLBACK");
            }
            int qty = extractQuantity(norm);
            String vCode = extractVoucherCode(raw);
            BookDto target = resolveTargetBookFromMessageOrHistory(raw, norm, request.history());
            if (target == null) {
                return new ChatResponse(
                        "Bạn muốn đặt cuốn sách nào nhỉ? Chỉ cần nhắn *\"Đặt cho mình cuốn Hamlet\"* hoặc bấm **⚡ Đặt ngay** trên thẻ sách là mình tạo đơn kèm mã giảm giá hời nhất cho bạn ngay!",
                        List.of(),
                        false,
                        null,
                        "SMART_FALLBACK");
            }
            Map<String, Object> orderRes = aiTools.placeOrderOrCheckoutForCustomer(
                    nullableNguoiDungId,
                    "DIRECT_BOOK",
                    target.id(),
                    qty,
                    vCode);
            return buildFallbackOrderChatResponse(orderRes);
        }

        // 4. Quản lý Giỏ hàng (Xem / Xóa / Đổi số lượng / Thêm vào giỏ)
        if (containsAny(norm, "xoa khoi gio", "bo khoi gio")) {
            BookDto target = resolveTargetBookFromMessageOrHistory(raw, norm, request.history());
            if (target == null) {
                return new ChatResponse("Bạn muốn xóa cuốn sách nào khỏi giỏ hàng nhỉ?", List.of(), false, null,
                        "SMART_FALLBACK");
            }
            Map<String, Object> res = aiTools.manageCustomerCart(nullableNguoiDungId, "REMOVE", target.id(), 1);
            return new ChatResponse(String.valueOf(res.get("message")), List.of(),
                    Boolean.TRUE.equals(res.get("cartMutated")), null, "SMART_FALLBACK");
        }
        if (containsAny(norm, "xem gio hang", "gio hang cua toi", "gio hang co gi", "kiem tra gio hang")) {
            Map<String, Object> res = aiTools.manageCustomerCart(nullableNguoiDungId, "VIEW", null, 1);
            if (!Boolean.TRUE.equals(res.get("success"))) {
                return new ChatResponse(String.valueOf(res.get("message")), List.of(), false, null, "SMART_FALLBACK");
            }
            int totalItems = ((Number) res.getOrDefault("totalItemsInCart", 0)).intValue();
            if (totalItems == 0) {
                return new ChatResponse(
                        "Giỏ hàng của bạn hiện đang trống. Mình gợi ý vài cuốn đang Flash Sale cho bạn nhé?",
                        aiTools.getFlashSaleOrBestsellerBooks("FLASH_SALE", 3), false, false, false, null,
                        List.of("🔥 Sách đang Flash Sale", "💡 Gợi ý sách hợp gu của tôi"), "SMART_FALLBACK");
            }
            BigDecimal sub = (BigDecimal) res.get("subtotalVnd");
            BigDecimal miss = (BigDecimal) res.get("missingForFreeshipVnd");
            String shipNote = miss.compareTo(BigDecimal.ZERO) == 0
                    ? "✅ Đơn của bạn đã đủ điều kiện **Freeship**!"
                    : "🚚 Mua thêm **" + formatVnd(miss) + "** nữa để được **Freeship** toàn quốc!";
            String reply = "🛒 **Giỏ hàng hiện tại của bạn (" + totalItems + " cuốn):**\n"
                    + "• **Tạm tính:** **" + formatVnd(sub) + "**\n• " + shipNote;
            return new ChatResponse(reply, List.of(), false, false, false, null,
                    List.of("💳 Thanh toán giỏ hàng giúp mình", "🎟️ Xem mã giảm giá đang mở"), "SMART_FALLBACK");
        }
        if (containsAny(norm, "them vao gio", "bo vao gio", "them cuon", "cho vao gio", "them sach")) {
            int qty = extractQuantity(norm);
            BookDto target = resolveTargetBookFromMessageOrHistory(raw, norm, request.history());
            if (target == null) {
                return new ChatResponse(
                        "Mình chưa tìm thấy cuốn sách bạn muốn thêm. Bạn ghi rõ tên sách giúp mình nhé!", List.of(),
                        false, null, "SMART_FALLBACK");
            }
            Map<String, Object> addRes = aiTools.addBookToCustomerCart(nullableNguoiDungId, target.id(), qty);
            if (Boolean.TRUE.equals(addRes.get("success"))) {
                BigDecimal miss = (BigDecimal) addRes.getOrDefault("missingForFreeshipVnd", BigDecimal.ZERO);
                String reply = "🛒 Đã thêm **" + qty + " cuốn " + target.tieuDe() + "** ("
                        + formatVnd(target.donGiaApDung())
                        + ") vào giỏ hàng!\n"
                        + (miss.compareTo(BigDecimal.ZERO) == 0
                                ? "✅ Giỏ hàng của bạn đã đủ mốc **Freeship 300.000đ**!"
                                : "• Mua thêm **" + formatVnd(miss) + "** để được Freeship nhé!");
                return new ChatResponse(reply, List.of(target), true, false, false, null,
                        List.of("💳 Thanh toán giỏ hàng giúp mình", "🛒 Xem giỏ hàng của tôi"), "SMART_FALLBACK");
            }
            return new ChatResponse(String.valueOf(addRes.get("message")), List.of(target), false, null,
                    "SMART_FALLBACK");
        }

        // 5. Quản lý Tủ sách Yêu thích
        if (containsAny(norm, "yeu thich", "tha tim", "luu sach")) {
            boolean isView = containsAny(norm, "xem tu", "xem danh sach", "sach yeu thich cua toi",
                    "tu sach yeu thich");
            BookDto target = isView ? null : resolveTargetBookFromMessageOrHistory(raw, norm, request.history());
            Map<String, Object> wRes = aiTools.manageCustomerWishlist(
                    nullableNguoiDungId,
                    target != null ? "TOGGLE" : "VIEW",
                    target != null ? target.id() : null);
            if (!Boolean.TRUE.equals(wRes.get("success"))) {
                return new ChatResponse(String.valueOf(wRes.get("message")), List.of(), false, null, "SMART_FALLBACK");
            }
            if (Boolean.TRUE.equals(wRes.get("wishlistMutated"))) {
                return new ChatResponse("❤️ " + wRes.get("message"), target != null ? List.of(target) : List.of(),
                        false, true, false, null, List.of("❤️ Xem tủ sách yêu thích", "🛒 Xem giỏ hàng của tôi"),
                        "SMART_FALLBACK");
            }
            List<BookDto> wBooks = (List<BookDto>) wRes.getOrDefault("books", List.of());
            String reply = wBooks.isEmpty()
                    ? "Tủ sách yêu thích của bạn hiện chưa có cuốn nào."
                    : "❤️ Bạn đang lưu **" + wRes.get("totalWishlistCount") + " cuốn sách** trong Tủ yêu thích:";
            return new ChatResponse(reply, wBooks, false, false, false, null,
                    List.of("💡 Gợi ý sách hợp gu của tôi"), "SMART_FALLBACK");
        }

        // 6. Hỏi chính sách vận chuyển / đổi trả / hotline
        if (containsAny(norm, "chinh sach", "doi tra", "van chuyen", "phi ship", "freeship", "hotline")) {
            String reply = "**Chính sách Nhà sách Tri Thức Việt:**\n"
                    + "• **Vận chuyển:** Miễn phí ship (Freeship) toàn quốc cho đơn từ **300.000đ**; đơn dưới 300.000đ phí tiêu chuẩn **25.000đ**.\n"
                    + "• **Đổi trả:** Hỗ trợ đổi cuốn mới miễn phí trong **7 ngày** nếu sách lỗi in ấn, rách trang hoặc móp góc do vận chuyển.\n"
                    + "• **Hotline CSKH:** `1900 6886` (8:00 – 21:00 hằng ngày).";
            return new ChatResponse(reply, List.of(), false, false, false, null,
                    List.of("🎟️ Xem mã giảm giá đang mở", "🔥 Sách đang Flash Sale"), "SMART_FALLBACK");
        }

        // 7. Hỏi mã giảm giá / voucher
        if (containsAny(norm, "voucher", "ma giam gia", "khuyen mai")) {
            Map<String, Object> info = aiTools.getActiveVouchersAndDefaultAddress(nullableNguoiDungId);
            List<VoucherDto> vouchers = (List<VoucherDto>) info.get("activeVouchers");
            if (vouchers == null || vouchers.isEmpty()) {
                return new ChatResponse("Hiện chưa có mã giảm giá nào đang mở. Bạn quay lại sau nhé!", List.of(), false,
                        null, "SMART_FALLBACK");
            }
            StringBuilder sb = new StringBuilder("🎟️ Hiện Nhà sách đang mở **" + vouchers.size()
                    + " mã giảm giá** (khi đặt qua AI mình sẽ tự săn mã hời nhất cho bạn!):\n");
            for (VoucherDto v : vouchers) {
                sb.append("• **").append(v.maCode()).append("**: ").append(v.tieuDe())
                        .append(" (Đơn từ ").append(formatVnd(v.donToiThieu())).append(")\n");
            }
            return new ChatResponse(sb.toString().trim(), List.of(), false, false, false, null,
                    List.of("🔥 Sách đang Flash Sale", "🎁 Ghép combo sách dưới 350k"), "SMART_FALLBACK");
        }

        // 8. Săn Flash Sale
        if (containsAny(norm, "flash sale", "giam sau", "dang sale", "khung gio vang")) {
            List<BookDto> flashBooks = aiTools.getFlashSaleOrBestsellerBooks("FLASH_SALE", 4);
            StringBuilder sb = new StringBuilder("🔥 **Top sách đang Flash Sale giảm sâu hôm nay:**\n");
            for (BookDto b : flashBooks) {
                sb.append("• **").append(b.tieuDe()).append("** — **").append(formatVnd(b.donGiaApDung()))
                        .append("** (giảm **-").append(b.phanTramGiam()).append("%**, còn ").append(b.tonKho())
                        .append(" cuốn)\n");
            }
            return new ChatResponse(sb.toString().trim(), flashBooks, false, false, false, null,
                    buildDynamicChips(flashBooks, false, null), "SMART_FALLBACK");
        }

        // 9. Đề xuất cá nhân hóa "Hợp gu của tôi"
        if (containsAny(norm, "hop gu", "ca nhan hoa", "nen doc gi", "goi y rieng", "so thich cua toi")) {
            Map<String, Object> rec = aiTools.getPersonalizedRecommendations(nullableNguoiDungId);
            List<BookDto> recBooks = (List<BookDto>) rec.getOrDefault("books", List.of());
            List<String> favs = (List<String>) rec.getOrDefault("favoriteTopics", List.of());
            String intro = !favs.isEmpty()
                    ? "✨ Dựa trên gu đọc gần đây của bạn về **" + String.join(", ", favs)
                            + "**, mình chọn riêng cho bạn:"
                    : "✨ Mình gợi ý các tựa sách thịnh hành được yêu thích nhất dành cho bạn:";
            return new ChatResponse(intro, recBooks, false, false, false, null,
                    buildDynamicChips(recBooks, false, null), "SMART_FALLBACK");
        }

        // 10. Ghép Combo sách theo ngân sách / quà tặng
        if (containsAny(norm, "combo", "qua tang")) {
            BigDecimal budget = extractBudgetVnd(norm);
            Map<String, Object> combo = aiTools.suggestBookComboByBudget(raw, budget, 3);
            List<BookDto> comboBooks = (List<BookDto>) combo.getOrDefault("books", List.of());
            BigDecimal sum = (BigDecimal) combo.get("comboSubtotalVnd");
            boolean fs = Boolean.TRUE.equals(combo.get("qualifiesFreeship"));
            String reply = "🎁 **Combo " + comboBooks.size() + " cuốn sách tối ưu cho bạn** (Tổng: **" + formatVnd(sum)
                    + "** — "
                    + (fs ? "được **Freeship**!" : "phí ship 25.000đ") + "):";
            return new ChatResponse(reply, comboBooks, false, false, false, null,
                    buildDynamicChips(comboBooks, false, null), "SMART_FALLBACK");
        }

        // 11. Tư vấn tìm sách / theo chủ đề / đọc review / tóm tắt
        BigDecimal maxPrice = extractBudgetVnd(norm);
        List<BookDto> books = aiTools.searchBooksBySemanticOrAuthor(raw, maxPrice, 4);
        if (containsAny(norm, "gia re", "re nhat", "tiet kiem", "binh dan") && !books.isEmpty()) {
            books = books.stream()
                    .sorted((a, b) -> a.donGiaApDung().compareTo(b.donGiaApDung()))
                    .toList();
        }
        if (books.isEmpty()) {
            return new ChatResponse(
                    "Mình chưa tìm thấy đầu sách khớp yêu cầu trong tầm giá này. Bạn thử nới rộng ngân sách hoặc chọn chủ đề khác (Lập trình, Kinh tế, Tâm lý, Trinh thám, Thiếu nhi...) nhé!",
                    List.of(),
                    false,
                    null,
                    "SMART_FALLBACK");
        }

        if (containsAny(norm, "tom tat", "noi dung", "review", "danh gia", "binh luan")) {
            BookDto top = books.get(0);
            Map<String, Object> cmp = aiTools.compareBooksOrReadReviews(top.id(), "");
            List<Map<String, Object>> detailList = (List<Map<String, Object>>) cmp.getOrDefault("bookDetails",
                    List.of());
            StringBuilder sb = new StringBuilder("**\"" + top.tieuDe() + "\"** — *" + top.tenTacGia() + "*:\n");
            sb.append(top.tomTatNgan() != null ? top.tomTatNgan() : ("Thuộc chủ đề " + top.tenDanhMuc())).append('\n');
            sb.append("• Giá áp dụng: **").append(formatVnd(top.donGiaApDung())).append("** • Đánh giá: **")
                    .append(top.diemDanhGiaTb()).append("★** (").append(top.soLuotDanhGia())
                    .append(" lượt) • Tồn kho: **")
                    .append(top.tonKho()).append(" cuốn**");
            if (!detailList.isEmpty() && detailList.get(0).get("recentReviews") instanceof List<?> revs
                    && !revs.isEmpty()) {
                sb.append("\n• **Nhận xét thực tế từ độc giả:**");
                for (Object ro : revs) {
                    if (ro instanceof Map<?, ?> rm) {
                        sb.append("\n  - *").append(rm.get("reviewer")).append("* (").append(rm.get("rating"))
                                .append("★): \"")
                                .append(rm.get("comment")).append("\"");
                    }
                }
            }
            return new ChatResponse(sb.toString(), List.of(top), false, false, false, null,
                    buildDynamicChips(List.of(top), false, null), "SMART_FALLBACK");
        }

        boolean genericTopicAsk = containsAny(norm, "chu de", "the loai", "danh muc")
                && aiTools.inferCategoryIdsFromVietnameseQuery(norm).isEmpty();
        StringBuilder sb = new StringBuilder();
        if (genericTopicAsk) {
            sb.append("📚 **Các chủ đề nổi bật tại Tri Thức Việt:**\n")
                    .append("• Công nghệ & Lập trình • Kinh tế & Tài chính\n")
                    .append("• Kỹ năng & Tâm lý • Văn học & Trinh thám\n")
                    .append("• Khoa học & Lịch sử • Thiếu nhi • Ngoại ngữ\n\n")
                    .append("Mình chọn trước **").append(books.size()).append(" tựa sách tiêu biểu**");
        } else {
            sb.append("📚 Mình gợi ý **").append(books.size()).append(" tựa sách phù hợp** đang sẵn kho");
        }
        if (maxPrice != null) {
            sb.append(" (dưới ").append(formatVnd(maxPrice)).append(")");
        }
        sb.append(":\n");
        for (int i = 0; i < books.size(); i++) {
            BookDto b = books.get(i);
            sb.append(i + 1).append(". **").append(b.tieuDe()).append("** — *").append(b.tenTacGia()).append("*\n")
                    .append("   • Giá: **").append(formatVnd(b.donGiaApDung())).append("** • Chủ đề: ")
                    .append(b.tenDanhMuc()).append(" (còn ").append(b.tonKho()).append(" cuốn)\n");
        }
        sb.append("\nBạn có thể bấm **⚡ Đặt ngay** trên thẻ sách bên dưới để tự săn mã giảm giá hời nhất nhé!");
        return new ChatResponse(sb.toString().trim(), books, false, false, false, null,
                buildDynamicChips(books, false, null), "SMART_FALLBACK");
    }

    private ChatResponse buildFallbackOrderChatResponse(Map<String, Object> orderRes) {
        if (Boolean.TRUE.equals(orderRes.get("success"))) {
            String orderCode = String.valueOf(orderRes.get("orderCode"));
            BigDecimal subtotal = (BigDecimal) orderRes.get("subtotalVnd");
            BigDecimal ship = (BigDecimal) orderRes.get("shippingFeeVnd");
            BigDecimal discount = (BigDecimal) orderRes.get("discountVnd");
            BigDecimal total = (BigDecimal) orderRes.get("totalAmount");
            String appliedVoucher = (String) orderRes.get("appliedVoucherCode");
            String addr = String.valueOf(orderRes.get("shippingAddress"));
            boolean fromCart = Boolean.TRUE.equals(orderRes.get("fromCart"));
            BookDto orderedBook = orderRes.get("book") instanceof BookDto b ? b : null;

            StringBuilder sb = new StringBuilder("🎉 **Đặt hàng tự động thành công!** Mã đơn: **" + orderCode + "**\n");
            sb.append("• **Tạm tính:** ").append(formatVnd(subtotal))
                    .append(" • **Phí ship:** ")
                    .append(ship.compareTo(BigDecimal.ZERO) == 0 ? "Miễn phí (Freeship)" : formatVnd(ship))
                    .append('\n');
            if (appliedVoucher != null && discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("• **Voucher tự áp dụng:** `").append(appliedVoucher).append("` (-")
                        .append(formatVnd(discount)).append(")\n");
            }
            sb.append("• **Tổng thanh toán (COD):** **").append(formatVnd(total)).append("**\n");
            sb.append("• **Giao tới:** ").append(addr);

            return new ChatResponse(
                    sb.toString(),
                    orderedBook != null ? List.of(orderedBook) : List.of(),
                    fromCart,
                    false,
                    true,
                    orderCode,
                    List.of("📦 Kiểm tra đơn " + orderCode, "❌ Hủy đơn " + orderCode, "🔥 Sách đang Flash Sale"),
                    "SMART_FALLBACK");
        }
        return new ChatResponse(String.valueOf(orderRes.get("message")), List.of(), false, null, "SMART_FALLBACK");
    }

    private List<String> buildDynamicChips(List<BookDto> books, boolean cartUpdated, String createdOrderCode) {
        if (createdOrderCode != null && !createdOrderCode.isBlank()) {
            return List.of(
                    "📦 Kiểm tra đơn " + createdOrderCode,
                    "❌ Hủy đơn " + createdOrderCode,
                    "🔥 Sách đang Flash Sale");
        }
        if (cartUpdated) {
            return List.of(
                    "💳 Thanh toán giỏ hàng giúp mình",
                    "🛒 Xem giỏ hàng của tôi",
                    "🎁 Ghép combo sách dưới 350k");
        }
        if (books != null && !books.isEmpty()) {
            BookDto top = books.get(0);
            return List.of(
                    "⚡ Đặt ngay 1 cuốn " + top.tieuDe() + " [ID: " + top.id() + "]",
                    "🔍 Xem đánh giá cuốn " + top.tieuDe(),
                    "❤️ Lưu cuốn " + top.tieuDe() + " vào yêu thích");
        }
        return List.of(
                "✨ Gợi ý sách hợp gu của tôi",
                "🔥 Sách đang Flash Sale",
                "🎁 Ghép combo sách dưới 350k");
    }

    // --- Các hàm tiện ích nội bộ ---

    private BookDto resolveTargetBookFromMessageOrHistory(
            String rawMessage,
            String normMessage,
            List<ChatRequest.HistoryMessage> history) {
        Matcher idMatcher = BOOK_ID_TAG_PATTERN.matcher(rawMessage);
        if (idMatcher.find()) {
            String id = idMatcher.group(1) != null ? idMatcher.group(1) : idMatcher.group(2);
            BookDto byId = aiTools.resolveBookByIdOrQuery(id);
            if (byId != null)
                return byId;
        }

        String stripped = normMessage
                .replaceAll(
                        "\\b(toi|minh|em|ban|shop|oi|muon|can|nho|giup|cho|dat|mua|lay|chot|luon|ngay|don|sach|cuon|quyen|nay|do|dau|tien|so|them|vao|gio|hang|xoa|khoi|yeu|thich|tha|tim|luu|\\d+)\\b",
                        " ")
                .trim();

        if (!stripped.isBlank() && stripped.length() >= 2) {
            List<BookDto> found = aiTools.searchBooksBySemanticOrAuthor(rawMessage, null, 1);
            if (!found.isEmpty()) {
                return found.get(0);
            }
        }

        String pendingId = extractPendingBookIdFromHistory(history);
        if (pendingId != null) {
            BookDto byPending = aiTools.resolveBookByIdOrQuery(pendingId);
            if (byPending != null)
                return byPending;
        }

        String recentTitle = extractRecentBookTitleFromHistory(history);
        if (recentTitle != null) {
            return aiTools.resolveBookByIdOrQuery(recentTitle);
        }
        return null;
    }

    private boolean isOrderIntent(String norm) {
        return containsAny(
                norm,
                "dat luon", "dat mua", "dat sach", "dat giup", "mua giup",
                "dat cuon", "dat quyen", "mua cuon", "mua sach", "muon dat",
                "dat don", "dat cho", "dat ngay", "dat nhanh", "chot cuon", "lay cuon", "mua luon");
    }

    private boolean isConfirmationIntent(String norm) {
        return containsAny(norm, "xac nhan dat", "dong y dat", "ok dat", "chot don");
    }

    private String extractOrderCode(String raw, List<ChatRequest.HistoryMessage> history) {
        if (raw != null) {
            Matcher m = ORDER_CODE_PATTERN.matcher(raw);
            if (m.find())
                return m.group(1).toUpperCase(Locale.ROOT);
        }
        if (history != null) {
            for (int i = history.size() - 1; i >= 0; i--) {
                ChatRequest.HistoryMessage msg = history.get(i);
                if (msg != null && msg.text() != null) {
                    Matcher m = ORDER_CODE_PATTERN.matcher(msg.text());
                    if (m.find())
                        return m.group(1).toUpperCase(Locale.ROOT);
                }
            }
        }
        return null;
    }

    private String extractVoucherCode(String raw) {
        if (raw == null)
            return null;
        Matcher m = VOUCHER_CODE_PATTERN.matcher(raw.toUpperCase(Locale.ROOT));
        return m.find() ? m.group(1) : null;
    }

    private int extractQuantity(String norm) {
        Matcher m = QUANTITY_PATTERN.matcher(norm);
        if (m.find()) {
            try {
                return Math.min(Math.max(Integer.parseInt(m.group(1)), 1), 20);
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFirstCandidateContent(Map<String, Object> response) {
        if (response == null || !(response.get("candidates") instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }
        Object first = candidates.get(0);
        if (!(first instanceof Map<?, ?> candMap)) {
            return null;
        }
        return candMap.get("content") instanceof Map<?, ?> contentMap ? (Map<String, Object>) contentMap : null;
    }

    private List<BookDto> deduplicateBooks(List<BookDto> books) {
        Map<String, BookDto> unique = new LinkedHashMap<>();
        for (BookDto b : books) {
            unique.putIfAbsent(b.id(), b);
        }
        return unique.values().stream().limit(4).toList();
    }

    private String extractPendingBookIdFromHistory(List<ChatRequest.HistoryMessage> history) {
        if (history == null)
            return null;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatRequest.HistoryMessage msg = history.get(i);
            if (msg != null && msg.text() != null) {
                Matcher m = BOOK_ID_TAG_PATTERN.matcher(msg.text());
                if (m.find()) {
                    return m.group(1) != null ? m.group(1) : m.group(2);
                }
            }
        }
        return null;
    }

    private String extractRecentBookTitleFromHistory(List<ChatRequest.HistoryMessage> history) {
        if (history == null)
            return null;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatRequest.HistoryMessage msg = history.get(i);
            if (msg != null && !"USER".equalsIgnoreCase(msg.sender()) && msg.text() != null) {
                Matcher m = BOLD_TITLE_PATTERN.matcher(msg.text());
                while (m.find()) {
                    String candidate = m.group(1).trim();
                    String cNorm = normalizeText(candidate);
                    if (!containsAny(cNorm, "dau sach", "nha sach", "chinh sach", "ma giam gia", "xac nhan",
                            "mien phi", "tong thanh toan", "sach:", "tam tinh:", "dat hang", "thong tin don",
                            "combo")) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private BigDecimal extractBudgetVnd(String norm) {
        Matcher mTr = BUDGET_TRIEU_PATTERN.matcher(norm);
        if (mTr.find()) {
            double millions = Double.parseDouble(mTr.group(1).replace(',', '.'));
            return BigDecimal.valueOf(Math.round(millions * 1_000_000L));
        }
        Matcher mK = BUDGET_K_PATTERN.matcher(norm);
        if (mK.find()) {
            return BigDecimal.valueOf(Long.parseLong(mK.group(1)) * 1000L);
        }
        Matcher mVnd = BUDGET_VND_PATTERN.matcher(norm);
        if (mVnd.find()) {
            long val = Long.parseLong(mVnd.group(1)) * 1000L + Long.parseLong(mVnd.group(2));
            return BigDecimal.valueOf(val);
        }
        Matcher mPlain = BUDGET_PLAIN_VND_PATTERN.matcher(norm);
        if (mPlain.find()) {
            return BigDecimal.valueOf(Long.parseLong(mPlain.group(1)));
        }
        return null;
    }

    private String optStr(Map<String, Object> args, String key, String def) {
        Object v = args.get(key);
        return v == null ? def : String.valueOf(v).trim();
    }

    private BigDecimal parseOptionalDecimal(Object val) {
        if (val instanceof Number n)
            return BigDecimal.valueOf(n.doubleValue());
        if (val instanceof String s && !s.isBlank()) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private int parseOptionalInt(Object val, int fallback) {
        if (val instanceof Number n)
            return n.intValue();
        if (val instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw))
                return true;
        }
        return false;
    }

    private String normalizeText(String input) {
        if (input == null)
            return "";
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null)
            return "0đ";
        NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        return nf.format(amount.longValue()) + "đ";
    }
}
