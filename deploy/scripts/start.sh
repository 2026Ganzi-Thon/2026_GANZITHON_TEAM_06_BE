#!/usr/bin/env bash
# CodeDeploy AfterInstall: 새 이미지를 받아 컨테이너를 다시 띄웁니다.
# 서버 .env를 고친 뒤 반영할 때도 이 스크립트를 다시 실행합니다:
#   sudo bash /home/ubuntu/station/current/scripts/start.sh
set -euo pipefail
STATION_HOME=/home/ubuntu/station
cd "$STATION_HOME/current"

# current/.env = 서버 비밀값(../.env) + 이번 배포의 이미지 주소(image.env)
# docker compose는 실행 폴더의 .env를 자동으로 읽으므로, 이후 current/ 안에서
# docker compose ps / logs / exec 를 별도 준비 없이 바로 쓸 수 있습니다.
# 비밀값이 들어 있으므로 root 소유, 권한 600으로 만듭니다.
umask 077
{
  tr -d '\r' < "$STATION_HOME/.env"
  echo
  tr -d '\r' < ./image.env
} > .env.tmp
chown root:root .env.tmp
chmod 600 .env.tmp
mv -f .env.tmp .env

# .env를 셸로 source 하지 않습니다 (JAVA_OPTS처럼 공백이 있는 값이 셸 명령으로 해석될 수 있음)
IMAGE_URI="$(grep -E '^IMAGE_URI=' ./image.env | tail -n 1 | cut -d= -f2- | tr -d '\r')"
if [ -z "$IMAGE_URI" ]; then
  echo "image.env에 IMAGE_URI가 없습니다" >&2
  exit 1
fi

# ECR 로그인 (EC2 인스턴스 역할 권한 사용, 키 없음)
# 리전은 하드코딩하지 않고 레지스트리 주소(<계정>.dkr.ecr.<리전>.amazonaws.com)에서 꺼냅니다.
REGISTRY="${IMAGE_URI%%/*}"
REGION="$(echo "$REGISTRY" | sed -nE 's#^[0-9]+\.dkr\.ecr\.([a-z0-9-]+)\.amazonaws\.com$#\1#p')"
if [ -z "$REGION" ]; then
  echo "IMAGE_URI에서 리전을 읽지 못했습니다: $IMAGE_URI" >&2
  exit 1
fi
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"

docker compose pull app
docker compose up -d
docker image prune -f
