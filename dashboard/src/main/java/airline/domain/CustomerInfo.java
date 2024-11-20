package airline.domain;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import lombok.Data;

//<<< EDA / CQRS
@Entity
@Table(name = "CustomerInfo_table")
@Data
public class CustomerInfo {

    @Id
    //@GeneratedValue(strategy=GenerationType.AUTO)
    private Long customerId;

    private Long flightCount;
    private Long recentReserveDate;
}
