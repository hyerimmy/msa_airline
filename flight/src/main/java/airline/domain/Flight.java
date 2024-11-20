package airline.domain;

import airline.FlightApplication;
import airline.domain.RemainingSeatsDecreased;
import airline.domain.RemainingSeatsIncreased;
import airline.domain.SeatsSoldOut;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Flight_table")
@Data
//<<< DDD / Aggregate Root
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long remainingSeatsCount;

    private String flightCode;

    private Date takeoffDate;

    private Long cost;

    @PostPersist
    public void onPostPersist() {
        RemainingSeatsDecreased remainingSeatsDecreased = new RemainingSeatsDecreased(
            this
        );
        remainingSeatsDecreased.publishAfterCommit();

        RemainingSeatsIncreased remainingSeatsIncreased = new RemainingSeatsIncreased(
            this
        );
        remainingSeatsIncreased.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate() {
        SeatsSoldOut seatsSoldOut = new SeatsSoldOut(this);
        seatsSoldOut.publishAfterCommit();
    }

    public static FlightRepository repository() {
        FlightRepository flightRepository = FlightApplication.applicationContext.getBean(
            FlightRepository.class
        );
        return flightRepository;
    }

    //<<< Clean Arch / Port Method
    public static void decreaseRemainingSeats(
        ReservationPlaced reservationPlaced
    ) {
        //implement business logic here:

        /** Example 1:  new item 
        Flight flight = new Flight();
        repository().save(flight);

        RemainingSeatsDecreased remainingSeatsDecreased = new RemainingSeatsDecreased(flight);
        remainingSeatsDecreased.publishAfterCommit();
        SeatsSoldOut seatsSoldOut = new SeatsSoldOut(flight);
        seatsSoldOut.publishAfterCommit();
        */

        /** Example 2:  finding and process
        
        repository().findById(reservationPlaced.get???()).ifPresent(flight->{
            
            flight // do something
            repository().save(flight);

            RemainingSeatsDecreased remainingSeatsDecreased = new RemainingSeatsDecreased(flight);
            remainingSeatsDecreased.publishAfterCommit();
            SeatsSoldOut seatsSoldOut = new SeatsSoldOut(flight);
            seatsSoldOut.publishAfterCommit();

         });
        */

    }

    //>>> Clean Arch / Port Method
    //<<< Clean Arch / Port Method
    public static void increaseRemainingSeats(
        ReservationCanceled reservationCanceled
    ) {
        //implement business logic here:

        /** Example 1:  new item 
        Flight flight = new Flight();
        repository().save(flight);

        RemainingSeatsIncreased remainingSeatsIncreased = new RemainingSeatsIncreased(flight);
        remainingSeatsIncreased.publishAfterCommit();
        */

        /** Example 2:  finding and process
        
        repository().findById(reservationCanceled.get???()).ifPresent(flight->{
            
            flight // do something
            repository().save(flight);

            RemainingSeatsIncreased remainingSeatsIncreased = new RemainingSeatsIncreased(flight);
            remainingSeatsIncreased.publishAfterCommit();

         });
        */

    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
