package vn.datve.dat_ve.showtime.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.datve.dat_ve.seat.dto.SeatSectionInput;

public record ShowtimeInput(
        @NotNull(message = "Vui lòng chọn thời gian bắt đầu") Instant startTime,
        @NotNull(message = "Vui lòng chọn thời gian kết thúc") Instant endTime,
        @Size(max = 10, message = "Tối đa 10 khu vực ghế") List<@Valid SeatSectionInput> sections
) {
}
