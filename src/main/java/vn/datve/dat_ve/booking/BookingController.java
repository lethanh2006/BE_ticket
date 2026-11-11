package vn.datve.dat_ve.booking;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.datve.dat_ve.auth.CurrentUser;
import vn.datve.dat_ve.booking.dto.BookingResponse;
import vn.datve.dat_ve.booking.dto.CreateBookingRequest;
import vn.datve.dat_ve.common.response.PageResponse;

@RestController
public class BookingController {

    private final BookingService bookingService;
    private final BookingQueryService bookingQueryService;

    public BookingController(BookingService bookingService, BookingQueryService bookingQueryService) {
        this.bookingService = bookingService;
        this.bookingQueryService = bookingQueryService;
    }

    // giữ ghế cho người đang đăng nhập (userId lấy từ JWT, không nhận từ client)
    @PostMapping("/api/showtimes/{showtimeId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long showtimeId,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return bookingService.createBooking(CurrentUser.id(jwt), showtimeId, request);
    }

    @GetMapping("/api/bookings")
    public PageResponse<BookingResponse> myBookings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return bookingQueryService.pageForUser(CurrentUser.id(jwt), status, page, size);
    }

    @GetMapping("/api/bookings/{id}")
    public BookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingQueryService.getForUser(id, CurrentUser.id(jwt), CurrentUser.isAdmin(jwt));
    }

    @PostMapping("/api/bookings/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.cancel(CurrentUser.id(jwt), id);
    }
}
