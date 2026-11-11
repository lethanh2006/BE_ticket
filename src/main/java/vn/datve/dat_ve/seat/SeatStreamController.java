package vn.datve.dat_ve.seat;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.datve.dat_ve.showtime.ShowtimeQueryService;

@RestController
public class SeatStreamController {

    private final SeatStreamService seatStreamService;
    private final ShowtimeQueryService showtimeQueryService;

    public SeatStreamController(SeatStreamService seatStreamService, ShowtimeQueryService showtimeQueryService) {
        this.seatStreamService = seatStreamService;
        this.showtimeQueryService = showtimeQueryService;
    }

    // luồng sự kiện "seats": báo trình duyệt tải lại sơ đồ ghế khi có người giữ/mua/nhả ghế
    @GetMapping(value = "/api/showtimes/{showtimeId}/seats/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long showtimeId, HttpServletResponse response) {
        showtimeQueryService.get(showtimeId); // 404 nếu suất diễn không tồn tại
        response.setHeader("X-Accel-Buffering", "no"); // không để reverse proxy gom dữ liệu
        return seatStreamService.subscribe(showtimeId);
    }
}
