#!/usr/bin/env bash
# CodeDeploy AfterInstall: 새 이미지를 받아 컨테이너를 다시 띄웁니다.
set -euo pipefail
cd /home/ubuntu/station/current

# 서버 비밀값(.env) + 이번 배포의 이미지 주소(image.env)
set -a
. /home/ubuntu/station/.env
. ./image.env
set +a

# ECR 로그인 (EC2 인스턴스 역할 권한 사용, 키 없음)
REGISTRY="${IMAGE_URI%%/*}"
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin "$REGISTRY"

docker compose pull app
docker compose up -d
docker image prune -f
