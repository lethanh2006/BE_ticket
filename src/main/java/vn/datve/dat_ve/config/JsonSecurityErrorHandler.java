package vn.datve.dat_ve.config;

import java.io.IOException;
import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import vn.datve.dat_ve.common.exception.ApiErrorResponse;

/**
 * Lỗi xác thực/phân quyền trả về cùng dạng JSON với các lỗi khác.
 * INVALID_TOKEN: có gửi token nhưng token sai hoặc hết hạn (frontend sẽ đăng xuất);
 * UNAUTHENTICATED: chưa đăng nhập.
 */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper json;

    public JsonSecurityErrorHandler(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean sentToken = header != null && header.startsWith("Bearer ");
        if (sentToken) {
            write(response, 401, "INVALID_TOKEN", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn, vui lòng đăng nhập lại");
        } else {
            write(response, 401, "UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        write(response, 403, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này");
    }

    private void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), new ApiErrorResponse(code, message, Instant.now()));
    }
}
