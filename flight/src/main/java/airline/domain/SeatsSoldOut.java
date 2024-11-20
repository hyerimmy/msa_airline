package airline.domain;

import airline.domain.*;
import airline.infra.AbstractEvent;
import java.time.LocalDate;
import java.util.*;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class SeatsSoldOut extends AbstractEvent {

    private Long id;
    private Long remainingSeatsCount;
    private String flightCode;
    private Date takeoffDate;
    private Long cost;

    public SeatsSoldOut(Flight aggregate) {
        super(aggregate);
    }

    public SeatsSoldOut() {
        super();
    }
}
//>>> DDD / Domain Event
