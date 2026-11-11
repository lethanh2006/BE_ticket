package vn.datve.dat_ve.admin;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.datve.dat_ve.common.exception.ApiException;
import vn.datve.dat_ve.event.Event;
import vn.datve.dat_ve.event.EventQueryService;
import vn.datve.dat_ve.event.EventRepository;
import vn.datve.dat_ve.event.dto.EventInput;
import vn.datve.dat_ve.event.dto.EventSummary;
import vn.datve.dat_ve.seat.SeatGenerator;
import vn.datve.dat_ve.seat.SeatsChangedEvent;
import vn.datve.dat_ve.seat.dto.SeatSectionInput;
import vn.datve.dat_ve.showtime.Showtime;
import vn.datve.dat_ve.showtime.ShowtimeQueryService;
import vn.datve.dat_ve.showtime.ShowtimeRepository;
import vn.datve.dat_ve.showtime.dto.ShowtimeInput;
import vn.datve.dat_ve.showtime.dto.ShowtimeResponse;

/** Quản lý danh mục: sự kiện, suất diễn và ghế. Không xoá được thứ đã có người đặt. */
@Service
public class AdminCatalogService {

    private final EventRepository eventRepository;
    private final ShowtimeRepository showtimeRepository;
    private final EventQueryService eventQueryService;
    private final ShowtimeQueryService showtimeQueryService;
    private final SeatGenerator seatGenerator;
    private final JdbcClient jdbc;
    private final ApplicationEventPublisher events;

    public AdminCatalogService(EventRepository eventRepository, ShowtimeRepository showtimeRepository,
            EventQueryService eventQueryService, ShowtimeQueryService showtimeQueryService,
            SeatGenerator seatGenerator, JdbcClient jdbc, ApplicationEventPublisher events) {
        this.eventRepository = eventRepository;
        this.showtimeRepository = showtimeRepository;
        this.eventQueryService = eventQueryService;
        this.showtimeQueryService = showtimeQueryService;
        this.seatGenerator = seatGenerator;
        this.jdbc = jdbc;
        this.events = events;
    }

    @Transactional
    public EventSummary createEvent(EventInput input) {
        Event event = eventRepository.save(new Event(input.name().strip(), blankToNull(input.description()),
                input.venue().strip(), blankToNull(input.imageUrl()), blankToNull(input.category())));
        return eventQueryService.get(event.getId());
    }

    @Transactional
    public EventSummary updateEvent(long id, EventInput input) {
        Event event = eventRepository.findById(id).orElseThrow(() -> eventNotFound(id));
        event.update(input.name().strip(), blankToNull(input.description()), input.venue().strip(),
                blankToNull(input.imageUrl()), blankToNull(input.category()));
        eventRepository.flush();
        return eventQueryService.get(id);
    }

    @Transactional
    public void deleteEvent(long id) {
        if (!eventRepository.existsById(id)) {
            throw eventNotFound(id);
        }
        boolean hasBookings = jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM bookings b JOIN showtimes st ON st.id = b.showtime_id
                                       WHERE st.event_id = :id)
                        """)
                .param("id", id).query(Boolean.class).single();
        if (hasBookings) {
            throw new ApiException(HttpStatus.CONFLICT, "EVENT_HAS_BOOKINGS", "Sự kiện đã có người đặt vé, không thể xoá");
        }
        jdbc.sql("DELETE FROM seats WHERE showtime_id IN (SELECT id FROM showtimes WHERE event_id = :id)")
                .param("id", id).update();
        jdbc.sql("DELETE FROM showtimes WHERE event_id = :id").param("id", id).update();
        jdbc.sql("DELETE FROM events WHERE id = :id").param("id", id).update();
    }

    @Transactional
    public ShowtimeResponse createShowtime(long eventId, ShowtimeInput input) {
        if (!eventRepository.existsById(eventId)) {
            throw eventNotFound(eventId);
        }
        if (!input.endTime().isAfter(input.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE", "Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (!input.startTime().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SHOWTIME_IN_PAST", "Không tạo được suất diễn trong quá khứ");
        }
        Showtime showtime;
        try {
            showtime = showtimeRepository.saveAndFlush(new Showtime(eventId, input.startTime(), input.endTime()));
        } catch (DataIntegrityViolationException exception) {
            // ràng buộc EXCLUDE showtimes_no_overlap trong database
            throw new ApiException(HttpStatus.CONFLICT, "SHOWTIME_OVERLAP",
                    "Sự kiện đã có suất diễn trùng khung giờ này");
        }
        if (input.sections() != null && !input.sections().isEmpty()) {
            seatGenerator.addSections(showtime.getId(), input.sections());
        }
        return showtimeQueryService.get(showtime.getId());
    }

    @Transactional
    public ShowtimeResponse addSeats(long showtimeId, List<SeatSectionInput> sections) {
        showtimeQueryService.get(showtimeId); // 404 nếu không có suất diễn
        seatGenerator.addSections(showtimeId, sections);
        events.publishEvent(new SeatsChangedEvent(showtimeId));
        return showtimeQueryService.get(showtimeId);
    }

    @Transactional
    public void deleteShowtime(long showtimeId) {
        showtimeQueryService.get(showtimeId);
        boolean hasBookings = jdbc.sql("SELECT EXISTS (SELECT 1 FROM bookings WHERE showtime_id = :id)")
                .param("id", showtimeId).query(Boolean.class).single();
        if (hasBookings) {
            throw new ApiException(HttpStatus.CONFLICT, "SHOWTIME_HAS_BOOKINGS", "Suất diễn đã có người đặt vé, không thể xoá");
        }
        jdbc.sql("DELETE FROM seats WHERE showtime_id = :id").param("id", showtimeId).update();
        jdbc.sql("DELETE FROM showtimes WHERE id = :id").param("id", showtimeId).update();
    }

    private static ApiException eventNotFound(long id) {
        return new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Không tìm thấy sự kiện có id " + id);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
