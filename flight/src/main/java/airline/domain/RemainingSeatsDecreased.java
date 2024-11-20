package airline.domain;

import airline.domain.*;
import airline.infra.AbstractEvent;
import java.time.LocalDate;
import java.util.*;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class RemainingSeatsDecreased extends AbstractEvent {

    private Long id;
    private Long remainingSeatsCount;
    private String flightCode;
    private Date takeoffDate;
    private Long cost;

    public RemainingSeatsDecreased(Flight aggregate) {
        super(aggregate);
    }

    public RemainingSeatsDecreased() {
        super();
    }
}
//>>> DDD / Domain Event
