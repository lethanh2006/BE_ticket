package vn.datve.dat_ve.seat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "seat_code", nullable = false)
    private String seatCode;

    @Column(nullable = false)
    private String category;

    @Column(name = "face_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal faceValue;

    protected Seat() {
    }

    public Long getId() { return id; }
    public Long getShowtimeId() { return showtimeId; }
    public String getSeatCode() { return seatCode; }
    public String getCategory() { return category; }
    public BigDecimal getFaceValue() { return faceValue; }
}