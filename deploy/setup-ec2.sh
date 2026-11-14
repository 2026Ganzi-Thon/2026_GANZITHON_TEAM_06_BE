#!/usr/bin/env bash
# EC2(Ubuntu 24.04)에서 처음 한 번만 실행: sudo bash setup-ec2.sh
set -euo pipefail
REGION=ap-northeast-2

apt-get update
apt-get install -y ruby-full wget unzip curl

# 1) Docker
curl -fsSL https://get.docker.com | sh
usermod -aG docker ubuntu

# 2) AWS CLI v2 (ECR 로그인용)
curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip
unzip -q -o /tmp/awscliv2.zip -d /tmp
/tmp/aws/install --update

# 3) CodeDeploy 에이전트
cd /tmp
wget -q "https://aws-codedeploy-${REGION}.s3.${REGION}.amazonaws.com/latest/install"
chmod +x ./install
./install auto
systemctl enable --now codedeploy-agent

# 4) 스왑 2GB (메모리 부족으로 컨테이너가 죽는 것 방지)
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# 5) 배포 폴더, 시간대
mkdir -p /home/ubuntu/station
chown ubuntu:ubuntu /home/ubuntu/station
timedatectl set-timezone Asia/Seoul

systemctl status codedeploy-agent --no-pager | head -5
echo "완료. 다음: /home/ubuntu/station/.env 작성 (deploy/.env.example 참고)"
