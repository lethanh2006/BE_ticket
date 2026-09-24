package vn.datve.dat_ve.showtime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import vn.datve.dat_ve.showtime.dto.ShowtimeResponse;

import java.util.List;

@RestController
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping("/api/events/{eventId}/showtimes")
    public List<ShowtimeResponse> listByEvent(@PathVariable Long eventId) {
        return showtimeService.findByEventId(eventId);
    }
}