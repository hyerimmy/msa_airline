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
    PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {}

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='RemainingSeatsDecreased'"
    )
    public void wheneverRemainingSeatsDecreased_RequestPayment(
        @Payload RemainingSeatsDecreased remainingSeatsDecreased
    ) {
        RemainingSeatsDecreased event = remainingSeatsDecreased;
        System.out.println(
            "\n\n##### listener RequestPayment : " +
            remainingSeatsDecreased +
            "\n\n"
        );

        // Sample Logic //
        Payment.requestPayment(event);
    }
}
//>>> Clean Arch / Inbound Adaptor
