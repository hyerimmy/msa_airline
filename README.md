# ✈️ Airline Service (항공사 예약 시스템)
> 사내 교육 'Cloud Native 개발자 코스' Final Project


<!-- code_chunk_output -->
  - [👩🏻‍🎨 [IaaS] 아키텍처](#-iaas-아키텍처)
    - [1. 클라우드 아키텍처 구성](#1-클라우드-아키텍처-구성)
      - [MSA 아키텍처 구성도](#msa-아키텍처-구성도)
  - [👷🏻 [Biz] 모델링](#-biz-모델링)
    - [1. 이벤트 스토밍](#1-이벤트-스토밍)
  - [🧑🏻‍💻 [Dev] MSA 개발](#-dev-msa-개발)
    - [1. 분산 트랜잭션 `Saga` & 보상처리 `Compensation`](#1-분산-트랜잭션-saga--보상처리-compensation)
    - [2. 단일 진입점 `Gateway`](#2-단일-진입점-gateway)
    - [3. 분산 데이터 프로젝션 `CQRS`](#3-분산-데이터-프로젝션-cqrs)
  - [🤵🏻‍♂️ [Ops/PaaS] 운영](#️-opspaas-운영)
    - [1. 클라우드 배포 `Container`](#1-클라우드-배포-container)
    - [2. 컨테이너 자동 확장 `HPA`](#2-컨테이너-자동-확장-hpa)
    - [3. 컨테이너로부터 환경 분리 `ConfigMap`](#3-컨테이너로부터-환경-분리-configmap)
    - [4. 클라우드 스토리지 활용 `PVC`](#4-클라우드-스토리지-활용-pvc)
    - [5. 무정지배포 `Rediness Probe`](#5-무정지배포-rediness-probe)
    - [6. 서비스 메쉬 응용 `Mesh`](#6-서비스-메쉬-응용-mesh)
    - [7. 통합 모니터링 `Loggeration/Monitoring`](#7-통합-모니터링-loggerationmonitoring)

<!-- /code_chunk_output -->



## 👩🏻‍🎨 [IaaS] 아키텍처
### 1. 클라우드 아키텍처 구성
#### MSA 아키텍처 구성도
<img width="976" alt="image" src="https://github.com/user-attachments/assets/40f25dec-7de3-48ae-8af5-029c753d995d">


## 👷🏻 [Biz] 모델링
### 1. 이벤트 스토밍
#### 📌 시나리오
1. 고객은 원하는 매수만큼 항공권 예약을 할 수 있고, 잔여좌석이 있다면 결제를 진행한다.
2. 고객이 예약하였는데 잔여좌석이 부족하다면 예약 취소처리한다.
3. 고객은 예약을 취소할 수 있다.
4. 예약을 취소할 경우 잔여좌석도 다시 예약 전으로 되돌린다.
5. 고객의 '항공권 예약 횟수'와 '최근 예약일'을 대시보드에서 확인할 수 있다.

#### 📌 이벤트 스토밍 결과 
<img width="1247" alt="image" src="https://github.com/user-attachments/assets/fc1ebc5f-b798-47e8-8049-dfd711163fe7">

#### 📌 시나리오 검증
![image](https://github.com/user-attachments/assets/16be4e47-3da2-4889-8cc6-be0d318beb1e)


## 🧑🏻‍💻 [Dev] MSA 개발
### 1. 분산 트랜잭션 `Saga` & 보상처리 `Compensation`
> 고객이 항공권을 예약하였지만 남은 좌석이 없어 SeatsSoldOut 이벤트가 Pub되었을 때, 이를 Sub하고 있는 UpdateStatus 이벤트를 발행하여 고객의 예약요청(reservationId) 상태값(status)을 'CANCELED'로 변경한다.

#### 💬시나리오
1. 비행기 잔여석보다 더 많은 예약을 발행하는 ReservationPlaced 이벤트가 발행된다. (비즈니스 예외 케이스)
2. flight 서비스에서 잔여석보다 더 많은 예약이 불가능함에 따라 SeatSoldOut 이벤트를 pub한다.
3. SeatSoldOut 이벤트를 sub하고 있는 reservation 서비스에서는 원예약(reservationId)의 상태값(status)을 수정한다.

#### 🚧 작업 내용

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

#### 🏁 작업 결과
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
1. 게이트웨이로 마이크로 서비스 GET 접근
   - 아래와 같이 마이크로서비스 포트 8083 뿐 아니라 게이트웨이로 설정한 포트 8088로 접근해도 진입된다.
    ![image](https://github.com/user-attachments/assets/7e25763d-50db-4e1e-97ae-539c57a0c2ab)
    ![image](https://github.com/user-attachments/assets/cb21c102-5046-4e14-8e58-ca6b5518f9f2)

2. 게이트웨이로 마이크로 서비스 POST 접근
   - 아래와 같이 post 요청도 게이트웨이 포트를 통해 진입해 처리 가능하다.
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
#### 💬시나리오
고객이 예약을 할 경우, 예약 횟수와 가장 최근 예약한 일자를 Dashboard서비스의 ReadModel에 업데이트해준다.

#### 🚧 작업 내용
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

#### 🏁 작업 결과
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
   - 각 마이크로 서비스의 deploy.yaml에 ACR 정보 입력
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

#### 💬시나리오
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


### 3. 컨테이너로부터 환경 분리 `ConfigMap`
> 컨테이너로부터 환경변수를 분리하여 쿠버네티스에 ConfigMap으로 저장한다.

#### 💬시나리오
- flight 서비스의 DB정보, log level을 ConfigMap으로 저장하고 활용한다.

#### 작업내용
1. YAML 기반의 ConfigMap을 생성하고 배포한다.
    ```bash
    kubectl apply -f - <<EOF
    apiVersion: v1
    kind: ConfigMap
    metadata:
      name: config-flight
      namespace: default
    data:
      FLIGHT_DB_URL: jdbc:mysql://mysql:3306/connectdb1?serverTimezone=Asia/Seoul&useSSL=false
      FLIGHT_DB_USER: myuser
      FLIGHT_LOG_LEVEL: DEBUG
    EOF
    ```

2. 생성된 ConfigMap 객체를 확인한다.
    ```bash
    kubectl get configmap
    kubectl get configmap config-flight -o yaml
    ```
    ![image](https://github.com/user-attachments/assets/972680e7-cf8b-4f97-b8a6-acd057656664)

3. 주문서비스의 Logging 레벨을 Configmap의 ORDER_DEBUG_INFO 참조하도록 설정한다.
    ```bash
    logging:
      level:
        root: ${FLIGHT_LOG_LEVEL}
        org:
          hibernate:
            SQL: ${FLIGHT_LOG_LEVEL}
          springframework:
            cloud: ${FLIGHT_LOG_LEVEL}
    ```
    ![image](https://github.com/user-attachments/assets/7595b9a4-0370-4912-a408-bccc1e4eb749)

4. flight서비스의 deploy.yaml에 아래 환경변수 추가한다.
    ```yaml
    env:
      - name: FLIGHT_LOG_LEVEL
        valueFrom:
          configMapKeyRef:
              name: config-flight
              key: FLIGHT_LOG_LEVEL
    ```
    ![image](https://github.com/user-attachments/assets/82c21718-bf7a-4aa8-8e34-7eff9bb8b787)

#### 작업결과
1. flight 서비스 Kubernetes에 배포 후, 컨테이너 Log를 통해 ConfigMap에 설정한 DEBUG 로그레벨이 적용되었음을 확인한다.
    ```bash
    kubectl logs -l app=flight
    ```
    ![image](https://github.com/user-attachments/assets/b3b2c915-e2ab-47b5-8149-aa54c2d89447)

2. env 조회 명령어를 통해 Configmap에서 각 Container로 환경정보가 알맞게 전달되었음을 확인한다.
    - 배포시 전달된 ORDER_LOG_LEVEL 정보가 주문 컨테이너 OS에 설정되었음을 알 수 있다.
    ```bash
    kubectl exec pod/flight-78d456dfd8-qbcj8 -- env
    ```
    ![image](https://github.com/user-attachments/assets/790aef6e-59b5-483f-b6af-25ad8855bac8)

### 4. 클라우드 스토리지 활용 `PVC`
#### 💬시나리오
- 항공사의 이벤트 문구를 ConfigMap으로 저장하여 쿠버네티스에서 관리한다.

#### 작업내용
1. 아래 YAML로 'promotional-text'라는 PVC(Persistence Volume Claim)를 생성한다.
    ```bash
    kubectl apply -f - <<EOF
    apiVersion: v1
    **kind: PersistentVolumeClaim**
    metadata:
      name: promotional-text
    spec:
      accessModes:
      - ReadWriteMany
      storageClassName: azurefile
      resources:
        requests:
          storage: 1Gi
    EOF
    ```

2. pvc가 제대로 생성되었는지 확인한다.
    ```bash
    kubectl get pvc
    ```
    ![image](https://github.com/user-attachments/assets/13a7ca99-c6e8-4f62-ab4d-288b314e57a1)

3. flight 마이크로서비스에 볼륨 설정 추가
    ```bash
    volumeMounts:
      - mountPath: "/mnt/data"
        name: volume
        volumes:
        - name: volume
          persistentVolumeClaim:
            claimName: promotional-text 
    ```
    ![image](https://github.com/user-attachments/assets/310c4932-e795-475d-bdc0-2a7f4ceb42e8)

#### 작업결과
1. flight 컨테이너에 접속하여 summer-event.txt를 작성한다.
    ```bash
    kubectl exec -it pod/flight-54b56676f4-cr28w -- /bin/sh
    cd /mnt/data
    echo "[SUMMER EVENT] 50% DISCOUNT!" > summer-event.txt
    ```
    ![image](https://github.com/user-attachments/assets/9b2f1505-092e-4687-aee6-2e1f1433f26d)

2. flight 서비스를 2개로 Scale Out하고, 확장된 주문 서비스에서 summer-event.txt를 조회하여 이전 pod에서 작성한 문구가 조회됨을 확인한다.
    ```bash
    kubectl scale deploy flight --replicas=2
    kubectl exec -it pod/flight-54b56676f4-fzvkk -- /bin/sh
    cd /mnt/data
    cat summer-event.txt
    ```
    ![image](https://github.com/user-attachments/assets/c20d75be-ac36-4468-8229-3ac21631cc69)

### 5. 무정지배포 `Rediness Probe`
> 컨테이너의 상태를 관리하는 readinessProbe 설정을 하여 무정지 배포를 적용한다.

#### 작업내용
1. flight서비스의 delpoy.yaml 내에 readiness 설정을 주입한다.
    ```bash
    readinessProbe:
      httpGet:
        path: '/flights'
        port: 8080
      initialDelaySeconds: 10
      timeoutSeconds: 2
      periodSeconds: 5
      failureThreshold: 10
    ```
    ![image](https://github.com/user-attachments/assets/96957976-6fe1-4671-b560-ca749193c829)

#### 작업결과
**[readinessProbe 적용 전]**
  1. seege를 동작시킨 상태에서 배포를 진행한다.
      ```bash
      # siege를 사용해 충분한 시간만큼 부하를 준다.
      kubectl exec -it siege -- /bin/bash
      siege -c1 -t60S -v http://flight:8080/flights --delay=1S
      
      # 배포를 반영한다.
      kubectl apply -f deployment.yaml
      ```
  2. siege 로그를 통해 배포시 정지시간이 발생한것을 확인할 수 있다.
      ```bash
      Lifting the server siege...
      Transactions:		         65 hits
      Availability:		        58.056 %
      ```
     ![image](https://github.com/user-attachments/assets/1e917df4-468b-46aa-a5ea-040d400dc2be)


**[readinessProbe 적용 후]**
  1. seege를 동작시킨 상태에서 배포를 진행한다.
      ```bash
      # siege를 사용해 충분한 시간만큼 부하를 준다.
      kubectl exec -it siege -- /bin/bash
      siege -c1 -t60S -v http://flight:8080/flights --delay=1S
      
      # 배포를 반영한다.
      kubectl apply -f deployment.yaml
      ```
  2. siege 로그를 통해 배포시 무정지로 배포된 것을 확인할 수 있다.
      ```bash
      Lifting the server siege...
      Transactions:		         108 hits
      Availability:		      100.00 %
      ```
      ![image](https://github.com/user-attachments/assets/eeb3c4f1-1337-42eb-a8e6-f4e890d9acbb)


### 6. 서비스 메쉬 응용 `Mesh`
> Istio Service Mesh를 내 클러스터에 설치하고 Sidecar 인젝션을 진행한다.

#### 작업내용
1. Istio 설치
    ```bash
    # 기본적인 구성인 `demo` 를 기반으로 설치
    istioctl install --set profile=demo --set hub=gcr.io/istio-release
    ```
    ![image](https://github.com/user-attachments/assets/5a396052-98a3-4dde-9db5-60d133115587)


2. Istio add-on Dashboard 설치
    ```bash
    mv samples/addons/loki.yaml samples/addons/loki.yaml.old
    curl -o samples/addons/loki.yaml https://raw.githubusercontent.com/msa-school/Lab-required-Materials/main/Ops/loki.yaml
    kubectl apply -f samples/addons
    ```

3. 인그레이스 게이트웨이 설치
    ```yaml
    # Helm 3.x 설치(권장)
    curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 > get_helm.sh
    chmod 700 get_helm.sh
    ./get_helm.sh
    ```
    ```yaml
    # 인그레이스 게이트웨이를 위한 Helm repo 설정
    helm repo add stable https://charts.helm.sh/stable
    helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
    helm repo update
    kubectl create namespace ingress-basic
    ```
    ```bash
    # Nginx Ingress Controller 설치 (Azure)
    helm install ingress-nginx ingress-nginx/ingress-nginx --namespace ingress-basic --set controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-health-probe-request-path"=/healthz
    ```

4. Istio Dashboard를 위한 라우팅 룰(Ingress) 설정
    ```yaml
    kubectl apply -f - <<EOF
    apiVersion: networking.k8s.io/v1
    kind: "Ingress"
    metadata: 
      name: "airline-ingress"
      namespace: istio-system
      annotations: 
        nginx.ingress.kubernetes.io/ssl-redirect: "false"
        ingressclass.kubernetes.io/is-default-class: "true"
    spec: 
      ingressClassName: nginx
      rules: 
        - host: ""
          http: 
            paths: 
              - path: /kiali
                pathType: Prefix
                backend: 
                  service:
                    name: kiali
                    port:
                      number: 20001
              - path: /grafana
                pathType: Prefix
                backend: 
                  service:
                    name: grafana
                    port:
                      number: 3000
              - path: /prometheus
                pathType: Prefix
                backend: 
                  service:
                    name: prometheus
                    port:
                      number: 9090
              - path: /loki
                pathType: Prefix
                backend: 
                  service:
                    name: loki
                    port:
                      number: 3100
    EOF
    ```

5. Sidecar Injection
   istio-injection을 위한 airline이라는 네임스페이스를 생성하고, 서비스를 배포한다.
    ```yaml
    kubectl create namespace airline
    kubectl label namespace airline istio-injection=enabled
    kubectl apply -f deploy.yaml -n airline
    ```

#### 작업결과
1. istio설치 후 실행중인 파드를 확인한다.
    ```bash
    kubectl get svc -n istio-system
    ```
    ![image](https://github.com/user-attachments/assets/80ba4927-0b42-4c56-a892-b6953f2bab1d)

2. 마이크로서비스가 정상적으로 배포되었음을 확인한다.
    ![image](https://github.com/user-attachments/assets/6aa1d267-c490-4154-962c-454c928fa4db)


### 7. 통합 모니터링 `Loggeration/Monitoring`
> Grafana를 사용하여 마이크로 서비스의 로그들을 통합하여 모니터링해 본다.

#### 작업내용
1. Install Grafana with Helm
    - Helm에 Grafana 저장소 추가
        ```bash
        helm repo add grafana https://grafana.github.io/helm-charts
        helm repo update
        ```
        <img width="567" alt="image1" src="https://github.com/user-attachments/assets/37cee98b-c57b-4e8e-b37c-ba64ee7d13c7">
    - loki-stack 설치 스크립트 다운로드
        ```bash
        helm show values grafana/loki-stack > ./loki-stack-values.yaml
        ```
    - loki-stack-values.yaml을 편집하여 아래처럼 PLG 스텍만('true’로 수정) 선택한다.
        ```bash
        test-pod: enabled: false // 2번째 line 수정
        loki: enabled: true
        promtail: enabled: true
        fluent-bit: enabled: false
        grafana: enabled: true // 37번째 line 수정
        prometheus: enabled: false
        filebeat: enabled: false 
        logstash: enabled: false
        ```
    - Helm으로 PLG 스텍 설치   
        ```bash
        kubectl create namespace logging
        helm install loki-stack grafana/loki-stack --values ./loki-stack-values.yaml -n logging
        ```
2. 설치된 Pod 목록을 확인한다.
    ```bash
    kubectl get pod -n logging
    ```
    
#### 작업결과
1. Grafana External-IP를 생성한다.
   ```bash
    kubectl patch svc loki-stack-grafana -n logging -p '{"spec": {"type": "LoadBalancer"}}'
    kubectl get svc -n logging
    ```
2. 발급된 External-IP로 브라우저에서 접속한다.
3. 실행시간에 따른 4가지 마이크로 서비스 로그가 로그리게이션되어 나타난다.
    <img width="567" alt="image2" src="https://github.com/user-attachments/assets/eb185634-87dc-420d-b341-33b80e883572">



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
