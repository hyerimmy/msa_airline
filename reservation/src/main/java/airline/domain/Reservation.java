package airline.domain;

import airline.ReservationApplication;
import airline.domain.ReservationCanceled;
import airline.domain.ReservationPlaced;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Reservation_table")
@Data
//<<< DDD / Aggregate Root
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long customerId;

    private Long flightId;

    private Long seatQty;

    private Date reserveDate;

    private String status;

    @PostPersist
    public void onPostPersist() {
        ReservationPlaced reservationPlaced = new ReservationPlaced(this);
        reservationPlaced.publishAfterCommit();

        ReservationCanceled reservationCanceled = new ReservationCanceled(this);
        reservationCanceled.publishAfterCommit();
    }

    public static ReservationRepository repository() {
        ReservationRepository reservationRepository = ReservationApplication.applicationContext.getBean(
            ReservationRepository.class
        );
        return reservationRepository;
    }

    //<<< Clean Arch / Port Method
    public static void updateStatus(SeatsSoldOut seatsSoldOut) {
        repository().findById(seatsSoldOut.getReservationId()).ifPresent(reservation->{
            reservation.setStatus("CANCELED");
            repository().save(reservation);
        });
    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
