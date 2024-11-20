package airline.domain;

import airline.domain.*;
import airline.infra.AbstractEvent;
import java.time.LocalDate;
import java.util.*;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class ReservationCanceled extends AbstractEvent {

    private Long id;
    private Long customerId;
    private Long flightId;
    private Long seatQty;
    private Date reserveDate;
    private String status;

    public ReservationCanceled(Reservation aggregate) {
        super(aggregate);
    }

    public ReservationCanceled() {
        super();
    }
}
//>>> DDD / Domain Event
