package vn.datve.dat_ve.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EventInput(
        @NotBlank(message = "Vui lòng nhập tên sự kiện")
        @Size(max = 255, message = "Tên sự kiện tối đa 255 ký tự")
        String name,

        @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
        String description,

        @NotBlank(message = "Vui lòng nhập địa điểm")
        @Size(max = 255, message = "Địa điểm tối đa 255 ký tự")
        String venue,

        @Size(max = 2000, message = "Đường dẫn ảnh quá dài")
        @Pattern(regexp = "^$|https?://.+", message = "Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://")
        String imageUrl,

        @Size(max = 50, message = "Thể loại tối đa 50 ký tự")
        String category
) {
}
