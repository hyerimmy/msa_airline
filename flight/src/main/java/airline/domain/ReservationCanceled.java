package airline.domain;

import airline.domain.*;
import airline.infra.AbstractEvent;
import java.util.*;
import lombok.*;

@Data
@ToString
public class ReservationCanceled extends AbstractEvent {

    private Long id;
    private Long customerId;
    private Long flightId;
    private Long seatQty;
    private Date reserveDate;
    private String status;
}
