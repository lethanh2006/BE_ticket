package vn.datve.dat_ve.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/** Đọc thông tin người gọi từ JWT đã được Spring Security kiểm tra. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static long id(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public static boolean isAdmin(Jwt jwt) {
        return Role.ADMIN.name().equals(jwt.getClaimAsString("role"));
    }
}
