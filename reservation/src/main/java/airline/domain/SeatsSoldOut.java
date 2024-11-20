package airline.domain;

import airline.domain.*;
import airline.infra.AbstractEvent;
import java.util.*;
import lombok.*;

@Data
@ToString
public class SeatsSoldOut extends AbstractEvent {

    private Long id;
    private Long remainingSeatsCount;
    private String flightCode;
    private Date takeoffDate;
    private Long cost;
    private Long reservationId;
}
