package vn.datve.dat_ve.booking;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.datve.dat_ve.booking.dto.BookingResponse;
import vn.datve.dat_ve.booking.dto.BookingSeatResponse;
import vn.datve.dat_ve.booking.dto.CreateBookingRequest;
import vn.datve.dat_ve.common.exception.ApiException;
import vn.datve.dat_ve.seat.Seat;
import vn.datve.dat_ve.seat.SeatRepository;
import vn.datve.dat_ve.showtime.ShowtimeRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            SeatRepository seatRepository,
            ShowtimeRepository showtimeRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Transactional
    public BookingResponse createBooking(
            Long userId,
            Long showtimeId,
            CreateBookingRequest request
    ) {

        List<Long> seatIds = request.seatIds();

        // 1. Kiểm tra seat ID bị trùng
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DUPLICATE_SEAT_ID",
                    "Danh sách ghế không được chứa seat ID trùng nhau"
            );
        }

        // 2. Kiểm tra showtime tồn tại
        if (!showtimeRepository.existsById(showtimeId)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "SHOWTIME_NOT_FOUND",
                    "Không tìm thấy suất diễn có id " + showtimeId
            );
        }

        // 3. Lock những ghế user muốn giữ
        List<Seat> seats =
                seatRepository.findSeatsForUpdate(showtimeId, seatIds);

        // 4. Phải tìm thấy đầy đủ tất cả ghế
        if (seats.size() != seatIds.size()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "SEAT_NOT_FOUND",
                    "Một hoặc nhiều ghế không tồn tại trong suất diễn này"
            );
        }

        Instant now = Instant.now();

        // 5. Tìm những booking hết hạn đang giữ các ghế này
        List<Long> expiredBookingIds =
                bookingRepository.findExpiredPendingBookingIdsTouchingSeats(
                        seatIds,
                        now
                );

        if (!expiredBookingIds.isEmpty()) {

            List<Booking> expiredBookings =
                    bookingRepository.findAllById(expiredBookingIds);

            for (Booking booking : expiredBookings) {
                booking.expire();
            }

            List<BookingSeat> expiredSeats =
                    bookingSeatRepository
                            .findByBookingIdInAndReservationStatus(
                                    expiredBookingIds,
                                    BookingSeatStatus.HELD
                            );

            for (BookingSeat bookingSeat : expiredSeats) {
                bookingSeat.release();
            }

            bookingRepository.flush();
            bookingSeatRepository.flush();
        }

        // 6. Kiểm tra có ghế đang HELD hoặc BOOKED không
        boolean unavailable =
                bookingSeatRepository
                        .existsBySeatIdInAndReservationStatusIn(
                                seatIds,
                                List.of(
                                        BookingSeatStatus.HELD,
                                        BookingSeatStatus.BOOKED
                                )
                        );

        if (unavailable) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SEAT_UNAVAILABLE",
                    "Một hoặc nhiều ghế không còn trống"
            );
        }

        // 7. Tính tổng tiền từ database
        BigDecimal totalAmount = seats.stream()
                .map(Seat::getFaceValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 8. Giữ ghế 5 phút
        Instant expiresAt =
                now.plus(5, ChronoUnit.MINUTES);

        // 9. Tạo booking
        Booking booking = new Booking(
                userId,
                showtimeId,
                BookingStatus.PENDING,
                totalAmount,
                expiresAt
        );

        bookingRepository.save(booking);

        // 10. Tạo các booking_seats HELD
        List<BookingSeat> bookingSeats = new ArrayList<>();

        for (Seat seat : seats) {
            BookingSeat bookingSeat = new BookingSeat(
                    booking.getId(),
                    showtimeId,
                    seat.getId(),
                    BookingSeatStatus.HELD,
                    seat.getFaceValue()
            );

            bookingSeats.add(bookingSeat);
        }

        bookingSeatRepository.saveAll(bookingSeats);

        // 11. Tạo response
        List<BookingSeatResponse> seatResponses =
                seats.stream()
                        .map(seat -> new BookingSeatResponse(
                                seat.getId(),
                                seat.getSeatCode(),
                                seat.getFaceValue()
                        ))
                        .toList();

        return new BookingResponse(
                booking.getId(),
                showtimeId,
                BookingStatus.PENDING,
                seatResponses,
                totalAmount,
                expiresAt
        );
    }
}