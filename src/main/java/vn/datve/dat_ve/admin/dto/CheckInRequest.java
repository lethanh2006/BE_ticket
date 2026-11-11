package vn.datve.dat_ve.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckInRequest(
        @NotBlank(message = "Vui lòng nhập mã vé")
        @Size(max = 32, message = "Mã vé không hợp lệ")
        String ticketCode
) {
}
