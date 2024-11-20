package airline.domain;

import airline.infra.AbstractEvent;
import java.time.LocalDate;
import java.util.*;
import lombok.Data;

@Data
public class ReservationCanceled extends AbstractEvent {

    private Long id;
    private Long customerId;
    private Long flightId;
    private Long seatQty;
    private Date reserveDate;
    private String status;
}
