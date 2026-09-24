package vn.datve.dat_ve.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.datve.dat_ve.common.response.PageResponse;
import vn.datve.dat_ve.event.dto.EventResponse;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public PageResponse<EventResponse> list(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return eventService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.findById(id);
    }
}