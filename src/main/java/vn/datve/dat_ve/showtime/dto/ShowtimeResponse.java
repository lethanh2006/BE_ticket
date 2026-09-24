package vn.datve.dat_ve.showtime.dto;

import vn.datve.dat_ve.showtime.Showtime;

import java.time.Instant;

public record ShowtimeResponse(
        Long id,
        Long eventId,
        Instant startTime,
        Instant endTime
) {
    public static ShowtimeResponse from(Showtime showtime) {
        return new ShowtimeResponse(
                showtime.getId(),
                showtime.getEventId(),
                showtime.getStartTime(),
                showtime.getEndTime()
        );
    }
}