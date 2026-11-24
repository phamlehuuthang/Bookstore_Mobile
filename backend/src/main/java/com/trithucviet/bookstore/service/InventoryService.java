package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.request.CreateInventoryReceiptRequest;
import com.trithucviet.bookstore.dto.response.DashboardBookItemDto;
import com.trithucviet.bookstore.dto.response.InventoryReceiptDto;
import com.trithucviet.bookstore.dto.response.InventoryReceiptItemDto;
import com.trithucviet.bookstore.entity.ChiTietPhieuNhap;
import com.trithucviet.bookstore.entity.PhieuNhapKho;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.ChiTietPhieuNhapRepository;
import com.trithucviet.bookstore.repository.PhieuNhapKhoRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Quản lý Nhập kho & Cảnh báo Tồn kho (Bước 8 - Khối A).
 *
 * Quy tắc nghiệp vụ:
 * - Ngưỡng cảnh báo sách sắp hết hàng thống nhất: ton_kho <= 5 cuốn.
 * - Khi lập phiếu nhập kho mới (ADMIN & WAREHOUSE_MANAGER):
 *   1. Kiểm tra toàn bộ đầu sách hợp lệ, số lượng > 0, giá nhập >= 0.
 *   2. Tự sinh mã phiếu PNK-YYYYMM-XXXX duy nhất và ghi nhận tên người lập từ JWT.
 *   3. Cộng tồn kho tự động: sach.ton_kho = sach.ton_kho + so_luong trong cùng 1 @Transactional.
 */
@Service
public class InventoryService {

    public static final int LOW_STOCK_THRESHOLD = 5;
    private static final DateTimeFormatter RECEIPT_MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    private final PhieuNhapKhoRepository phieuNhapKhoRepository;
    private final ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;
    private final SachRepository sachRepository;
    private final TkNguoiDungRepository tkNguoiDungRepository;

    public InventoryService(
            PhieuNhapKhoRepository phieuNhapKhoRepository,
            ChiTietPhieuNhapRepository chiTietPhieuNhapRepository,
            SachRepository sachRepository,
            TkNguoiDungRepository tkNguoiDungRepository
    ) {
        this.phieuNhapKhoRepository = phieuNhapKhoRepository;
        this.chiTietPhieuNhapRepository = chiTietPhieuNhapRepository;
        this.sachRepository = sachRepository;
        this.tkNguoiDungRepository = tkNguoiDungRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryReceiptDto> getAllReceipts() {
        List<PhieuNhapKho> receipts = phieuNhapKhoRepository.findAllOrdered();
        if (receipts.isEmpty()) {
            return List.of();
        }

        List<String> receiptIds = receipts.stream().map(PhieuNhapKho::getId).toList();
        Map<String, List<ChiTietPhieuNhap>> itemsByReceiptId = new HashMap<>();
        for (ChiTietPhieuNhap item : chiTietPhieuNhapRepository.findByPhieuNhapIdInWithSach(receiptIds)) {
            itemsByReceiptId
                    .computeIfAbsent(item.getPhieuNhap().getId(), k -> new ArrayList<>())
                    .add(item);
        }

        return receipts.stream()
                .map(receipt -> toReceiptDto(receipt, itemsByReceiptId.getOrDefault(receipt.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardBookItemDto> getLowStockBooks() {
        return sachRepository.findAllWithDetails().stream()
                .filter(book -> (book.getTonKho() == null ? 0 : book.getTonKho()) <= LOW_STOCK_THRESHOLD)
                .sorted(Comparator
                        .comparingInt((Sach s) -> s.getTonKho() == null ? 0 : s.getTonKho())
                        .thenComparing(Comparator.comparingInt((Sach s) -> s.getDaBan() == null ? 0 : s.getDaBan()).reversed()))
                .map(InventoryService::toDashboardBookItemDto)
                .toList();
    }

    /**
     * Lập phiếu nhập kho mới và tự động cộng tồn kho cho từng đầu sách trong 1 transaction nguyên tử.
     */
    @Transactional
    public InventoryReceiptDto createReceipt(String staffUserId, CreateInventoryReceiptRequest request) {
        TkNguoiDung staff = tkNguoiDungRepository.findById(staffUserId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "Phiên đăng nhập nhân sự không hợp lệ."
                ));
        if (Boolean.TRUE.equals(staff.getBiKhoa())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Tài khoản nhân sự đã bị khóa.");
        }

        LocalDateTime now = LocalDateTime.now();
        PhieuNhapKho receipt = new PhieuNhapKho();
        receipt.setId("pnk_" + shortUuid());
        receipt.setMaPhieu(generateUniqueReceiptCode(now));
        receipt.setTenNhaCungCap(request.tenNhaCungCap().trim());
        receipt.setNguoiTao(staff.getHoTen());
        receipt.setGhiChu(normalizeBlankToNull(request.ghiChu()));
        receipt.setNgayTao(now);

        int totalQuantity = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<ChiTietPhieuNhap> receiptItems = new ArrayList<>();
        Map<String, Sach> touchedBooks = new HashMap<>();

        for (CreateInventoryReceiptRequest.ItemRequest itemReq : request.items()) {
            String bookId = itemReq.sachId().trim();
            Sach book = touchedBooks.get(bookId);
            if (book == null) {
                book = sachRepository.findById(bookId)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy đầu sách với mã: " + bookId
                        ));
                touchedBooks.put(bookId, book);
            }

            int qty = itemReq.soLuong();
            BigDecimal importPrice = itemReq.giaNhap();
            BigDecimal lineCost = importPrice.multiply(BigDecimal.valueOf(qty));

            totalQuantity += qty;
            totalCost = totalCost.add(lineCost);

            // Tự động cộng tồn kho theo Quy tắc 3.2
            int currentStock = book.getTonKho() == null ? 0 : book.getTonKho();
            book.setTonKho(currentStock + qty);

            ChiTietPhieuNhap detail = new ChiTietPhieuNhap();
            detail.setId("ctpn_" + shortUuid());
            detail.setPhieuNhap(receipt);
            detail.setSach(book);
            detail.setSoLuong(qty);
            detail.setGiaNhap(importPrice);
            receiptItems.add(detail);
        }

        receipt.setTongSoLuong(totalQuantity);
        receipt.setTongTienNhap(totalCost);

        phieuNhapKhoRepository.saveAndFlush(receipt);
        chiTietPhieuNhapRepository.saveAllAndFlush(receiptItems);
        sachRepository.saveAllAndFlush(touchedBooks.values());

        return toReceiptDto(receipt, receiptItems);
    }

    public static DashboardBookItemDto toDashboardBookItemDto(Sach book) {
        return new DashboardBookItemDto(
                book.getId(),
                book.getMaIsbn(),
                book.getTieuDe(),
                book.getTacGia() != null ? book.getTacGia().getTenTacGia() : "",
                book.getDanhMuc() != null ? book.getDanhMuc().getTenDanhMuc() : "",
                book.getNhaXuatBan() != null ? book.getNhaXuatBan().getTenNxb() : "",
                book.getAnhBiaUrl(),
                book.getGiaBan(),
                book.getGiaGoc(),
                Boolean.TRUE.equals(book.getDangFlashSale()),
                book.getGiaFlashSale(),
                book.getTonKho() == null ? 0 : book.getTonKho(),
                book.getDaBan() == null ? 0 : book.getDaBan()
        );
    }

    private InventoryReceiptDto toReceiptDto(PhieuNhapKho receipt, List<ChiTietPhieuNhap> details) {
        List<InventoryReceiptItemDto> itemDtos = details.stream()
                .map(d -> new InventoryReceiptItemDto(
                        d.getId(),
                        d.getSach().getId(),
                        d.getSach().getTieuDe(),
                        d.getSach().getMaIsbn(),
                        d.getSoLuong(),
                        d.getGiaNhap(),
                        d.getGiaNhap().multiply(BigDecimal.valueOf(d.getSoLuong()))
                ))
                .toList();

        return new InventoryReceiptDto(
                receipt.getId(),
                receipt.getMaPhieu(),
                receipt.getTenNhaCungCap(),
                receipt.getNguoiTao(),
                receipt.getGhiChu(),
                receipt.getTongSoLuong(),
                receipt.getTongTienNhap(),
                receipt.getNgayTao(),
                itemDtos
        );
    }

    private String generateUniqueReceiptCode(LocalDateTime now) {
        String prefix = "PNK-" + now.format(RECEIPT_MONTH_FMT) + "-";
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
            if (!phieuNhapKhoRepository.existsByMaPhieuIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return prefix + System.currentTimeMillis() % 100000;
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
