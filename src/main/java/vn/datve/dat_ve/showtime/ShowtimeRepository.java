package vn.datve.dat_ve.showtime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    List<Showtime> findByEventIdOrderByStartTimeAsc(Long eventId);
}