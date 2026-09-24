package vn.datve.dat_ve.showtime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.datve.dat_ve.event.EventRepository;
import vn.datve.dat_ve.showtime.dto.ShowtimeResponse;

import java.util.List;

@Service
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final EventRepository eventRepository;

    public ShowtimeService(ShowtimeRepository showtimeRepository,
                           EventRepository eventRepository) {
        this.showtimeRepository = showtimeRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> findByEventId(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện có id " + eventId);
        }
        return showtimeRepository.findByEventIdOrderByStartTimeAsc(eventId)
                .stream()
                .map(ShowtimeResponse::from)
                .toList();
    }
}