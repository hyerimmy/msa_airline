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
    ReservationRepository reservationRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {}

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='SeatsSoldOut'"
    )
    public void wheneverSeatsSoldOut_UpdateStatus(
        @Payload SeatsSoldOut seatsSoldOut
    ) {
        SeatsSoldOut event = seatsSoldOut;
        System.out.println(
            "\n\n##### listener UpdateStatus : " + seatsSoldOut + "\n\n"
        );

        // Sample Logic //
        Reservation.updateStatus(event);
    }
}
//>>> Clean Arch / Inbound Adaptor
