package vn.datve.dat_ve.event;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.datve.dat_ve.common.response.PageResponse;
import vn.datve.dat_ve.event.dto.EventResponse;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> findAll(Pageable pageable) {
        return PageResponse.from(
                eventRepository.findAll(pageable).map(EventResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public EventResponse findById(Long id) {
        return eventRepository.findById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện có id " + id));
    }
}