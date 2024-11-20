package airline.infra;

import airline.config.kafka.KafkaProcessor;
import airline.domain.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.naming.NameParser;
import javax.naming.NameParser;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

//<<< Clean Arch / Inbound Adaptor
@Service
@Transactional
public class PolicyHandler {

    @Autowired
    FlightRepository flightRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {}

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='ReservationPlaced'"
    )
    public void wheneverReservationPlaced_DecreaseRemainingSeats(
        @Payload ReservationPlaced reservationPlaced
    ) {
        ReservationPlaced event = reservationPlaced;
        System.out.println(
            "\n\n##### listener DecreaseRemainingSeats : " +
            reservationPlaced +
            "\n\n"
        );

        // Sample Logic //
        Flight.decreaseRemainingSeats(event);
    }

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='ReservationCanceled'"
    )
    public void wheneverReservationCanceled_IncreaseRemainingSeats(
        @Payload ReservationCanceled reservationCanceled
    ) {
        ReservationCanceled event = reservationCanceled;
        System.out.println(
            "\n\n##### listener IncreaseRemainingSeats : " +
            reservationCanceled +
            "\n\n"
        );

        // Sample Logic //
        Flight.increaseRemainingSeats(event);
    }
}
//>>> Clean Arch / Inbound Adaptor
