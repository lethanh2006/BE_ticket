package vn.datve.dat_ve.booking;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.datve.dat_ve.booking.dto.BookingResponse;
import vn.datve.dat_ve.booking.dto.CreateBookingRequest;
import vn.datve.dat_ve.common.exception.ApiException;
import vn.datve.dat_ve.seat.Seat;
import vn.datve.dat_ve.seat.SeatRepository;
import vn.datve.dat_ve.seat.SeatsChangedEvent;
import vn.datve.dat_ve.showtime.Showtime;
import vn.datve.dat_ve.showtime.ShowtimeRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingQueryService bookingQueryService;
    private final ApplicationEventPublisher events;
    private final Duration holdDuration;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            SeatRepository seatRepository,
            ShowtimeRepository showtimeRepository,
            BookingQueryService bookingQueryService,
            ApplicationEventPublisher events,
            @Value("${app.booking.hold-duration}") Duration holdDuration
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
        this.bookingQueryService = bookingQueryService;
        this.events = events;
        this.holdDuration = holdDuration;
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

        // 2. Kiểm tra showtime tồn tại và chưa bắt đầu
        Showtime showtime = showtimeRepository.findById(showtimeId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "SHOWTIME_NOT_FOUND",
                "Không tìm thấy suất diễn có id " + showtimeId
        ));
        Instant now = Instant.now();
        if (!showtime.getStartTime().isAfter(now)) {
            throw new ApiException(HttpStatus.CONFLICT, "SHOWTIME_STARTED", "Suất diễn đã bắt đầu, không thể đặt vé");
        }

        // 3. Lock những ghế user muốn giữ (theo thứ tự id để hai request không khoá chéo nhau)
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

        // 7. Tính tổng tiền từ database (không tin giá client gửi lên)
        BigDecimal totalAmount = seats.stream()
                .map(Seat::getFaceValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 8. Giữ ghế trong thời gian cấu hình (mặc định 5 phút) chờ thanh toán
        Instant expiresAt = now.plus(holdDuration);

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
        // (unique index một-giữ-chỗ-mỗi-ghế trong DB là chốt chặn cuối nếu code có sai sót)
        List<BookingSeat> bookingSeats = new ArrayList<>();

        for (Seat seat : seats) {
            bookingSeats.add(new BookingSeat(
                    booking.getId(),
                    showtimeId,
                    seat.getId(),
                    BookingSeatStatus.HELD,
                    seat.getFaceValue()
            ));
        }

        bookingSeatRepository.saveAll(bookingSeats);
        bookingSeatRepository.flush();

        // 11. Báo cho các trình duyệt đang xem sơ đồ ghế (gửi sau khi commit)
        events.publishEvent(new SeatsChangedEvent(showtimeId));

        return bookingQueryService.getForUser(booking.getId(), userId, false);
    }

    /** Huỷ đơn đang chờ thanh toán và nhả ghế ngay cho người khác. */
    @Transactional
    public BookingResponse cancel(long userId, long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .filter(b -> b.isOwnedBy(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Không tìm thấy đơn đặt vé"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_CANCELLABLE",
                    "Chỉ huỷ được đơn đang chờ thanh toán");
        }
        booking.cancel();
        bookingSeatRepository.findByBookingIdAndReservationStatus(bookingId, BookingSeatStatus.HELD)
                .forEach(BookingSeat::release);
        bookingSeatRepository.flush();
        events.publishEvent(new SeatsChangedEvent(booking.getShowtimeId()));
        return bookingQueryService.getForUser(bookingId, userId, false);
    }
}
