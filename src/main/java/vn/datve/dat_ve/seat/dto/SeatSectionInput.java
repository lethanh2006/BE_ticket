package vn.datve.dat_ve.seat.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Một khu vực ghế: các hàng (A, B...) × số ghế mỗi hàng, cùng hạng và cùng giá. */
public record SeatSectionInput(
        @NotBlank(message = "Vui lòng nhập hạng ghế")
        @Size(max = 30, message = "Hạng ghế tối đa 30 ký tự")
        String category,

        @NotEmpty(message = "Vui lòng nhập ít nhất một hàng ghế")
        @Size(max = 26, message = "Tối đa 26 hàng mỗi khu vực")
        List<@Pattern(regexp = "[A-Z]{1,3}", message = "Tên hàng chỉ gồm 1-3 chữ in hoa") String> rows,

        @Min(value = 1, message = "Mỗi hàng có ít nhất 1 ghế")
        @Max(value = 50, message = "Mỗi hàng tối đa 50 ghế")
        int seatsPerRow,

        @NotNull(message = "Vui lòng nhập giá vé")
        @PositiveOrZero(message = "Giá vé không được âm")
        @Digits(integer = 10, fraction = 2, message = "Giá vé không hợp lệ")
        BigDecimal faceValue
) {
}
