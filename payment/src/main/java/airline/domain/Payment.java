package airline.domain;

import airline.PaymentApplication;
import airline.domain.Paid;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Payment_table")
@Data
//<<< DDD / Aggregate Root
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long customerId;

    private Long cost;

    @PostPersist
    public void onPostPersist() {
        Paid paid = new Paid(this);
        paid.publishAfterCommit();
    }

    public static PaymentRepository repository() {
        PaymentRepository paymentRepository = PaymentApplication.applicationContext.getBean(
            PaymentRepository.class
        );
        return paymentRepository;
    }

    //<<< Clean Arch / Port Method
    public static void requestPayment(
        RemainingSeatsDecreased remainingSeatsDecreased
    ) {
        //implement business logic here:

        /** Example 1:  new item 
        Payment payment = new Payment();
        repository().save(payment);

        Paid paid = new Paid(payment);
        paid.publishAfterCommit();
        */

        /** Example 2:  finding and process
        
        repository().findById(remainingSeatsDecreased.get???()).ifPresent(payment->{
            
            payment // do something
            repository().save(payment);

            Paid paid = new Paid(payment);
            paid.publishAfterCommit();

         });
        */

    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
