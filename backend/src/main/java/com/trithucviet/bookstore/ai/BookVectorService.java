package com.trithucviet.bookstore.ai;

import com.trithucviet.bookstore.config.AiConfig;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.repository.SachRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

/**
 * Dịch vụ đồng bộ Vector ngữ nghĩa 768 chiều (pgvector) cho bảng `sach` (Bước 10 - Phần 1).
 *
 * - Ghép toàn bộ metadata của mỗi cuốn sách thành 1 chuỗi văn bản duy nhất theo PHAN_TICH_NGHIEP_VU_VA_CSDL.md:
 *   "{tieu_de} | Tác giả: {ten_tac_gia} | Danh mục: {ten_danh_muc} | NXB: {ten_nxb} | Tóm tắt: {tom_tat_ngan} | Từ khóa: {tu_khoa}"
 * - Gọi Google Gemini Embedding REST API (`embedContent`, outputDimensionality = 768) bằng RestClient.
 * - Tự động đồng bộ ngầm các đầu sách còn thiếu vector ngay khi Backend khởi động nếu đã có GEMINI_API_KEY.
 */
@Service
public class BookVectorService {

    private static final Logger log = LoggerFactory.getLogger(BookVectorService.class);

    private final AiConfig aiConfig;
    private final RestClient geminiRestClient;
    private final SachRepository sachRepository;
    private final TransactionTemplate transactionTemplate;
    private final AtomicBoolean syncingInProgress = new AtomicBoolean(false);

    public BookVectorService(
            AiConfig aiConfig,
            @Qualifier("geminiRestClient") RestClient geminiRestClient,
            SachRepository sachRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.aiConfig = aiConfig;
        this.geminiRestClient = geminiRestClient;
        this.sachRepository = sachRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void autoSyncMissingVectorsOnStartup() {
        if (!aiConfig.isConfigured() || !aiConfig.isAutoSyncOnStartup() || isRunningInTest()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                List<Sach> books = sachRepository.findAllWithDetails();
                long missing = books.stream()
                        .filter(b -> b.getVectorNguNghia() == null
                                || b.getVectorNguNghia().length != aiConfig.getEmbeddingDimensions())
                        .count();
                if (missing > 0) {
                    log.info("Phát hiện {}/{} đầu sách chưa có vector ngữ nghĩa pgvector. Bắt đầu đồng bộ ngầm...",
                            missing, books.size());
                    Map<String, Object> res = syncAllBookVectors(false);
                    log.info("Đồng bộ ngầm pgvector hoàn tất: {}", res);
                }
            } catch (Exception ex) {
                log.warn("Đồng bộ ngầm pgvector khi khởi động tạm dừng: {}", ex.getMessage());
            }
        });
    }

    /**
     * Ghép toàn bộ thông tin liên quan của 1 cuốn sách thành đoạn văn bản ngữ nghĩa trước khi đưa vào Embedding Model.
     */
    public String buildSemanticDocument(Sach book) {
        String author = book.getTacGia() != null ? book.getTacGia().getTenTacGia() : "";
        String category = book.getDanhMuc() != null ? book.getDanhMuc().getTenDanhMuc() : "";
        String subCategory = book.getTenDanhMucCon() != null ? book.getTenDanhMucCon() : "";
        String publisher = book.getNhaXuatBan() != null ? book.getNhaXuatBan().getTenNxb() : "";
        String summary = book.getTomTatNgan() != null ? book.getTomTatNgan() : "";
        String keywords = book.getTuKhoa() != null ? book.getTuKhoa() : "";

        return book.getTieuDe()
                + " | Tác giả: " + author
                + " | Danh mục: " + category + (subCategory.isBlank() ? "" : " - " + subCategory)
                + " | NXB: " + publisher
                + " | Tóm tắt: " + summary
                + " | Từ khóa: " + keywords;
    }

    /**
     * Gọi Google Gemini Embedding REST API sinh mảng vector 768 chiều.
     * Trả về null nếu chưa cấu hình GEMINI_API_KEY hoặc gặp sự cố mạng/quota.
     */
    @SuppressWarnings("unchecked")
    public float[] embedText(String text) {
        if (!aiConfig.isConfigured() || text == null || text.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", "models/" + aiConfig.getEmbeddingModel(),
                    "content", Map.of("parts", List.of(Map.of("text", text))),
                    "outputDimensionality", aiConfig.getEmbeddingDimensions()
            );

            Map<String, Object> response = geminiRestClient.post()
                    .uri("/models/{model}:embedContent?key={key}", aiConfig.getEmbeddingModel(), aiConfig.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !(response.get("embedding") instanceof Map<?, ?> embeddingObj)) {
                return null;
            }
            Object valuesObj = embeddingObj.get("values");
            if (!(valuesObj instanceof List<?> values) || values.isEmpty()) {
                return null;
            }

            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = ((Number) values.get(i)).floatValue();
            }
            return vector;
        } catch (Exception ex) {
            log.warn("Không thể gọi Gemini Embedding API: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Đồng bộ vector 768 chiều cho toàn bộ sách trong DB (POST /api/admin/books/sync-vectors).
     * Lưu từng cuốn theo transaction ngắn để không giữ lock DB lâu khi gọi HTTP ra ngoài.
     */
    public Map<String, Object> syncAllBookVectors(boolean force) {
        List<Sach> allBooks = sachRepository.findAllWithDetails();
        int total = allBooks.size();

        if (!aiConfig.isConfigured()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("geminiConfigured", false);
            result.put("totalBooks", total);
            result.put("syncedCount", 0);
            result.put("skippedCount", total);
            result.put("failedCount", 0);
            result.put("note", "Chưa cấu hình biến môi trường GEMINI_API_KEY. Hệ thống đang chạy ở chế độ Smart DB Fallback.");
            return result;
        }

        if (!syncingInProgress.compareAndSet(false, true)) {
            Map<String, Object> busy = new LinkedHashMap<>();
            busy.put("geminiConfigured", true);
            busy.put("totalBooks", total);
            busy.put("syncedCount", 0);
            busy.put("skippedCount", total);
            busy.put("failedCount", 0);
            busy.put("note", "Tiến trình đồng bộ vector đang chạy ngầm, vui lòng đợi ít giây.");
            return busy;
        }

        int synced = 0;
        int skipped = 0;
        int failed = 0;
        try {
            for (Sach book : allBooks) {
                if (!force && book.getVectorNguNghia() != null
                        && book.getVectorNguNghia().length == aiConfig.getEmbeddingDimensions()) {
                    skipped++;
                    continue;
                }
                float[] vector = embedText(buildSemanticDocument(book));
                if (vector != null && vector.length == aiConfig.getEmbeddingDimensions()) {
                    String literal = toVectorLiteral(vector);
                    String bookId = book.getId();
                    transactionTemplate.executeWithoutResult(status -> sachRepository.updateVectorById(bookId, literal));
                    synced++;
                } else {
                    failed++;
                }
            }
        } finally {
            syncingInProgress.set(false);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("geminiConfigured", true);
        result.put("totalBooks", total);
        result.put("syncedCount", synced);
        result.put("skippedCount", skipped);
        result.put("failedCount", failed);
        result.put("note", "Đồng bộ vector ngữ nghĩa pgvector hoàn tất.");
        return result;
    }

    /**
     * Tự động đồng bộ ngầm vector ngữ nghĩa 768 chiều cho 1 đầu sách ngay sau khi Admin thêm mới hoặc chỉnh sửa (Phần A1).
     */
    public void embedSingleBookAsync(String bookId) {
        if (bookId == null || bookId.isBlank() || !aiConfig.isConfigured() || isRunningInTest()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                sachRepository.findByIdWithDetails(bookId).ifPresent(book -> {
                    float[] vector = embedText(buildSemanticDocument(book));
                    if (vector != null && vector.length == aiConfig.getEmbeddingDimensions()) {
                        String literal = toVectorLiteral(vector);
                        transactionTemplate.executeWithoutResult(
                                status -> sachRepository.updateVectorById(book.getId(), literal)
                        );
                        log.info("Đã tự động cập nhật vector pgvector cho sách {}", book.getId());
                    }
                });
            } catch (Exception ex) {
                log.warn("Không thể tự động cập nhật vector cho sách {}: {}", bookId, ex.getMessage());
            }
        });
    }

    /**
     * Tìm top ID sách tương đồng ngữ nghĩa nhất qua toán tử Cosine (<=>) của pgvector.
     */
    public List<String> findSemanticMatchIds(String queryText, int limit) {
        float[] queryVector = embedText(queryText);
        if (queryVector == null || queryVector.length != aiConfig.getEmbeddingDimensions()) {
            return List.of();
        }
        try {
            return sachRepository.findTopIdsBySemanticSimilarity(toVectorLiteral(queryVector), Math.max(1, limit));
        } catch (Exception ex) {
            log.warn("Lỗi truy vấn pgvector: {}", ex.getMessage());
            return List.of();
        }
    }

    /**
     * Chuyển float[] sang chuỗi chuẩn pgvector: "[0.0123,-0.0456,...]".
     */
    public String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 10);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private boolean isRunningInTest() {
        try {
            Class.forName("org.junit.jupiter.api.Test");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
