package vn.datve.dat_ve.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.datve.dat_ve.auth.dto.AuthResponse;
import vn.datve.dat_ve.auth.dto.UserResponse;
import vn.datve.dat_ve.common.exception.ApiException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    /** So khớp với hash giả khi email không tồn tại để thời gian phản hồi không lộ email nào đã đăng ký. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public AuthResponse register(String email, String password, String fullName) {
        String normalizedEmail = normalize(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", "Email này đã được đăng ký");
        }
        User user = userRepository.save(
                new User(normalizedEmail, passwordEncoder.encode(password), fullName.strip(), Role.USER));
        return tokenService.issue(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(normalize(email)).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        if (!passwordEncoder.matches(password, hash) || user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng");
        }
        return tokenService.issue(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(long userId) {
        return UserResponse.from(find(userId));
    }

    @Transactional
    public UserResponse updateProfile(long userId, String fullName) {
        User user = find(userId);
        user.changeFullName(fullName.strip());
        return UserResponse.from(user);
    }

    @Transactional
    public AuthResponse changePassword(long userId, String currentPassword, String newPassword) {
        User user = find(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD", "Mật khẩu hiện tại không đúng");
        }
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        return tokenService.issue(user);
    }

    private User find(long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Tài khoản không còn tồn tại"));
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
