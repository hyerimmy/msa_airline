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
> 마이크로서비스 4개(dashboard, flight, payment, reservation)를 API 게이트웨이를 활용해 엔드포인트를 단일화한다. 

#### [2-1. local환경] 작업 내용
1. Docker실행
    - http 클라이언트를 설치하고 kafka를 Local에 컨테이너 기반으로 실행한다.
    
    ```bash
    brew install httpie
    cd infra
    docker-compose up
    ```
    
2. 각 마이크로서비스 실행
    ```bash
    cd dashboard
    mvn spring-boot:run
    
    cd flight
    mvn spring-boot:run
    
    cd payment
    mvn spring-boot:run
    
    cd reservation
    mvn spring-boot:run
    
    cd gateway
    mvn spring-boot:run
    ```
    
3. API 게이트웨이 역할을 하는 ‘gateway’ 서비스의 application.yml에 아래와 같이 설정해준다.
    - gateway : 8088
        - reservation : 8082
        - flight : 8083
        - payment : 8084
        - dashboard : 8085
    
    ```yaml
    server:
      port: 8088
    
    ---
    
    spring:
      profiles: default
      cloud:
        gateway:
    #<<< API Gateway / Routes
          routes:
            - id: reservation
              uri: http://localhost:8082
              predicates:
                - Path=/reservations/**, 
            - id: flight
              uri: http://localhost:8083
              predicates:
                - Path=/flights/**, 
            - id: payment
              uri: http://localhost:8084
              predicates:
                - Path=/payments/**, 
            - id: dashboard
              uri: http://localhost:8085
              predicates:
                - Path=, 
            - id: frontend
              uri: http://localhost:8080
              predicates:
                - Path=/**
    #>>> API Gateway / Routes
          globalcors:
            corsConfigurations:
              '[/**]':
                allowedOrigins:
                  - "*"
                allowedMethods:
                  - "*"
                allowedHeaders:
                  - "*"
                allowCredentials: true
    ...
    
    ```
    
#### [2-1. local환경] 작업 결과
아래와 같이 마이크로서비스 포트 8083 뿐 아니라 게이트웨이로 설정한 포트 8088로 접근해도 진입된다.
![image](https://github.com/user-attachments/assets/7e25763d-50db-4e1e-97ae-539c57a0c2ab)
![image](https://github.com/user-attachments/assets/cb21c102-5046-4e14-8e58-ca6b5518f9f2)

아래와 같이 post 요청도 게이트웨이 포트를 통해 진입해 처리 가능하다.
![image](https://github.com/user-attachments/assets/b75460b6-63b8-4a92-be65-90b2f8ae2b70)
![image](https://github.com/user-attachments/assets/5c83f7b8-16b4-49e1-8a76-5917700a56b0)

---

#### [2-2. cloud환경] 작업 내용

1. 게이트웨이 application.yaml에 아래와 같이 라우팅룰 설정 후 배포한다.
    ```bash
    spring:
      profiles: default
      cloud:
        gateway:
    #<<< API Gateway / Routes
          routes:
            - id: reservation
              uri: http://localhost:8082
              predicates:
                - Path=/reservations/**, 
            - id: flight
              uri: http://localhost:8083
              predicates:
                - Path=/flights/**, 
            - id: payment
              uri: http://localhost:8084
              predicates:
                - Path=/payments/**, 
            - id: dashboard
              uri: http://localhost:8085
              predicates:
                - Path=, 
    ```

2. 게이트웨이 파드의 external IP를 조회한다.
    ![image](https://github.com/user-attachments/assets/965cf5f6-35b4-4b3f-be5e-76f453c181f9)


#### [2-2. cloud환경] 작업 결과
모든 마이크로서비스는 게이트웨이 ip를  통해 접근된다.
![image](https://github.com/user-attachments/assets/c6dd1145-3169-4788-bad4-4dd0005beac3)


### 3. 분산 데이터 프로젝션 `CQRS`
#### 시나리오
고객이 예약을 할 경우, 예약 횟수와 가장 최근 예약한 일자를 Dashboard서비스의 ReadModel에 업데이트해준다.

#### 작업 내용
1. [Event Storming] ReadModel ‘CustomerInfo’ 설정
![image](https://github.com/user-attachments/assets/40c2349d-ba2b-40bd-aea5-6e941a10f5f5)
![image](https://github.com/user-attachments/assets/4f8b8d28-0b5e-4dd5-b332-c20e6e45d1d3)


2. [Dev] CustomerInfo CQRS 로직 작성
    `dashboard/src/main/java/airline/infra/CustomerInfoViewHandler.java`
    ```java
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
    ```

#### 작업 결과
1. 3차례 예약 진행
    ```bash
    # 1번항공권의 잔여좌석수 5개로 초기셋팅
    http :8083/flights remainingSeatsCount=5 flightCode="ACE890" takeoffDate="2025-12-25" cost=200000
    
    # 2024-11-11 / 1번고객 / 1번항공권 / 3자리 예약
    http :8082/reservations customerId=1 flightId=1 seatQty=3 reserveDate="2024-11-11" status="SUCCEED"
    
    # 2024-11-23 / 2번고객 / 1번항공권 / 1자리 예약
    http :8082/reservations customerId=2 flightId=1 seatQty=1 reserveDate="2024-11-23" status="SUCCEED"
    
    # 2024-12-25 / 2번고객 / 1번항공권 / 1자리 예약
    http :8082/reservations customerId=2 flightId=1 seatQty=1 reserveDate="2024-12-25" status="SUCCEED"
    ```

2. Dashboard의 customerInfo에 통계값 입력된 것 확인
    ```bash
    http :8085/customerInfos
    ```
    ![image](https://github.com/user-attachments/assets/365a62cb-5d50-487e-a20e-62d7e4b9b6d8)

## 🤵🏻‍♂️ [Ops/PaaS] 운영
### 1. 클라우드 배포 `Container`
> Azure VM(가상머신) 상에서 Jenkins를 설치한 다음, 대상 서비스를 도커라이징하고 ACR(Azure Container Registry)에 푸쉬한 다음 AKS에 배포하는 전 과정을 Jenkins 파이프라인으로 구성해 본다.
#### 작업내용
1. Azure 리소스 생성
   ![image](https://github.com/user-attachments/assets/0a357212-fbe5-4c8b-b901-937895044f32)

2. Azure ACR, AKS SSO 로그인
    ```bash
    # az login (SSO)
    az login --use-device-code
    
    # Kubernetes login (SSO)
    az aks get-credentials --resource-group user16-rsrcgrp --name user16-aks
    
    # Azure AKS와 ACR 바인딩
    az aks update -n user16-aks -g user16-rsrcgrp --attach-acr user16
    ```

3. Jenkins에 Azure, Git 접근 크레덴셜 등록
   ![image](https://github.com/user-attachments/assets/019cd782-a748-41d6-8f13-5b7acf4c2336)

4. CI/CD 파이프라인 작성
   - Jenkinsfile
     ```
     pipeline {
        agent any
    
        environment {
            SERVICES = 'gateway,dashboard,flight,payment,reservation'
            REGISTRY = 'user16.azurecr.io'
            IMAGE_NAME = 'airline'
            AKS_CLUSTER = 'user16-aks'
            RESOURCE_GROUP = 'user16-rsrcgrp'
            AKS_NAMESPACE = 'default'
            AZURE_CREDENTIALS_ID = 'Azure-Cred'
            TENANT_ID = '29d166ad-94ec-45cb-9f65-561c038e1c7a' // Service Principal 등록 후 생성된 ID
            GIT_USER_NAME = 'hyerimmy'
            GIT_USER_EMAIL = 'heyrim2010@naver.com'
            GITHUB_CREDENTIALS_ID = 'Github-Cred'
            GITHUB_REPO = 'https://github.com/hyerimmy/msa_airline.git'
            GITHUB_BRANCH = 'main'
        }
     
        stages {
            stage('Clone Repository') {
                steps {
                    checkout scm
                }
            }
    
            stage('Build and Deploy Services') {
                steps {
                    script {
                        def services = SERVICES.tokenize(',') // Use tokenize to split the string into a list
                        for (int i = 0; i < services.size(); i++) {
                            def service = services[i] // Define service as a def to ensure serialization
                            dir(service) {
                                stage("Maven Build - ${service}") {
                                    withMaven(maven: 'Maven') {
                                        sh 'mvn package -DskipTests'
                                    }
                                }
    
                                stage("Docker Build - ${service}") {
                                    def image = docker.build("${REGISTRY}/${service}:v${env.BUILD_NUMBER}")
                                }
    
                                stage('Azure Login') {
                                    withCredentials([usernamePassword(credentialsId: env.AZURE_CREDENTIALS_ID, usernameVariable: 'AZURE_CLIENT_ID', passwordVariable: 'AZURE_CLIENT_SECRET')]) {
                                        sh 'az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant ${TENANT_ID}'
                                    }
                                }
    
                                stage("Push to ACR - ${service}") {
                                    sh "az acr login --name ${REGISTRY.split('\\.')[0]}"
                                    sh "docker push ${REGISTRY}/${service}:v${env.BUILD_NUMBER}"
                                }
    
                                stage("Deploy to AKS - ${service}") {
                                    
                                    sh "az aks get-credentials --resource-group ${RESOURCE_GROUP} --name ${AKS_CLUSTER}"
    
                                    sh 'pwd'
                                    
                                    sh """
                                    sed 's/latest/v${env.BUILD_ID}/g' kubernetes/deploy.yaml > output.yaml
                                    cat output.yaml
                                    kubectl apply -f output.yaml
                                    kubectl apply -f kubernetes/service.yaml
                                    rm output.yaml
                                    """
                                }
                            }
                        }
                    }
                }
            }
     
            stage('CleanUp Images') {
                steps {
                    script {
                        def services = SERVICES.tokenize(',') 
                        for (int i = 0; i < services.size(); i++) {
                            def service = services[i] 
                            sh "docker rmi ${REGISTRY}/${service}:v${env.BUILD_NUMBER}"
                        }
                    }
                }
            }
        }
    }
     ```
   - 각 마이크로 서비스의 deploy.yaml
   
#### 작업결과
1. 커밋하니 자동으로 CI/CD 파이프라인이 동작한다.
    ![image](https://github.com/user-attachments/assets/7bff10e5-efeb-486d-aa95-c4e7ee3b285d)

2. 파이프라인은 모두 정상적으로 완료되었음을 확인할 수 있다.
   ![image](https://github.com/user-attachments/assets/20eeeaa9-ad5b-4212-95d7-e6f6218aa200)
   ![image](https://github.com/user-attachments/assets/8e5f955c-5e48-450b-baf6-c7bcb2fc8b0d)
   ![image](https://github.com/user-attachments/assets/c20da300-8e08-4628-a6a3-8d16e73651d0)

3. Azure portal에서도 ACR, AKS에서 이미지가 올라가고 배포가 되었음을 확인할 수 있다.
   ![image](https://github.com/user-attachments/assets/0539b6c9-f6fb-4e6e-8102-9a5553ba6718)
   ![image](https://github.com/user-attachments/assets/b030ce38-7233-4c29-aecd-4f16b0982676)

### 2. 컨테이너 자동 확장 `HPA`
> 요청이 많이 들어올때 Auto Scale-Out 설정을 통하여 서비스를 동적으로 확장한다.

#### 시나리오
항공권 예약 요청이 갑자기 많아질 경우, 동적으로 서비스에 스케일아웃을 적용시켜 요청을 처리하도록 한다.

#### 작업내용
1. reservation서비스의 deploy.yaml을 아래와 같이 CPU 요청에 대한 값을 추가 작성 후 배포한다.
    ```yaml
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: reservation
      labels:
        app: reservation
      template:
        ...
        spec:
          containers:
            - name: reservation
              image: user16.azurecr.io/reservation:latest
              ports:
                - containerPort: 8080
              resources: #cpu
                requests:
                  cpu: "200m"
    ```
2. 오토 스케일링 설정명령어 호출한다.
    ```bash
    # cpu-percent=50 : Pod 들의 요청 대비 평균 CPU 사용율(YAML Spec.에서 요청량이 200 milli-cores일때, 모든 Pod의 평균 CPU 사용율이 100 milli-cores(50%)를 넘게되면 HPA 발생)
    kubectl autoscale deployment reservation --cpu-percent=50 --min=1 --max=3
    ```
    ![image](https://github.com/user-attachments/assets/99b0cc43-18dc-49a1-9cc7-d0a2149b7d96)

#### 작업결과
1. 1번 터미널을 열어서 seige 명령으로 부하를 주어서 Pod 가 늘어나도록 한다.
    ```bash
    kubectl exec -it siege -- /bin/bash
    siege -c20 -t40S -v http://reservation:8080/reservations
    exit
    ```
    ![image](https://github.com/user-attachments/assets/120eeb94-a10e-415c-9f69-e56ee636ad96)

2. 2번 터미널을 열어 kubectl get po -w 명령을 사용하면, pod 가 생성되는 것을 확인할 수 있다.
   ![image](https://github.com/user-attachments/assets/c05d63eb-e969-4a51-b2ed-2d4fa4b13dd4)

3. `kubectl get hpa` 명령어를 통해 CPU 값이 늘어난 것을 확인 한다.
   ![image](https://github.com/user-attachments/assets/3c3b53b4-826d-439a-aea4-b354a00c6f89)


### 2. 컨테이너로부터 환경 분리 `ConfigMap`
#### 작업내용
#### 작업결과

### 3. 클라우드 스토리지 활용 PVC
#### 작업내용
#### 작업결과

### 4. 셀프힐링/무정지배포 `Liveness/Rediness Probe`
#### 작업내용
#### 작업결과

### 5. 서비스 메쉬 응용 `Mesh`
#### 작업내용
#### 작업결과

### 6. 통합 모니터링 `Loggeration/Monitoring`
#### 작업내용
#### 작업결과



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
