package vn.datve.dat_ve.seat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.datve.dat_ve.seat.dto.SeatResponse;
import vn.datve.dat_ve.showtime.ShowtimeRepository;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    public SeatService(SeatRepository seatRepository,
                       ShowtimeRepository showtimeRepository) {
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> findByShowtimeId(Long showtimeId) {
        if (!showtimeRepository.existsById(showtimeId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Không tìm thấy suất diễn có id " + showtimeId);
        }
        return seatRepository.findSeatsWithStatus(showtimeId)
                .stream()
                .map(SeatResponse::from)
                .toList();
    }
}