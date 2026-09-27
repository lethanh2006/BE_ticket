package vn.datve.dat_ve.booking.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateBookingRequest(

        @NotEmpty(message = "Danh sách ghế không được để trống")
        List<Long> seatIds

) {
}