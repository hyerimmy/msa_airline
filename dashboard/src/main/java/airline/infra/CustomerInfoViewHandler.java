package airline.infra;

import airline.config.kafka.KafkaProcessor;
import airline.domain.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class CustomerInfoViewHandler {

    //<<< DDD / CQRS
    @Autowired
    private CustomerInfoRepository customerInfoRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenReservationPlaced_then_CREATE_1(
        @Payload ReservationPlaced reservationPlaced
    ) {
        try {
            if (!reservationPlaced.validate()) return;

            // view 객체 생성
            CustomerInfo customerInfo = new CustomerInfo();
            // view 객체에 이벤트의 Value 를 set 함
            customerInfo.setCustomerId(reservationPlaced.getCustomerId());
            customerInfo.setFlightCount(1);
            customerInfo.setRecentReserveDate(
                Long.valueOf(reservationPlaced.getReserveDate())
            );
            // view 레파지 토리에 save
            customerInfoRepository.save(customerInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenReservationPlaced_then_UPDATE_1(
        @Payload ReservationPlaced reservationPlaced
    ) {
        try {
            if (!reservationPlaced.validate()) return;
            // view 객체 조회
            Optional<CustomerInfo> customerInfoOptional = customerInfoRepository.findByCustomerId(
                reservationPlaced.getCustomerId()
            );

            if (customerInfoOptional.isPresent()) {
                CustomerInfo customerInfo = customerInfoOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                customerInfo.setFlightCount(customerInfo.getFlightCount() + 1);
                customerInfo.setRecentReserveDate(
                    Long.valueOf(reservationPlaced.getReserveDate())
                );
                // view 레파지 토리에 save
                customerInfoRepository.save(customerInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenReservationCanceled_then_UPDATE_2(
        @Payload ReservationCanceled reservationCanceled
    ) {
        try {
            if (!reservationCanceled.validate()) return;
            // view 객체 조회
            Optional<CustomerInfo> customerInfoOptional = customerInfoRepository.findByCustomerId(
                reservationCanceled.getCustomerId()
            );

            if (customerInfoOptional.isPresent()) {
                CustomerInfo customerInfo = customerInfoOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                customerInfo.setFlightCount(customerInfo.getFlightCount() - 1);
                // view 레파지 토리에 save
                customerInfoRepository.save(customerInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //>>> DDD / CQRS
}
