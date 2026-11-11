package vn.datve.dat_ve.booking.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
        @NotEmpty(message = "Danh sách ghế không được để trống")
        @Size(max = 10, message = "Mỗi lần đặt tối đa 10 ghế")
        List<@NotNull Long> seatIds
) {
}
