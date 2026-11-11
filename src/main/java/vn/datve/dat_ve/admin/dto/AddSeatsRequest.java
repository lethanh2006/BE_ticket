package vn.datve.dat_ve.admin.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import vn.datve.dat_ve.seat.dto.SeatSectionInput;

public record AddSeatsRequest(
        @NotEmpty(message = "Vui lòng nhập ít nhất một khu vực ghế")
        @Size(max = 10, message = "Tối đa 10 khu vực ghế")
        List<@Valid SeatSectionInput> sections
) {
}
