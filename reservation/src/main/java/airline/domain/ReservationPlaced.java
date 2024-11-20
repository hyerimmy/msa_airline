package airline.domain;

import airline.domain.*;
import airline.infra.AbstractEvent;
import java.time.LocalDate;
import java.util.*;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class ReservationPlaced extends AbstractEvent {

    private Long id;
    private Long customerId;
    private Long flightId;
    private Long seatQty;
    private Date reserveDate;
    private String status;

    public ReservationPlaced(Reservation aggregate) {
        super(aggregate);
    }

    public ReservationPlaced() {
        super();
    }
}
//>>> DDD / Domain Event
