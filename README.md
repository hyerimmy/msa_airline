# ✈️ Airline Service (항공사 예약 시스템)
> 사내 교육 'Cloud Native 개발자 코스' Final Project

## 👩🏻‍🎨 [IaaS] 아키텍처
### 1. 클라우드 아키텍처 구성
#### MSA 아키텍처 구성도

## 👷🏻 [Biz] 모델링
### 1. 이벤트 스토밍
#### 시나리오
#### 이벤트 스토밍 결과 
![image](https://github.com/user-attachments/assets/2c244fbb-44b1-4cd6-b7f5-7378304c1774)

#### 시나리오 검증

## 🧑🏻‍💻 [Dev] MSA 개발
### 1. 분산 트랜잭션 `Saga` & 보상처리 `Compensation`
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
