#!/usr/bin/env bash
# CodeDeploy ValidateService: 앱이 UP이 될 때까지 기다립니다. 실패하면 배포 실패로 표시됩니다.
set -uo pipefail
set -a; . /home/ubuntu/station/.env; set +a

for i in $(seq 1 30); do
  if curl -fsS --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}/actuator/health" | grep -q UP; then
    echo "health UP"
    exit 0
  fi
  sleep 5
done

echo "health check 실패"
cd /home/ubuntu/station/current && docker compose logs --tail 100 app
exit 1
