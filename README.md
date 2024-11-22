# ✈️ Airline Service (항공사 예약 시스템)
> 사내 교육 'Cloud Native 개발자 코스' Final Project


## 👩🏻‍🎨 [IaaS] 아키텍처
### 1. 클라우드 아키텍처 구성
#### MSA 아키텍처 구성도
<img width="976" alt="image" src="https://github.com/user-attachments/assets/40f25dec-7de3-48ae-8af5-029c753d995d">


## 👷🏻 [Biz] 모델링
### 1. 이벤트 스토밍
#### 시나리오
#### 이벤트 스토밍 결과 
<img width="1247" alt="image" src="https://github.com/user-attachments/assets/fc1ebc5f-b798-47e8-8049-dfd711163fe7">

#### 시나리오 검증

## 🧑🏻‍💻 [Dev] MSA 개발
### 1. 분산 트랜잭션 `Saga` & 보상처리 `Compensation`
> 고객이 항공권을 예약하였지만 남은 좌석이 없어 SeatsSoldOut 이벤트가 Pub되었을 때, 이를 Sub하고 있는 UpdateStatus 이벤트를 발행하여 고객의 예약요청(reservationId) 상태값(status)을 'CANCELED'로 변경한다.

#### 시나리오
1. 비행기 잔여석보다 더 많은 예약을 발행하는 ReservationPlaced 이벤트가 발행된다. (비즈니스 예외 케이스)
2. flight 서비스에서 잔여석보다 더 많은 예약이 불가능함에 따라 SeatSoldOut 이벤트를 pub한다.
3. SeatSoldOut 이벤트를 sub하고 있는 reservation 서비스에서는 원예약(reservationId)의 상태값(status)을 수정한다.

#### 작업 내용

1. [Event Storming] “SeatSoldOut” Event 설정

- Long 타입의 reservationId를 추가한다.
- “Trigger By LifeCycle” 설정에서 Post Update로 변경한다.
- “SeatSoldOut” Event와 “update Status” Policy를 pub/sub으로 연결한다.

2. [Dev] 예약 시 잔여좌석수 감소 로직
`flight/src/main/java/airline/domain/Flight.java`
```java
public static void decreaseRemainingSeats(
    ReservationPlaced reservationPlaced
) {
    repository().findById(reservationPlaced.getFlightId()).ifPresent(flight->{
        if(flight.getRemainingSeatsCount() >= reservationPlaced.getSeatQty()){
            flight.setRemainingSeatsCount(flight.getRemainingSeatsCount() - reservationPlaced.getSeatQty()); 
            repository().save(flight);
            
            RemainingSeatsDecreased remainingSeatDecreased = new RemainingSeatsDecreased(flight);
            remainingSeatDecreased.publishAfterCommit();
        }else{
            SeatsSoldOut seatsSoldOut = new SeatsSoldOut(flight);
            seatsSoldOut.setReservationId(reservationPlaced.getId());
            seatsSoldOut.publishAfterCommit();
        }
    });
}
```
3. [Dev] 잔여좌석 솔드아웃 시 예약 취소 처리 로직
```java
public static void updateStatus(SeatsSoldOut seatsSoldOut) {
    repository().findById(seatsSoldOut.getReservationId()).ifPresent(reservation->{
        reservation.setStatus("CANCELED");
        repository().save(reservation);
    });
}
```

#### 작업 결과
- `reservation` localhost:8082
- `flight` localhost:8083
```bash
# 1번항공권의 잔여좌석수 5개로 초기셋팅
http :8083/flights remainingSeatsCount=5 flightCode="ACE890" takeoffDate="2025-12-25" cost=200000

# 1번고객이 1번항공권 3자리 예약 (SUCCEED)
http :8082/reservations customerId=1 flightId=1 seatQty=3 reserveDate="2024-11-21" status="SUCCEED"

# 2번고객이 1번항공권 3자리 예약 (CANCELED)
http :8082/reservations customerId=2 flightId=1 seatQty=3 reserveDate="2024-11-22" status="SUCCEED"
```

```bash
# 항공권 잔여좌석수 확인
http :8083/flights
```
![image](https://github.com/user-attachments/assets/eab827a6-9a18-49a8-ae6d-8bd0bbfa2ef5)

```bash
# 예약 내역 확인
http :8082/reservations
```
![image](https://github.com/user-attachments/assets/3d3ccdb7-c7e6-47b8-91de-652b33e3df54)



### 2. 단일 진입점 `Gateway`
### 3. 분산 데이터 프로젝션 `CQRS`

## 🤵🏻‍♂️ [Ops/PaaS] 운영
### 1. 클라우드 배포 `HPA`
### 2. 컨테이너로부터 환경 분리 `ConfigMap`
### 3. 클라우드 스토리지 활용 PVC
### 4. 셀프힐링/무정지배포 `Liveness/Rediness Probe`
### 5. 서비스 메쉬 응용 `Mesh`
### 6. 통합 모니터링 `Loggeration/Monitoring`


<!-- 
## Model
www.msaez.io/#/70644449/storming/airline

## Before Running Services
### Make sure there is a Kafka server running
```
cd kafka
docker-compose up
```
- Check the Kafka messages:
```
cd infra
docker-compose exec -it kafka /bin/bash
cd /bin
./kafka-console-consumer --bootstrap-server localhost:9092 --topic
```

## Run the backend micro-services
See the README.md files inside the each microservices directory:

- reservation
- flight
- payment
- dashboard


## Run API Gateway (Spring Gateway)
```
cd gateway
mvn spring-boot:run
```

## Test by API
- reservation
```
 http :8088/reservations id="id" customerId="customerId" flightId="flightId" seatQty="seatQty" reserveDate="reserveDate" status="status" 
```
- flight
```
 http :8088/flights id="id" remainingSeatsCount="remainingSeatsCount" flightCode="flightCode" takeoffDate="takeoffDate" cost="cost" 
```
- payment
```
 http :8088/payments id="id" customerId="customerId" cost="cost" 
```
- dashboard
```
```


## Run the frontend
```
cd frontend
npm i
npm run serve
```

## Test by UI
Open a browser to localhost:8088

## Required Utilities

- httpie (alternative for curl / POSTMAN) and network utils
```
sudo apt-get update
sudo apt-get install net-tools
sudo apt install iputils-ping
pip install httpie
```

- kubernetes utilities (kubectl)
```
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

- aws cli (aws)
```
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

- eksctl 
```
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
```
 -->
