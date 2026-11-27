package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.dto.request.ChangePasswordRequest;
import com.trithucviet.bookstore.dto.request.ForgotPasswordRequest;
import com.trithucviet.bookstore.dto.request.LoginRequest;
import com.trithucviet.bookstore.dto.request.RegisterRequest;
import com.trithucviet.bookstore.dto.request.ResetPasswordRequest;
import com.trithucviet.bookstore.dto.request.UpdateProfileRequest;
import com.trithucviet.bookstore.dto.response.AuthResponse;
import com.trithucviet.bookstore.dto.response.UserDto;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Xác thực & Hồ sơ cá nhân (Bước 2 - phần 1):
 * - Đăng ký tài khoản khách hàng mới (mặc định vai trò CUSTOMER, mã hóa mật
 * khẩu BCrypt).
 * - Đăng nhập Mobile App (Platform Guard chặn ADMIN / WAREHOUSE_MANAGER / CSKH,
 * kiểm tra biKhoa).
 * - Đăng nhập Web Admin (Platform Guard chặn CUSTOMER, kiểm tra biKhoa).
 * - Xem hồ sơ, cập nhật họ tên / SĐT và đổi mật khẩu tài khoản đang đăng nhập.
 */
@Service
public class AuthService {

    private static final String DEMO_SEED_PASSWORD = "123456";
    private static final long OTP_TTL_SECONDS = 600L;
    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private record OtpRecord(String code, Instant expiresAt) {
    }

    private final Map<String, OtpRecord> resetOtpStore = new ConcurrentHashMap<>();
    private final TkNguoiDungRepository tkNguoiDungRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            TkNguoiDungRepository tkNguoiDungRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.tkNguoiDungRepository = tkNguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Đăng ký khách hàng mới cho Mobile App.
     * Vai trò luôn được cố định là CUSTOMER ở tầng Service để ngăn client tự truyền
     * vaiTro ADMIN/CSKH.
     */
    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (tkNguoiDungRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email " + email + " đã được đăng ký trước đó.");
        }

        TkNguoiDung user = new TkNguoiDung();
        user.setId("nd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        user.setHoTen(request.hoTen().trim());
        user.setEmail(email);
        user.setMatKhauMaHoa(passwordEncoder.encode(request.matKhau()));
        user.setSoDienThoai(normalizeOptionalPhone(request.soDienThoai()));
        user.setVaiTro(VaiTro.CUSTOMER);
        user.setBiKhoa(Boolean.FALSE);
        user.setTongChiTieu(BigDecimal.ZERO);
        user.setSoDonHang(0);
        user.setNgayTao(LocalDateTime.now());

        tkNguoiDungRepository.saveAndFlush(user);
        return buildAuthResponse(user);
    }

    /**
     * Đăng nhập cho Mobile App (chỉ chấp nhận tài khoản CUSTOMER đang hoạt động).
     * Kiểm tra mật khẩu trước khi báo lỗi Platform Guard hoặc Khóa tài khoản để
     * tránh lộ trạng thái tài khoản qua đoán mò.
     */
    @Transactional(readOnly = true)
    public AuthResponse loginMobile(LoginRequest request) {
        TkNguoiDung user = verifyCredentials(request);

        // Quy tắc 1.1 (Platform Guard): chặn nhân sự nội bộ đăng nhập nhầm lên Mobile
        // App khách hàng
        // Thông báo trung tính theo chuẩn app TMĐT để không lộ thông tin hệ thống quản
        // trị nội bộ
        if (user.getVaiTro() != VaiTro.CUSTOMER) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản này không hỗ trợ đăng nhập trên ứng dụng mua sắm. Vui lòng sử dụng tài khoản khách hàng hoặc đăng ký tài khoản mới.");
        }

        // Quy tắc 1.2: chặn tài khoản đang bị Admin khóa
        if (Boolean.TRUE.equals(user.getBiKhoa())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản của bạn đang tạm khóa do vi phạm chính sách hoặc lý do bảo mật. Vui lòng liên hệ CSKH để được hỗ trợ.");
        }

        return buildAuthResponse(user);
    }

    /**
     * Đăng nhập cho Web Admin Dashboard (chỉ chấp nhận ADMIN, WAREHOUSE_MANAGER,
     * CSKH).
     */
    @Transactional(readOnly = true)
    public AuthResponse loginAdmin(LoginRequest request) {
        TkNguoiDung user = verifyCredentials(request);

        // Quy tắc 1.1 (Platform Guard): chặn khách hàng đăng nhập vào Web Dashboard nội
        // bộ
        if (user.getVaiTro() == VaiTro.CUSTOMER) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản này không có quyền truy cập Cổng quản trị nội bộ.");
        }

        if (Boolean.TRUE.equals(user.getBiKhoa())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản nhân sự hiện đang tạm khóa. Vui lòng liên hệ Quản trị viên để được hỗ trợ.");
        }

        return buildAuthResponse(user);
    }

    /**
     * Lấy thông tin hồ sơ của chính người dùng đang đăng nhập (Mobile App và Web
     * Admin).
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String userId) {
        return toUserDto(getActiveAccount(userId));
    }

    /**
     * Cập nhật họ tên & số điện thoại cá nhân cho tài khoản đang đăng nhập.
     */
    @Transactional
    public UserDto updateProfile(String userId, UpdateProfileRequest request) {
        TkNguoiDung user = getActiveAccount(userId);
        user.setHoTen(request.hoTen().trim());
        user.setSoDienThoai(normalizeOptionalPhone(request.soDienThoai()));
        tkNguoiDungRepository.saveAndFlush(user);
        return toUserDto(user);
    }

    /**
     * Đổi mật khẩu tài khoản đang đăng nhập và nâng cấp lưu trữ bằng mã băm BCrypt
     * chuẩn.
     */
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        TkNguoiDung user = getActiveAccount(userId);

        if (!matchesPassword(request.matKhauCu(), user.getMatKhauMaHoa())) {
            throw new BusinessException("Mật khẩu hiện tại không chính xác.");
        }
        if (request.matKhauCu().equals(request.matKhauMoi())) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        user.setMatKhauMaHoa(passwordEncoder.encode(request.matKhauMoi()));
        tkNguoiDungRepository.saveAndFlush(user);
    }

    // --- Quên mật khẩu & Đặt lại mật khẩu bằng OTP 6 chữ số (Phần B2) ---

    @Transactional(readOnly = true)
    public Map<String, Object> requestPasswordResetOtp(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        TkNguoiDung user = tkNguoiDungRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy tài khoản nào đăng ký với email " + email + "."
                ));
        if (user.getVaiTro() != VaiTro.CUSTOMER) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản nội bộ vui lòng liên hệ Quản trị viên để cấp lại mật khẩu."
            );
        }
        if (Boolean.TRUE.equals(user.getBiKhoa())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản đang bị khóa, không thể đặt lại mật khẩu."
            );
        }

        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        resetOtpStore.put(email, new OtpRecord(otp, Instant.now().plusSeconds(OTP_TTL_SECONDS)));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("email", email);
        out.put("expiresInSeconds", OTP_TTL_SECONDS);
        out.put("demoOtp", otp);
        return out;
    }

    @Transactional
    public void resetPasswordWithOtp(ResetPasswordRequest request) {
        String email = normalizeEmail(request.email());
        OtpRecord record = resetOtpStore.get(email);
        if (record == null || Instant.now().isAfter(record.expiresAt())) {
            resetOtpStore.remove(email);
            throw new BusinessException("Mã OTP đã hết hạn hoặc chưa được yêu cầu. Vui lòng gửi lại mã mới.");
        }
        if (!record.code().equals(request.otp().trim())) {
            throw new BusinessException("Mã OTP không chính xác. Vui lòng kiểm tra lại.");
        }

        TkNguoiDung user = tkNguoiDungRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        user.setMatKhauMaHoa(passwordEncoder.encode(request.matKhauMoi()));
        tkNguoiDungRepository.saveAndFlush(user);
        resetOtpStore.remove(email);
    }

    // --- Các hàm tiện ích dùng chung ---

    private TkNguoiDung verifyCredentials(LoginRequest request) {
        String email = normalizeEmail(request.email());
        TkNguoiDung user = tkNguoiDungRepository.findByEmailIgnoreCase(email)
                .orElseThrow(
                        () -> new BusinessException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác."));

        if (!matchesPassword(request.matKhau(), user.getMatKhauMaHoa())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác.");
        }
        return user;
    }

    private TkNguoiDung getActiveAccount(String userId) {
        TkNguoiDung user = tkNguoiDungRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "Phiên đăng nhập không hợp lệ hoặc tài khoản không tồn tại."));

        if (Boolean.TRUE.equals(user.getBiKhoa())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản của bạn đang tạm khóa do vi phạm chính sách hoặc lý do bảo mật. Vui lòng liên hệ CSKH 1900 6886 để được hỗ trợ.");
        }
        return user;
    }

    /**
     * Đối chiếu mật khẩu:
     * - Tài khoản mới đăng ký hoặc đã đổi mật khẩu sẽ bắt đầu bằng tiền tố BCrypt
     * ($2a$/$2b$/$2y$) -> đối chiếu qua PasswordEncoder.
     * - 19 tài khoản dữ liệu mẫu trong database/02_seed_data.sql đang lưu chuỗi đại
     * diện (hash_kh_123, hash_admin_123, ...)
     * chưa băm BCrypt nên hỗ trợ đăng nhập bằng mật khẩu demo "123456" (hoặc đúng
     * chuỗi giữ chỗ đó).
     */
    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return DEMO_SEED_PASSWORD.equals(rawPassword) || storedPassword.equals(rawPassword);
    }

    private AuthResponse buildAuthResponse(TkNguoiDung user) {
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getVaiTro());
        return new AuthResponse(token, "Bearer", toUserDto(user));
    }

    private UserDto toUserDto(TkNguoiDung user) {
        return new UserDto(
                user.getId(),
                user.getHoTen(),
                user.getEmail(),
                user.getSoDienThoai(),
                user.getVaiTro(),
                user.getBiKhoa(),
                user.getTongChiTieu(),
                user.getSoDonHang(),
                user.getNgayTao()
        );

    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }
}
