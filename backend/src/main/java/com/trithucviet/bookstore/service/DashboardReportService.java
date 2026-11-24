package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.response.AdminDashboardDto;
import com.trithucviet.bookstore.dto.response.DashboardBookItemDto;
import com.trithucviet.bookstore.dto.response.RevenuePointDto;
import com.trithucviet.bookstore.entity.ChiTietDonHang;
import com.trithucviet.bookstore.entity.ChiTietPhieuNhap;
import com.trithucviet.bookstore.entity.DonHang;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.YeuCauDoiTra;
import com.trithucviet.bookstore.entity.enums.TrangThaiDoiTra;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.ChiTietDonHangRepository;
import com.trithucviet.bookstore.repository.ChiTietPhieuNhapRepository;
import com.trithucviet.bookstore.repository.DonHangRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import com.trithucviet.bookstore.repository.YeuCauDoiTraRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Thống kê Dashboard theo RBAC & Xuất Báo cáo Doanh thu CSV (Bước 8 - Khối B & C).
 *
 * Quy tắc dữ liệu & phân quyền:
 * - Doanh thu thực tế chỉ tính trên các đơn hàng đã giao thành công (trang_thai_don = DA_GIAO).
 * - Lợi nhuận gộp: ưu tiên trừ giá nhập gần nhất của sách trong chi_tiet_phieu_nhap;
 *   nếu sách chưa từng lập phiếu nhập, ước tính giá vốn = 65% giá bán (biên lợi nhuận gộp 35%).
 * - Chỉ ADMIN nhận được dữ liệu doanh thu/lợi nhuận; WAREHOUSE_MANAGER & CSKH nhận null ở các trường tài chính.
 */
@Service
public class DashboardReportService {

    private static final BigDecimal DEFAULT_COST_RATIO = new BigDecimal("0.65");
    private static final DateTimeFormatter DAY_LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM");

    private final SachRepository sachRepository;
    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;
    private final YeuCauDoiTraRepository yeuCauDoiTraRepository;
    private final TkNguoiDungRepository tkNguoiDungRepository;

    public DashboardReportService(
            SachRepository sachRepository,
            DonHangRepository donHangRepository,
            ChiTietDonHangRepository chiTietDonHangRepository,
            ChiTietPhieuNhapRepository chiTietPhieuNhapRepository,
            YeuCauDoiTraRepository yeuCauDoiTraRepository,
            TkNguoiDungRepository tkNguoiDungRepository
    ) {
        this.sachRepository = sachRepository;
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.chiTietPhieuNhapRepository = chiTietPhieuNhapRepository;
        this.yeuCauDoiTraRepository = yeuCauDoiTraRepository;
        this.tkNguoiDungRepository = tkNguoiDungRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard(String staffUserId) {
        TkNguoiDung staff = tkNguoiDungRepository.findById(staffUserId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "Phiên đăng nhập nhân sự không hợp lệ."
                ));

        boolean canViewFinance = staff.getVaiTro() == VaiTro.ADMIN;

        List<Sach> allBooks = sachRepository.findAllWithDetails();
        List<DonHang> allOrders = donHangRepository.findAllOrdered();
        List<YeuCauDoiTra> allReturns = yeuCauDoiTraRepository.findAllWithOrderOrdered();
        List<TkNguoiDung> allCustomers = tkNguoiDungRepository.findByVaiTroOrdered(VaiTro.CUSTOMER);

        List<DashboardBookItemDto> lowStockBooks = allBooks.stream()
                .filter(b -> (b.getTonKho() == null ? 0 : b.getTonKho()) <= InventoryService.LOW_STOCK_THRESHOLD)
                .sorted(Comparator.comparingInt((Sach b) -> b.getTonKho() == null ? 0 : b.getTonKho()))
                .map(InventoryService::toDashboardBookItemDto)
                .toList();

        List<DashboardBookItemDto> topSellingBooks = allBooks.stream()
                .sorted(Comparator.comparingInt((Sach b) -> b.getDaBan() == null ? 0 : b.getDaBan()).reversed())
                .limit(5)
                .map(InventoryService::toDashboardBookItemDto)
                .toList();

        long pendingOrdersCount = allOrders.stream()
                .filter(o -> o.getTrangThaiDon() == TrangThaiDon.CHO_XAC_NHAN)
                .count();
        long pendingReturnsCount = allReturns.stream()
                .filter(r -> r.getTrangThai() == TrangThaiDoiTra.CHO_DUYET)
                .count();

        List<DonHang> deliveredOrders = allOrders.stream()
                .filter(o -> o.getTrangThaiDon() == TrangThaiDon.DA_GIAO)
                .toList();
        long totalDeliveredOrders = deliveredOrders.size();
        long totalCustomers = allCustomers.size();

        if (!canViewFinance) {
            // WAREHOUSE_MANAGER & CSKH không nhận số liệu doanh thu/lợi nhuận
            return new AdminDashboardDto(
                    null,
                    null,
                    totalDeliveredOrders,
                    0.0,
                    totalCustomers,
                    0.0,
                    pendingOrdersCount,
                    pendingReturnsCount,
                    lowStockBooks.size(),
                    InventoryService.LOW_STOCK_THRESHOLD,
                    null,
                    null,
                    null,
                    topSellingBooks,
                    lowStockBooks
            );
        }

        Map<String, BigDecimal> latestImportPriceByBook = buildLatestImportPriceMap();
        Map<String, List<ChiTietDonHang>> itemsByOrder = buildOrderItemsMap(deliveredOrders);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, BigDecimal> profitByOrderId = new HashMap<>();
        for (DonHang order : deliveredOrders) {
            BigDecimal orderRev = safeMoney(order.getTongThanhToan());
            totalRevenue = totalRevenue.add(orderRev);
            profitByOrderId.put(
                    order.getId(),
                    computeOrderProfit(orderRev, itemsByOrder.getOrDefault(order.getId(), List.of()), latestImportPriceByBook)
            );
        }

        List<RevenuePointDto> byDay = aggregateByLast7Days(deliveredOrders, profitByOrderId);
        List<RevenuePointDto> byMonth = aggregateByLast6Months(deliveredOrders, profitByOrderId);
        List<RevenuePointDto> byYear = aggregateByYear(deliveredOrders, profitByOrderId);

        double revenueGrowth = computeLatestMonthGrowth(byMonth);

        return new AdminDashboardDto(
                totalRevenue,
                revenueGrowth,
                totalDeliveredOrders,
                0.0,
                totalCustomers,
                0.0,
                pendingOrdersCount,
                pendingReturnsCount,
                lowStockBooks.size(),
                InventoryService.LOW_STOCK_THRESHOLD,
                byDay,
                byMonth,
                byYear,
                topSellingBooks,
                lowStockBooks
        );
    }

    /**
     * Xuất file báo cáo doanh thu & top sách dạng CSV (UTF-8 BOM để mở trực tiếp trên Excel không lỗi dấu).
     */
    @Transactional(readOnly = true)
    public String exportRevenueCsv(String adminUserId) {
        AdminDashboardDto dashboard = getDashboard(adminUserId);
        List<Sach> topBooks = sachRepository.findAllWithDetails().stream()
                .sorted(Comparator.comparingInt((Sach b) -> b.getDaBan() == null ? 0 : b.getDaBan()).reversed())
                .limit(20)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("Ky bao cao,Doanh thu (VND),So don hang hoan tat,Loi nhuan gop uoc tinh (VND)\n");
        if (dashboard.revenueByMonth() != null) {
            for (RevenuePointDto point : dashboard.revenueByMonth()) {
                sb.append(csvEscape(point.label())).append(',')
                        .append(point.revenue().toPlainString()).append(',')
                        .append(point.orders()).append(',')
                        .append(point.profit().toPlainString()).append('\n');
            }
        }
        sb.append('\n');
        sb.append("Top Sach Ban Chay,Ma ISBN,Gia ban (VND),Da ban,Ton kho\n");
        for (Sach book : topBooks) {
            BigDecimal effectivePrice = Boolean.TRUE.equals(book.getDangFlashSale()) && book.getGiaFlashSale() != null
                    ? book.getGiaFlashSale()
                    : safeMoney(book.getGiaBan());
            sb.append(csvEscape(book.getTieuDe())).append(',')
                    .append(csvEscape(book.getMaIsbn())).append(',')
                    .append(effectivePrice.toPlainString()).append(',')
                    .append(book.getDaBan() == null ? 0 : book.getDaBan()).append(',')
                    .append(book.getTonKho() == null ? 0 : book.getTonKho()).append('\n');
        }
        return sb.toString();
    }

    // --- Các hàm tính toán nội bộ ---

    private Map<String, BigDecimal> buildLatestImportPriceMap() {
        Map<String, BigDecimal> latestPrice = new HashMap<>();
        for (ChiTietPhieuNhap detail : chiTietPhieuNhapRepository.findAllWithSachOrderedByReceiptDateDesc()) {
            latestPrice.putIfAbsent(detail.getSach().getId(), safeMoney(detail.getGiaNhap()));
        }
        return latestPrice;
    }

    private Map<String, List<ChiTietDonHang>> buildOrderItemsMap(List<DonHang> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<String> orderIds = orders.stream().map(DonHang::getId).toList();
        Map<String, List<ChiTietDonHang>> map = new HashMap<>();
        for (ChiTietDonHang item : chiTietDonHangRepository.findByDonHangIdInWithSach(orderIds)) {
            map.computeIfAbsent(item.getDonHang().getId(), k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private BigDecimal computeOrderProfit(
            BigDecimal orderRevenue,
            List<ChiTietDonHang> items,
            Map<String, BigDecimal> latestImportPriceByBook
    ) {
        if (items.isEmpty()) {
            return orderRevenue.multiply(BigDecimal.ONE.subtract(DEFAULT_COST_RATIO))
                    .setScale(0, RoundingMode.HALF_UP);
        }
        BigDecimal totalCost = BigDecimal.ZERO;
        for (ChiTietDonHang item : items) {
            int qty = item.getSoLuong() == null ? 0 : item.getSoLuong();
            BigDecimal importPrice = latestImportPriceByBook.get(item.getSach().getId());
            BigDecimal unitCost = importPrice != null
                    ? importPrice
                    : safeMoney(item.getDonGia()).multiply(DEFAULT_COST_RATIO).setScale(0, RoundingMode.HALF_UP);
            totalCost = totalCost.add(unitCost.multiply(BigDecimal.valueOf(qty)));
        }
        BigDecimal profit = orderRevenue.subtract(totalCost);
        return profit.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : profit;
    }

    private List<RevenuePointDto> aggregateByLast7Days(
            List<DonHang> deliveredOrders,
            Map<String, BigDecimal> profitByOrderId
    ) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, MutableBucket> buckets = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            buckets.put(today.minusDays(i), new MutableBucket());
        }
        for (DonHang order : deliveredOrders) {
            if (order.getNgayTao() == null) continue;
            MutableBucket bucket = buckets.get(order.getNgayTao().toLocalDate());
            if (bucket != null) {
                bucket.add(safeMoney(order.getTongThanhToan()), profitByOrderId.getOrDefault(order.getId(), BigDecimal.ZERO));
            }
        }
        List<RevenuePointDto> result = new ArrayList<>();
        buckets.forEach((date, b) -> result.add(new RevenuePointDto(
                date.format(DAY_LABEL_FMT), b.revenue, b.orders, b.profit
        )));
        return result;
    }

    private List<RevenuePointDto> aggregateByLast6Months(
            List<DonHang> deliveredOrders,
            Map<String, BigDecimal> profitByOrderId
    ) {
        YearMonth current = YearMonth.now();
        Map<YearMonth, MutableBucket> buckets = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            buckets.put(current.minusMonths(i), new MutableBucket());
        }
        for (DonHang order : deliveredOrders) {
            if (order.getNgayTao() == null) continue;
            YearMonth ym = YearMonth.from(order.getNgayTao());
            MutableBucket bucket = buckets.get(ym);
            if (bucket != null) {
                bucket.add(safeMoney(order.getTongThanhToan()), profitByOrderId.getOrDefault(order.getId(), BigDecimal.ZERO));
            }
        }
        List<RevenuePointDto> result = new ArrayList<>();
        buckets.forEach((ym, b) -> result.add(new RevenuePointDto(
                String.format("T%02d/%d", ym.getMonthValue(), ym.getYear()),
                b.revenue,
                b.orders,
                b.profit
        )));
        return result;
    }

    private List<RevenuePointDto> aggregateByYear(
            List<DonHang> deliveredOrders,
            Map<String, BigDecimal> profitByOrderId
    ) {
        int currentYear = LocalDate.now().getYear();
        Map<Integer, MutableBucket> buckets = new TreeMap<>();
        buckets.put(currentYear - 1, new MutableBucket());
        buckets.put(currentYear, new MutableBucket());
        for (DonHang order : deliveredOrders) {
            if (order.getNgayTao() == null) continue;
            int year = order.getNgayTao().getYear();
            buckets.computeIfAbsent(year, k -> new MutableBucket())
                    .add(safeMoney(order.getTongThanhToan()), profitByOrderId.getOrDefault(order.getId(), BigDecimal.ZERO));
        }
        List<RevenuePointDto> result = new ArrayList<>();
        buckets.forEach((year, b) -> result.add(new RevenuePointDto(
                String.valueOf(year), b.revenue, b.orders, b.profit
        )));
        return result;
    }

    private double computeLatestMonthGrowth(List<RevenuePointDto> byMonth) {
        if (byMonth.size() < 2) return 0.0;
        BigDecimal curr = byMonth.get(byMonth.size() - 1).revenue();
        BigDecimal prev = byMonth.get(byMonth.size() - 2).revenue();
        if (prev.compareTo(BigDecimal.ZERO) == 0) {
            return curr.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return curr.subtract(prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(prev, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String csvEscape(String raw) {
        if (raw == null) return "\"\"";
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    private static class MutableBucket {
        BigDecimal revenue = BigDecimal.ZERO;
        long orders = 0L;
        BigDecimal profit = BigDecimal.ZERO;

        void add(BigDecimal rev, BigDecimal prof) {
            this.revenue = this.revenue.add(rev);
            this.orders += 1;
            this.profit = this.profit.add(prof);
        }
    }
}
