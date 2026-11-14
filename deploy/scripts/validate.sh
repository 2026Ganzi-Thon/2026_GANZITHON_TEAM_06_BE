#!/usr/bin/env bash
# CodeDeploy ValidateService: 앱이 UP이 될 때까지 기다립니다. 실패하면 배포 실패로 표시됩니다.
set -uo pipefail
cd /home/ubuntu/station/current

# start.sh가 만든 current/.env (서버 .env + image.env)에서 DOMAIN만 읽습니다
DOMAIN="$(grep -E '^DOMAIN=' ./.env | tail -n 1 | cut -d= -f2- | tr -d "\"'\r")"
if [ -z "$DOMAIN" ]; then
  echo "서버 .env에 DOMAIN이 없습니다" >&2
  exit 1
fi

for i in $(seq 1 30); do
  if curl -fsS --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}/actuator/health" | grep -q UP; then
    echo "health UP"
    exit 0
  fi
  sleep 5
done

echo "health check 실패"
docker compose ps
docker compose logs --tail 100 app caddy
exit 1
