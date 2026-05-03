# Ubuntu Server Setup Guide

작성일: `2026-04-29`

이 문서는 `my-forever-music`을 Ubuntu 서버에 처음 올릴 때, 서버 설치 직후부터 개발 환경을 갖추기까지의 실제 작업 순서를 정리한 문서입니다.

현재 전략은 `MacBook 로컬에서 구현/시험 -> Ubuntu 이전` 순서입니다. 따라서 이 문서는 현재 단계의 주 개발 문서가 아니라, 로컬 시험 서비스가 안정화된 뒤 서버로 옮길 때 사용하는 이전 가이드입니다.

이 가이드는 `2026-04-29` 기준의 공식 문서를 참고해 작성했습니다.

## 1. 목표 상태

최종적으로 아래 구성을 목표로 합니다.

- Ubuntu 서버: `24.04 LTS` 권장
- 웹 프론트엔드: `Vite dev server` on `5173`
- 메인 API: `Spring Boot` on `8081`
- AI 서비스: `FastAPI` on `8000`
- DB: `PostgreSQL` on Docker
- 캐시: `Redis` on Docker
- 리버스 프록시: 호스트 설치 `Nginx`

현재 레포 상태를 기준으로, 가장 현실적인 초기 개발 방식은 아래입니다.

- 호스트에서 `web`, `api` 실행
- Docker는 `PostgreSQL`, `Redis`만 사용
- Nginx는 호스트에 직접 설치해서 `/`, `/api`, `/docs`, `/actuator`, `/ai`를 프록시
- Spotify OAuth callback 테스트를 위해 이후 `443` HTTPS reverse proxy도 연결

## 2. 서버 OS 선택

권장 OS는 `Ubuntu 24.04 LTS` 입니다.

이유:

- Docker 공식 문서에서 지원 대상
- Nginx 공식 패키지 지원 대상
- Python `3.12` 계열을 기본 패키지로 가져가기 쉬움
- 장기 유지보수에 유리

Ubuntu `22.04 LTS`도 가능하지만, 현재 프로젝트가 `Python 3.12`를 목표로 잡고 있으므로 `24.04 LTS`가 더 편합니다.

## 3. 최초 접속 후 기본 패키지 설치

관리자 권한이 있는 사용자로 접속한 뒤 아래를 먼저 실행합니다.

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y \
  git curl wget unzip zip jq ca-certificates gnupg gpg \
  build-essential pkg-config make \
  python3 python3-venv python3-pip \
  tmux
```

선택 사항:

- 시간대를 한국 기준으로 맞추려면:

```bash
sudo timedatectl set-timezone Asia/Seoul
timedatectl
```

## 4. 방화벽 기본 설정

Ubuntu 서버에서 `ufw`를 사용한다면 최소한 아래 포트를 엽니다.

```bash
sudo apt install -y ufw
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

주의:

- Docker 공식 문서는 Docker가 포트를 노출할 때 `ufw` 규칙을 우회할 수 있다고 안내합니다.
- 따라서 DB/Redis 포트는 외부에 직접 노출하지 않는 구성을 우선 권장합니다.

## 5. Docker Engine 설치

공식 Docker Engine Ubuntu 설치 가이드를 기준으로 설치합니다.

```bash
sudo apt remove $(dpkg --get-selections docker.io docker-compose docker-compose-v2 docker-doc podman-docker containerd runc | cut -f1) || true

sudo apt update
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo systemctl status docker --no-pager
sudo docker run hello-world
```

현재 사용자에게 Docker 권한을 주려면:

```bash
sudo usermod -aG docker "$USER"
newgrp docker
docker ps
```

## 6. Nginx 설치

공식 Nginx Ubuntu 패키지 가이드를 기준으로 stable 저장소를 추가합니다.

```bash
sudo apt install -y curl gnupg2 ca-certificates lsb-release ubuntu-keyring

curl https://nginx.org/keys/nginx_signing.key | gpg --dearmor \
  | sudo tee /usr/share/keyrings/nginx-archive-keyring.gpg >/dev/null

gpg --dry-run --quiet --no-keyring --import --import-options import-show \
  /usr/share/keyrings/nginx-archive-keyring.gpg

echo "deb [signed-by=/usr/share/keyrings/nginx-archive-keyring.gpg] \
https://nginx.org/packages/ubuntu $(lsb_release -cs) nginx" \
  | sudo tee /etc/apt/sources.list.d/nginx.list

echo -e "Package: *\nPin: origin nginx.org\nPin: release o=nginx\nPin-Priority: 900\n" \
  | sudo tee /etc/apt/preferences.d/99nginx

sudo apt update
sudo apt install -y nginx
sudo systemctl enable --now nginx
sudo systemctl status nginx --no-pager
```

## 7. Java 21 설치

Spring Boot API용으로 `Eclipse Temurin 21`을 설치합니다.

현재 프로젝트는 `Spring Boot 3.5.x + Java 21`을 기준으로 잡고 있습니다.

```bash
sudo apt install -y wget apt-transport-https gpg

wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | gpg --dearmor \
  | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg >/dev/null

echo "deb https://packages.adoptium.net/artifactory/deb \
$(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" \
  | sudo tee /etc/apt/sources.list.d/adoptium.list

sudo apt update
sudo apt install -y temurin-21-jdk
java -version
javac -version
```

## 7-1. JAVA_HOME 설정

Temurin 패키지를 설치하면 보통 `java`와 `javac` 실행은 바로 되지만, `JAVA_HOME`은 명시적으로 잡아두는 편이 안전합니다.

현재 설치된 Java 경로 확인:

```bash
readlink -f "$(which java)"
```

`JAVA_HOME` 계산:

```bash
JAVA_BIN=$(readlink -f "$(which java)")
JAVA_HOME=$(dirname "$(dirname "$JAVA_BIN")")
echo "$JAVA_HOME"
```

시스템 전체에 적용:

```bash
echo "export JAVA_HOME=$JAVA_HOME" | sudo tee /etc/profile.d/java.sh
echo 'export PATH=$JAVA_HOME/bin:$PATH' | sudo tee -a /etc/profile.d/java.sh
source /etc/profile.d/java.sh
echo "$JAVA_HOME"
java -version
```

## 7-2. 여러 Java 버전이 있을 때

서버에 JDK가 여러 개 설치되어 있으면 `update-alternatives`로 기본 버전을 선택합니다.

```bash
sudo update-alternatives --config java
sudo update-alternatives --config javac
```

선택 후 다시 확인:

```bash
java -version
javac -version
echo "$JAVA_HOME"
```

## 8. Gradle 준비

`services/api`는 Gradle 프로젝트입니다.

현재 레포에는 `gradlew` 스크립트와 `gradle-wrapper.properties`가 들어가 있지만, `gradle-wrapper.jar`는 아직 커밋되어 있지 않습니다.

즉 초기 상태에서는:

- `./gradlew` 실행 시 wrapper jar가 있으면 그걸 사용
- wrapper jar가 없으면 `system Gradle`로 자동 폴백
- Ubuntu 서버에서 한 번 `./gradlew wrapper`를 실행해 공식 wrapper 자산을 고정하는 것을 권장

Gradle 공식 문서는 wrapper가 있으면 wrapper 사용을 우선 권장하고, 없을 경우 Linux에서 수동 설치 또는 SDKMAN 사용을 안내합니다.

여기서는 수동 설치 예시를 사용합니다.

```bash
cd /tmp
wget https://services.gradle.org/distributions/gradle-8.14.3-bin.zip
sudo mkdir -p /opt/gradle
sudo unzip -d /opt/gradle gradle-8.14.3-bin.zip
echo 'export GRADLE_HOME=/opt/gradle/gradle-8.14.3' | sudo tee /etc/profile.d/gradle.sh
echo 'export PATH=${GRADLE_HOME}/bin:${PATH}' | sudo tee -a /etc/profile.d/gradle.sh
source /etc/profile.d/gradle.sh
gradle -v
```

설치 후 아래 명령으로 wrapper 자산을 고정합니다.

```bash
cd /srv/my-forever-music/services/api
./gradlew wrapper
ls -R gradle/wrapper
```

## 9. Node.js LTS 설치

`2026-04-29` 기준 Node.js `v24.14.1` LTS를 기준으로 안내합니다.  
공식 다운로드 아카이브 기준 `linux-x64`와 `linux-arm64` 바이너리가 제공됩니다.

`amd64/x86_64` 서버 예시:

```bash
cd /tmp
wget https://nodejs.org/dist/v24.14.1/node-v24.14.1-linux-x64.tar.xz
sudo tar -xJf node-v24.14.1-linux-x64.tar.xz -C /opt
sudo ln -sfn /opt/node-v24.14.1-linux-x64 /opt/node
echo 'export PATH=/opt/node/bin:${PATH}' | sudo tee /etc/profile.d/node.sh
source /etc/profile.d/node.sh
node -v
npm -v
```

`arm64` 서버라면 파일명을 `node-v24.14.1-linux-arm64.tar.xz`로 바꾸면 됩니다.

## 10. pnpm 활성화

Node 공식 문서의 `Corepack` 기능을 사용해 `pnpm`을 활성화합니다.

```bash
corepack enable
corepack install --global pnpm@*
pnpm -v
```

## 11. 프로젝트 클론

원하는 작업 디렉토리에 레포를 가져옵니다.

```bash
sudo mkdir -p /srv
sudo chown "$USER":"$USER" /srv
cd /srv
git clone <YOUR_REPOSITORY_URL> my-forever-music
cd my-forever-music
```

이미 로컬에서 작업한 내용을 서버로 옮기는 경우에는 Git remote 또는 `rsync` 기준으로 가져오면 됩니다.

## 12. DB/Redis 개발용 컨테이너 실행

현재 Ubuntu 개발 기준 Compose 템플릿은 `PostgreSQL`과 `Redis`만 올리도록 사용합니다.

두 서비스 포트는 기본적으로 `127.0.0.1`에만 바인딩되도록 작성되어 있습니다.

환경 변수 파일 준비:

```bash
cd /srv/my-forever-music
cp infra/docker/env.ubuntu.example infra/docker/.env.ubuntu-dev
```

컨테이너 실행:

```bash
docker compose \
  --env-file infra/docker/.env.ubuntu-dev \
  -f infra/docker/docker-compose.ubuntu-dev.yml \
  up -d
```

확인:

```bash
docker compose -f infra/docker/docker-compose.ubuntu-dev.yml ps
```

## 13. Spring Boot API 실행

현재 API 스캐폴드는 이미 들어가 있으므로 바로 실행을 시도할 수 있습니다.

```bash
cd /srv/my-forever-music/services/api
./gradlew bootRun
```

첫 실행 전 확인할 것:

- PostgreSQL 컨테이너가 올라와 있는지
- `application.yml`의 기본 DB 접속 정보와 실제 컨테이너 환경이 맞는지
- Flyway migration 파일은 아직 비어 있으므로 도메인 구현 전에 추가해야 함

헬스체크:

```bash
curl http://127.0.0.1:8080/actuator/health
curl http://127.0.0.1:8080/api/v1/system/info
```

## 14. 웹앱 실행

```bash
cd /srv/my-forever-music/apps/web
pnpm install
pnpm dev -- --host 0.0.0.0 --port 5173
```

직접 확인:

```bash
curl -I http://127.0.0.1:5173
```

## 15. AI 서비스 실행

현재 `services/ai`에는 최소 FastAPI 스캐폴드가 들어가 있습니다.

포함된 기본 엔드포인트:

- `/`
- `/health`
- `/docs`
- `/openapi.json`

직접 포트로 실행:

```bash
cd /srv/my-forever-music/services/ai
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements-dev.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Nginx에서 `/ai/` prefix로 공개할 때는 `AI_ROOT_PATH=/ai`를 같이 주는 것을 권장합니다.

```bash
cd /srv/my-forever-music/services/ai
source .venv/bin/activate
AI_ROOT_PATH=/ai uvicorn app.main:app --host 0.0.0.0 --port 8000
```

직접 확인:

```bash
curl http://127.0.0.1:8000/health
curl -I http://127.0.0.1:8000/docs
```

## 16. Nginx 개발용 프록시 연결

Ubuntu 서버에서 호스트 프로세스로 `web`, `api`, `ai`를 띄우는 개발 환경에는 아래 설정 파일을 사용합니다.

- [ubuntu.server.dev.conf](/Users/woosungjo/music-space/my-forever-music/infra/nginx/ubuntu.server.dev.conf)

적용:

```bash
cd /srv/my-forever-music
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.bak.$(date +%F-%H%M%S)
sudo cp infra/nginx/ubuntu.server.dev.conf /etc/nginx/nginx.conf
sudo nginx -t
sudo systemctl restart nginx
sudo systemctl status nginx --no-pager
```

## 17. 최종 확인

브라우저 또는 curl 기준으로 아래를 확인합니다.

```bash
curl http://127.0.0.1/
curl http://127.0.0.1/api/v1/system/info
curl http://127.0.0.1/actuator/health
curl http://127.0.0.1/ai/health
curl -X POST http://127.0.0.1/ai/v1/recommendations/preview -H 'Content-Type: application/json' -d '{"mode":"discovery","limit":2}'
curl -X POST http://127.0.0.1/api/v1/gms/recommendations/preview -H 'Content-Type: application/json' -d '{"mood":"upbeat","limit":2,"seed_track_ids":["track-alpha"]}'
curl -I http://127.0.0.1/docs
curl -I http://127.0.0.1/ai/docs
```

브라우저에서는 아래 순서로 체크하면 됩니다.

1. `http://SERVER_IP/`
2. `http://SERVER_IP/api/v1/system/info`
3. `http://SERVER_IP/actuator/health`
4. `http://SERVER_IP/ai/health`
5. `POST http://SERVER_IP/ai/v1/recommendations/preview`
6. `POST http://SERVER_IP/api/v1/gms/recommendations/preview`
7. `http://SERVER_IP/docs`
8. `http://SERVER_IP/ai/docs`

## 18. 현재 시점의 한계

- `services/api`에는 `gradle-wrapper.jar`가 아직 커밋되어 있지 않음
- `services/ai`에는 preview 추천 엔드포인트가 있으나 실제 모델 기반 ranking은 아직 없음
- `apps/web`는 아직 음악 서비스 전용 화면 구조로 정리되지 않음
- Flyway migration이 비어 있음

즉, 이 문서는 "Ubuntu 서버에 개발 환경을 올리고 접근 가능한 상태까지"를 목표로 하고, 실제 제품 기능 완성은 그 다음 단계입니다.

## 19. 다음 추천 작업

1. Ubuntu 서버에서 `./gradlew wrapper` 실행 후 wrapper 자산 고정
2. `services/ai` preview API를 실제 모델/카탈로그 로직과 연결
3. Flyway `V1__init.sql` 초안 작성
4. `apps/web` 라우트 정리
5. Nginx HTTPS/도메인 설정 추가

## 20. 공식 참고

- Docker Engine on Ubuntu: `https://docs.docker.com/engine/install/ubuntu/`
- Nginx Ubuntu packages: `https://nginx.org/en/linux_packages.html`
- Temurin Linux install: `https://adoptium.net/installation/linux/`
- Gradle install guide: `https://docs.gradle.org/current/userguide/installation.html`
- Node.js v24.14.1 archive: `https://nodejs.org/en/download/archive/v24.14.1`
- Node.js Corepack docs: `https://nodejs.org/download/release/v22.13.1/docs/api/corepack.html`
