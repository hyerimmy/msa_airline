# mvn
brew install mvn

# httpie
pip install httpie

# docker
brew install cask docker
brew install cask docker-compose
# + docker desktop 실행

# azure cli
brew install azure-cli

# jenkins
brew install jenkins-lts
brew services restart jenkins-lts

# kafka 를 docker를 통하여 실행
cd infra
docker-compose up

# kafka 유틸리티가 포함된 위치에 접속하기 위하여 docker 를 통하여 shell 에 진입
cd infra
docker-compose exec -it kafka /bin/bash