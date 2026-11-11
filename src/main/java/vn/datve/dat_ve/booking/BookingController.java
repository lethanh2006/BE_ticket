package vn.datve.dat_ve.booking;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.datve.dat_ve.booking.dto.BookingResponse;
import vn.datve.dat_ve.booking.dto.CreateBookingRequest;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/api/showtimes/{showtimeId}/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @PathVariable Long showtimeId,
            @Valid @RequestBody CreateBookingRequest request
    ) {

        Long userId = 1L; // tạm thời, chưa làm JWT

        BookingResponse response =
                bookingService.createBooking(
                        userId,
                        showtimeId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}