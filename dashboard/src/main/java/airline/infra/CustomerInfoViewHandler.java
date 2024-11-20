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
@Slf4j
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
            
            // view 객체 조회
            Optional<CustomerInfo> customerInfoOptional = customerInfoRepository.findById(
                reservationPlaced.getCustomerId()
            );

            // 기존 customerId의 대시보드 데이터가 존재하지 않는다면 생성, 존재한다면 수정
            if (customerInfoOptional.isPresent()) {  // 수정
                CustomerInfo customerInfo = customerInfoOptional.get();
                customerInfo.setFlightCount(customerInfo.getFlightCount()+1L);
                customerInfo.setRecentReserveDate(reservationPlaced.getReserveDate());
                customerInfoRepository.save(customerInfo);
            } else { // 생성
                CustomerInfo customerInfo = new CustomerInfo();
                customerInfo.setCustomerId(reservationPlaced.getCustomerId());
                customerInfo.setFlightCount(1L);
                customerInfo.setRecentReserveDate(reservationPlaced.getReserveDate());
                customerInfoRepository.save(customerInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //>>> DDD / CQRS
}
