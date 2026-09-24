package vn.datve.dat_ve.seat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import vn.datve.dat_ve.seat.dto.SeatResponse;

import java.util.List;

@RestController
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping("/api/showtimes/{showtimeId}/seats")
    public List<SeatResponse> listSeats(@PathVariable Long showtimeId) {
        return seatService.findByShowtimeId(showtimeId);
    }
}