package vn.datve.dat_ve.showtime;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import vn.datve.dat_ve.showtime.dto.ShowtimeResponse;

@RestController
public class ShowtimeController {

    private final ShowtimeQueryService showtimeQueryService;

    public ShowtimeController(ShowtimeQueryService showtimeQueryService) {
        this.showtimeQueryService = showtimeQueryService;
    }

    // danh sách suất diễn của 1 sự kiện
    @GetMapping("/api/events/{eventId}/showtimes")
    public List<ShowtimeResponse> listByEvent(@PathVariable Long eventId) {
        return showtimeQueryService.findByEventId(eventId);
    }

    @GetMapping("/api/showtimes/{id}")
    public ShowtimeResponse get(@PathVariable Long id) {
        return showtimeQueryService.get(id);
    }
}
