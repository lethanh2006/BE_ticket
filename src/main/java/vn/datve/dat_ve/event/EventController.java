package vn.datve.dat_ve.event;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.datve.dat_ve.common.response.PageResponse;
import vn.datve.dat_ve.event.dto.EventSummary;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventQueryService eventQueryService;

    public EventController(EventQueryService eventQueryService) {
        this.eventQueryService = eventQueryService;
    }

    // danh sách sự kiện: tìm kiếm không dấu, lọc thể loại, phân trang (page bắt đầu từ 0)
    @GetMapping
    public PageResponse<EventSummary> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return eventQueryService.search(q, category, page, size);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return eventQueryService.categories();
    }

    // chi tiết 1 sự kiện
    @GetMapping("/{id}")
    public EventSummary get(@PathVariable Long id) {
        return eventQueryService.get(id);
    }
}
