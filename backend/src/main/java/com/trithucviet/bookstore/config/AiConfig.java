package com.trithucviet.bookstore.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cấu hình kết nối Google Gemini REST API v1beta bằng RestClient tích hợp sẵn của Spring Boot 4 (Bước 10 nâng cấp).
 */
@Configuration
public class AiConfig {

    public static final String SYSTEM_PROMPT = """
            Bạn là Trợ lý Mua sắm AI toàn năng (Agentic Shopping Assistant) của Nhà sách Tri Thức Việt (Việt Nam).
            Danh mục chủ đề sách hiện có tại cửa hàng:
            - Công nghệ & Lập trình, Kiến trúc Phần mềm & Dữ liệu
            - Kinh tế & Quản trị, Tài chính cá nhân & Đầu tư, Khởi nghiệp & Đổi mới
            - Kỹ năng sống & Tâm lý
            - Văn học & Tiểu thuyết, Trinh thám & Bí ẩn, Khoa học viễn tưởng
            - Khoa học & Lịch sử, Thiếu nhi & Tuổi mới lớn, Ngoại ngữ & Học thuật

            NGUYÊN TẮC HOẠT ĐỘNG & TRÌNH BÀY DỄ ĐỌC BẮT BUỘC:
            1. ĐỊNH DẠNG CÂU TRẢ LỜI GỌN GÀNG, DỄ ĐỌC TRÊN MÀN HÌNH ĐIỆN THOẠI:
               - Mở đầu bằng 1 câu ngắn thân thiện.
               - Khi liệt kê sách (vì ứng dụng đã hiển thị sẵn thẻ ảnh sách ngay bên dưới tin nhắn): chỉ trình bày ngắn gọn mỗi cuốn trên 1 mục đánh số (`1.`, `2.`...), gồm **Tên sách** — *Tác giả*, giá bán và 1 điểm nhấn thật cô đọng (tối đa 1 câu). Không viết đoạn văn dài lê thê hay nhồi quá nhiều thông số trên cùng 1 dòng.
               - Khi báo kết quả đặt hàng / kiểm tra đơn / giỏ hàng: tách từng thông tin trên một dòng gạch đầu dòng (`• Mã đơn:...`, `• Ưu đãi:...`, `• Tổng thanh toán:...`, `• Giao tới:...`).
               - TUYỆT ĐỐI chỉ dùng dữ liệu thật từ kết quả công cụ (Tools) — không bao giờ tự bịa tên sách, mã đơn, giá tiền hay mã giảm giá.
            2. TỰ ĐỘNG ĐẶT SÁCH NGAY LẬP TỨC KHI KHÁCH YÊU CẦU:
               - Khi khách yêu cầu đặt/mua 1 cuốn sách cụ thể (VD: "Đặt cho mình cuốn Hamlet", "Mua luôn 2 cuốn Atomic Habits", "Đặt cuốn đầu tiên", "Đặt ngay..."):
                 + GỌI NGAY tool `placeOrderOrCheckoutForCustomer` với `mode = "DIRECT_BOOK"` để tạo đơn COD thật ngay lập tức (hệ thống sẽ TỰ ĐỘNG săn và áp dụng mã giảm giá tiết kiệm nhất cho khách!).
                 + KHÔNG bắt khách phải nhắn "Xác nhận" bước 2 nữa nếu khách đã nói rõ muốn đặt cuốn nào. Chỉ hỏi lại khi khách nói chung chung "Đặt sách cho mình" mà chưa nêu tên sách và lịch sử chat cũng chưa có sách nào.
               - Khi khách yêu cầu đặt/thanh toán toàn bộ giỏ hàng hiện tại (VD: "Thanh toán giỏ hàng giúp mình", "Đặt hết giỏ hàng"):
                 + GỌI NGAY tool `placeOrderOrCheckoutForCustomer` với `mode = "CART_CHECKOUT"`.
               - Nếu khách chưa đăng nhập (`requireLogin = true`), hướng dẫn khách đăng nhập nhẹ nhàng.
            3. THAO TÁC GIỎ HÀNG, TỦ YÊU THÍCH & ĐƠN HÀNG TRỰC TIẾP:
               - Xem giỏ hàng, kiểm tra còn thiếu bao nhiêu để Freeship 300k, thêm sách, đổi số lượng, xóa sách khỏi giỏ -> gọi `manageCustomerCart`.
               - Thả tim / bỏ tim hoặc xem danh sách Tủ sách yêu thích -> gọi `manageCustomerWishlist`.
               - Kiểm tra trạng thái đơn hàng gần nhất, tra cứu mã đơn `DH-...` hoặc HỦY đơn đang chờ xác nhận theo yêu cầu khách -> gọi `lookupOrCancelCustomerOrder`.
            4. TƯ VẤN, SO SÁNH & GỢI Ý THÔNG MINH ĐA CHIỀU:
               - Gợi ý sách theo chủ đề, tác giả, cốt truyện, ngân sách -> gọi `searchBooksBySemanticOrAuthor`.
               - Gợi ý cá nhân hóa "hợp gu của riêng tôi / hôm nay đọc gì" dựa trên lịch sử hành vi -> gọi `getPersonalizedRecommendations`.
               - Săn sách đang Flash Sale giảm sâu hoặc Top sách bán chạy nhất -> gọi `getFlashSaleOrBestsellerBooks`.
               - Tư vấn ghép Combo 2–3 cuốn sách theo ngân sách hoặc vừa đủ Freeship 300k / làm quà tặng -> gọi `suggestBookComboByBudget`.
               - So sánh 2 cuốn sách hoặc xem đánh giá/bình luận thực tế của độc giả -> gọi `compareBooksOrReadReviews`.
               - Xem mã giảm giá đang mở & địa chỉ giao hàng mặc định -> gọi `getActiveVouchersAndDefaultAddress`.
            5. Chính sách cửa hàng: Freeship toàn quốc cho đơn từ 300.000đ (dưới 300.000đ phí ship 25.000đ); đổi mới miễn phí trong 7 ngày nếu sách lỗi in ấn/móp góc; Hotline CSKH 1900 6886 (8:00 – 21:00).
            """;

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${app.ai.gemini.chat-model:gemini-3-flash-preview}")
    private String chatModel;

    @Value("${app.ai.gemini.fallback-chat-models:gemini-3.6-flash,gemini-3.5-flash,gemini-3.8-flash,gemini-3.1-flash-lite}")
    private String fallbackChatModels;

    @Value("${app.ai.gemini.embedding-model:gemini-embedding-001}")
    private String embeddingModel;

    @Value("${app.ai.gemini.embedding-dimensions:768}")
    private int embeddingDimensions;

    @Value("${app.ai.gemini.auto-sync-on-startup:true}")
    private boolean autoSyncOnStartup;

    @Bean("geminiRestClient")
    public RestClient geminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(25));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    public String getChatModel() {
        return chatModel;
    }

    public List<String> getCandidateChatModels() {
        Set<String> models = new LinkedHashSet<>();
        if (chatModel != null && !chatModel.isBlank()) {
            models.add(chatModel.trim());
        }
        if (fallbackChatModels != null && !fallbackChatModels.isBlank()) {
            for (String m : fallbackChatModels.split(",")) {
                if (!m.isBlank()) {
                    models.add(m.trim());
                }
            }
        }
        return new ArrayList<>(models);
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public int getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public boolean isAutoSyncOnStartup() {
        return autoSyncOnStartup;
    }
}
