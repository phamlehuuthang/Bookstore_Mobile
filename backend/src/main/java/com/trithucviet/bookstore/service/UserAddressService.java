package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.request.AddressUpsertRequest;
import com.trithucviet.bookstore.dto.request.CreateStaffRequest;
import com.trithucviet.bookstore.dto.response.AddressDto;
import com.trithucviet.bookstore.dto.response.UserDto;
import com.trithucviet.bookstore.entity.SoDiaChi;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.SoDiaChiRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Sổ địa chỉ nhận hàng (Bước 2 - phần 2) & Quản trị Khách hàng/Nhân sự Web Admin (Bước 2 - phần 3):
 * - Mỗi khách hàng chỉ có tối đa 1 địa chỉ macDinh = true tại mọi thời điểm (Quy tắc 1.3).
 * - Địa chỉ đầu tiên của khách luôn tự động đặt làm mặc định.
 * - Khi xóa địa chỉ mặc định mà khách vẫn còn địa chỉ khác, tự động đôn 1 địa chỉ còn lại lên làm mặc định.
 * - Mọi thao tác đọc/sửa/xóa địa chỉ đều lọc theo nguoiDungId lấy từ JWT để chống IDOR.
 * - Nghiệp vụ Admin: xem khách hàng/nhân sự, cấp tài khoản nhân sự, khóa/mở tài khoản, phân quyền RBAC.
 */
@Service
public class UserAddressService {

    private static final String DEFAULT_STAFF_PASSWORD = "123456";

    private final SoDiaChiRepository soDiaChiRepository;
    private final TkNguoiDungRepository tkNguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAddressService(
            SoDiaChiRepository soDiaChiRepository,
            TkNguoiDungRepository tkNguoiDungRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.soDiaChiRepository = soDiaChiRepository;
        this.tkNguoiDungRepository = tkNguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Lấy toàn bộ sổ địa chỉ của khách hàng (địa chỉ mặc định luôn đứng đầu).
     */
    @Transactional(readOnly = true)
    public List<AddressDto> getAddresses(String nguoiDungId) {
        requireActiveCustomer(nguoiDungId);
        return soDiaChiRepository.findByNguoiDungIdOrderByMacDinhDescIdAsc(nguoiDungId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Thêm địa chỉ nhận hàng mới:
     * - Nếu sổ địa chỉ đang trống -> tự động đặt macDinh = true bất kể client gửi false/null.
     * - Nếu địa chỉ mới có macDinh = true -> tự động tắt macDinh của tất cả địa chỉ cũ trong cùng transaction.
     */
    @Transactional
    public AddressDto createAddress(String nguoiDungId, AddressUpsertRequest request) {
        TkNguoiDung customer = requireActiveCustomer(nguoiDungId);
        List<SoDiaChi> existing = soDiaChiRepository.findByNguoiDungIdOrderByMacDinhDescIdAsc(nguoiDungId);

        boolean shouldBeDefault = existing.isEmpty() || Boolean.TRUE.equals(request.macDinh());
        if (shouldBeDefault && !existing.isEmpty()) {
            for (SoDiaChi item : existing) {
                if (Boolean.TRUE.equals(item.getMacDinh())) {
                    item.setMacDinh(Boolean.FALSE);
                }
            }
            soDiaChiRepository.saveAllAndFlush(existing);
        }

        SoDiaChi address = new SoDiaChi();
        address.setId("dc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        address.setNguoiDung(customer);
        applyAddressFields(address, request);
        address.setMacDinh(shouldBeDefault);

        soDiaChiRepository.saveAndFlush(address);
        return toDto(address);
    }

    /**
     * Cập nhật thông tin 1 địa chỉ thuộc về khách hàng:
     * - request.macDinh() == null: giữ nguyên cờ mặc định hiện tại.
     * - request.macDinh() == true: hạ các địa chỉ khác về false và đặt địa chỉ này là mặc định.
     * - request.macDinh() == false trên địa chỉ đang là mặc định:
     *   nếu khách còn địa chỉ khác thì đôn 1 địa chỉ khác lên mặc định (nhất quán quy tắc 1A);
     *   nếu là địa chỉ duy nhất thì vẫn giữ macDinh = true (nhất quán quy tắc 2A).
     */
    @Transactional
    public AddressDto updateAddress(String nguoiDungId, String addressId, AddressUpsertRequest request) {
        requireActiveCustomer(nguoiDungId);
        SoDiaChi target = findOwnedAddressOrThrow(nguoiDungId, addressId);

        applyAddressFields(target, request);
        List<SoDiaChi> allAddresses = soDiaChiRepository.findByNguoiDungIdOrderByMacDinhDescIdAsc(nguoiDungId);

        if (Boolean.TRUE.equals(request.macDinh())) {
            for (SoDiaChi item : allAddresses) {
                item.setMacDinh(item.getId().equals(target.getId()));
            }
        } else if (Boolean.FALSE.equals(request.macDinh()) && Boolean.TRUE.equals(target.getMacDinh())) {
            SoDiaChi fallback = allAddresses.stream()
                    .filter(item -> !item.getId().equals(target.getId()))
                    .findFirst()
                    .orElse(null);
            if (fallback != null) {
                target.setMacDinh(Boolean.FALSE);
                fallback.setMacDinh(Boolean.TRUE);
            } else {
                // Địa chỉ duy nhất trong sổ -> giữ mặc định để không mất địa chỉ mặc định khi thanh toán
                target.setMacDinh(Boolean.TRUE);
            }
        }

        soDiaChiRepository.saveAllAndFlush(allAddresses);
        return toDto(target);
    }

    /**
     * Chuyển 1 địa chỉ sang trạng thái mặc định và đồng loạt tắt mặc định ở các địa chỉ còn lại.
     */
    @Transactional
    public AddressDto setDefaultAddress(String nguoiDungId, String addressId) {
        requireActiveCustomer(nguoiDungId);
        SoDiaChi target = findOwnedAddressOrThrow(nguoiDungId, addressId);

        List<SoDiaChi> allAddresses = soDiaChiRepository.findByNguoiDungIdOrderByMacDinhDescIdAsc(nguoiDungId);
        for (SoDiaChi item : allAddresses) {
            item.setMacDinh(item.getId().equals(target.getId()));
        }

        soDiaChiRepository.saveAllAndFlush(allAddresses);
        return toDto(target);
    }

    /**
     * Xóa 1 địa chỉ nhận hàng:
     * Nếu địa chỉ vừa xóa là macDinh = true và khách vẫn còn địa chỉ khác,
     * tự động đôn địa chỉ đầu tiên còn lại lên làm mặc định rồi trả về danh sách mới nhất.
     */
    @Transactional
    public List<AddressDto> deleteAddress(String nguoiDungId, String addressId) {
        requireActiveCustomer(nguoiDungId);
        SoDiaChi target = findOwnedAddressOrThrow(nguoiDungId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(target.getMacDinh());

        soDiaChiRepository.delete(target);
        soDiaChiRepository.flush();

        List<SoDiaChi> remaining = soDiaChiRepository.findByNguoiDungIdOrderByMacDinhDescIdAsc(nguoiDungId);
        if (wasDefault && !remaining.isEmpty()) {
            boolean hasDefault = remaining.stream().anyMatch(item -> Boolean.TRUE.equals(item.getMacDinh()));
            if (!hasDefault) {
                SoDiaChi promoted = remaining.get(0);
                promoted.setMacDinh(Boolean.TRUE);
                soDiaChiRepository.saveAndFlush(promoted);
            }
        }

        return remaining.stream().map(this::toDto).toList();
    }

    // =========================================================================
    // NGHIỆP VỤ QUẢN TRỊ KHÁCH HÀNG & NHÂN SỰ WEB ADMIN (BƯỚC 2 - PHẦN 3)
    // =========================================================================

    /**
     * Lấy danh sách tài khoản Khách hàng (CUSTOMER) cho ADMIN và CSKH tra cứu.
     */
    @Transactional(readOnly = true)
    public List<UserDto> getCustomers(String keyword) {
        return filterUsersByKeyword(tkNguoiDungRepository.findByVaiTroOrdered(VaiTro.CUSTOMER), keyword);
    }

    /**
     * Lấy danh sách nhân sự nội bộ (ADMIN, WAREHOUSE_MANAGER, CSKH) cho ADMIN quản lý.
     */
    @Transactional(readOnly = true)
    public List<UserDto> getStaff(String keyword) {
        return filterUsersByKeyword(tkNguoiDungRepository.findStaffOrdered(), keyword);
    }

    /**
     * Cấp tài khoản nhân sự mới cho Web Admin:
     * - Chỉ chấp nhận vai trò nội bộ (ADMIN, WAREHOUSE_MANAGER, CSKH), chặn tạo CUSTOMER.
     * - Kiểm tra trùng email (không phân biệt hoa/thường) và mã hóa mật khẩu bằng BCrypt.
     */
    @Transactional
    public UserDto createStaffAccount(CreateStaffRequest request) {
        if (request.vaiTro() == VaiTro.CUSTOMER) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể cấp tài khoản Khách hàng từ mục nhân sự nội bộ."
            );
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (tkNguoiDungRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Email công vụ này đã tồn tại trong hệ thống."
            );
        }

        String rawPassword = DEFAULT_STAFF_PASSWORD;
        if (request.matKhau() != null && !request.matKhau().isBlank()) {
            rawPassword = request.matKhau().trim();
            if (rawPassword.length() < 6) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Mật khẩu nhân sự phải có ít nhất 6 ký tự."
                );
            }
        }

        TkNguoiDung staff = new TkNguoiDung();
        staff.setId("nd_nv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        staff.setHoTen(request.hoTen().trim());
        staff.setEmail(normalizedEmail);
        staff.setMatKhauMaHoa(passwordEncoder.encode(rawPassword));
        staff.setSoDienThoai(request.soDienThoai().trim());
        staff.setVaiTro(request.vaiTro());
        staff.setBiKhoa(Boolean.FALSE);
        staff.setTongChiTieu(BigDecimal.ZERO);
        staff.setSoDonHang(0);
        staff.setNgayTao(LocalDateTime.now());

        tkNguoiDungRepository.saveAndFlush(staff);
        return toUserDto(staff);
    }

    /**
     * Khóa hoặc mở khóa tài khoản (chặn tự khóa chính tài khoản đang đăng nhập).
     */
    @Transactional
    public UserDto toggleUserLock(String actorId, String targetUserId) {
        TkNguoiDung target = findUserOrThrow(targetUserId);
        if (actorId != null && actorId.equals(target.getId())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể tự khóa tài khoản quản trị đang đăng nhập."
            );
        }

        target.setBiKhoa(!Boolean.TRUE.equals(target.getBiKhoa()));
        tkNguoiDungRepository.saveAndFlush(target);
        return toUserDto(target);
    }

    /**
     * Đổi vai trò phân quyền (RBAC) cho nhân sự nội bộ:
     * - Không cho đổi thành CUSTOMER hoặc đổi vai trò của tài khoản CUSTOMER.
     * - Chặn Admin tự thay đổi vai trò chính mình để tránh mất quyền quản trị.
     */
    @Transactional
    public UserDto updateStaffRole(String actorId, String targetUserId, VaiTro newRole) {
        if (newRole == null || newRole == VaiTro.CUSTOMER) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Vai trò nhân sự hợp lệ gồm: ADMIN, WAREHOUSE_MANAGER hoặc CSKH."
            );
        }

        TkNguoiDung target = findUserOrThrow(targetUserId);
        if (target.getVaiTro() == VaiTro.CUSTOMER) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể gán quyền nhân sự nội bộ cho tài khoản Khách hàng."
            );
        }

        if (actorId != null && actorId.equals(target.getId()) && target.getVaiTro() != newRole) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể tự thay đổi vai trò của chính tài khoản đang đăng nhập."
            );
        }

        target.setVaiTro(newRole);
        tkNguoiDungRepository.saveAndFlush(target);
        return toUserDto(target);
    }

    // --- Các hàm tiện ích dùng chung ---

    private List<UserDto> filterUsersByKeyword(List<TkNguoiDung> users, String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return users.stream()
                .filter(user -> normalized.isEmpty()
                        || containsIgnoreCase(user.getHoTen(), normalized)
                        || containsIgnoreCase(user.getEmail(), normalized)
                        || containsIgnoreCase(user.getSoDienThoai(), normalized))
                .map(this::toUserDto)
                .toList();
    }

    private boolean containsIgnoreCase(String value, String lowerKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private TkNguoiDung findUserOrThrow(String userId) {
        return tkNguoiDungRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy tài khoản người dùng."
                ));
    }

    private UserDto toUserDto(TkNguoiDung user) {
        return new UserDto(
                user.getId(),
                user.getHoTen(),
                user.getEmail(),
                user.getSoDienThoai(),
                user.getVaiTro(),
                Boolean.TRUE.equals(user.getBiKhoa()),
                user.getTongChiTieu(),
                user.getSoDonHang(),
                user.getNgayTao()
        );
    }

    private TkNguoiDung requireActiveCustomer(String nguoiDungId) {
        TkNguoiDung user = tkNguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "Phiên đăng nhập không hợp lệ hoặc tài khoản không tồn tại."
                ));

        if (Boolean.TRUE.equals(user.getBiKhoa())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ CSKH để được hỗ trợ."
            );
        }
        return user;
    }

    private SoDiaChi findOwnedAddressOrThrow(String nguoiDungId, String addressId) {
        return soDiaChiRepository.findByIdAndNguoiDungId(addressId, nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy địa chỉ nhận hàng."
                ));
    }

    private void applyAddressFields(SoDiaChi target, AddressUpsertRequest request) {
        target.setHoTenNguoiNhan(request.hoTenNguoiNhan().trim());
        target.setSoDienThoai(request.soDienThoai().trim());
        target.setTinhThanh(request.tinhThanh().trim());
        target.setQuanHuyen(request.quanHuyen().trim());
        target.setPhuongXa(request.phuongXa().trim());
        target.setDiaChiChiTiet(request.diaChiChiTiet().trim());
    }

    private AddressDto toDto(SoDiaChi entity) {
        String fullAddress = String.join(", ",
                entity.getDiaChiChiTiet(),
                entity.getPhuongXa(),
                entity.getQuanHuyen(),
                entity.getTinhThanh()
        );
        return new AddressDto(
                entity.getId(),
                entity.getHoTenNguoiNhan(),
                entity.getSoDienThoai(),
                entity.getTinhThanh(),
                entity.getQuanHuyen(),
                entity.getPhuongXa(),
                entity.getDiaChiChiTiet(),
                fullAddress,
                Boolean.TRUE.equals(entity.getMacDinh())
        );
    }
}
