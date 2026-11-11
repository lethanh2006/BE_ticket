package vn.datve.dat_ve.admin;

import java.time.Instant;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.datve.dat_ve.admin.dto.AddSeatsRequest;
import vn.datve.dat_ve.admin.dto.CheckInRequest;
import vn.datve.dat_ve.admin.dto.CheckInResponse;
import vn.datve.dat_ve.admin.dto.StatsResponse;
import vn.datve.dat_ve.booking.Booking;
import vn.datve.dat_ve.booking.BookingQueryService;
import vn.datve.dat_ve.booking.BookingRepository;
import vn.datve.dat_ve.booking.BookingStatus;
import vn.datve.dat_ve.booking.dto.BookingResponse;
import vn.datve.dat_ve.common.exception.ApiException;
import vn.datve.dat_ve.common.response.PageResponse;
import vn.datve.dat_ve.event.dto.EventInput;
import vn.datve.dat_ve.event.dto.EventSummary;
import vn.datve.dat_ve.showtime.dto.ShowtimeInput;
import vn.datve.dat_ve.showtime.dto.ShowtimeResponse;

/** Mọi đường dẫn /api/admin/** chỉ dành cho ROLE_ADMIN (cấu hình trong SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminStatsService statsService;
    private final AdminCatalogService catalogService;
    private final BookingQueryService bookingQueryService;
    private final BookingRepository bookingRepository;

    public AdminController(AdminStatsService statsService, AdminCatalogService catalogService,
            BookingQueryService bookingQueryService, BookingRepository bookingRepository) {
        this.statsService = statsService;
        this.catalogService = catalogService;
        this.bookingQueryService = bookingQueryService;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return statsService.stats();
    }

    @GetMapping("/bookings")
    public PageResponse<BookingResponse> bookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookingQueryService.pageForAdmin(status, q, page, size);
    }

    // soát vé tại cửa: khoá đơn để hai máy quét cùng lúc không cùng báo "vào cửa lần đầu"
    @PostMapping("/bookings/check-in")
    @Transactional
    public CheckInResponse checkIn(@Valid @RequestBody CheckInRequest request) {
        Booking booking = bookingRepository.findByTicketCodeForUpdate(request.ticketCode().strip().toLowerCase())
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", "Mã vé không hợp lệ"));
        String result = "ALREADY_CHECKED_IN";
        if (booking.getCheckedInAt() == null) {
            booking.checkIn(Instant.now());
            bookingRepository.flush();
            result = "CHECKED_IN";
        }
        return new CheckInResponse(result, bookingQueryService.getForUser(booking.getId(), 0, true));
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventSummary createEvent(@Valid @RequestBody EventInput input) {
        return catalogService.createEvent(input);
    }

    @PutMapping("/events/{id}")
    public EventSummary updateEvent(@PathVariable Long id, @Valid @RequestBody EventInput input) {
        return catalogService.updateEvent(id, input);
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        catalogService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/events/{eventId}/showtimes")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowtimeResponse createShowtime(@PathVariable Long eventId, @Valid @RequestBody ShowtimeInput input) {
        return catalogService.createShowtime(eventId, input);
    }

    @PostMapping("/showtimes/{id}/seats")
    public ShowtimeResponse addSeats(@PathVariable Long id, @Valid @RequestBody AddSeatsRequest request) {
        return catalogService.addSeats(id, request.sections());
    }

    @DeleteMapping("/showtimes/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable Long id) {
        catalogService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }
}
