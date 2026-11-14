#!/usr/bin/env bash
# EC2(Ubuntu 24.04) 첫 세팅. Terraform이 user_data로 넣어서 첫 부팅 때 root로 한 번 자동 실행됩니다.
# 진행 확인: sudo cloud-init status --wait   /   로그: /var/log/cloud-init-output.log
# 직접 실행할 때: sudo bash setup-ec2.sh
set -euo pipefail
REGION=ap-northeast-2
export HOME="${HOME:-/root}"            # cloud-init에서는 HOME이 비어 있을 수 있음 (CodeDeploy 설치 스크립트가 사용)
export DEBIAN_FRONTEND=noninteractive  # apt가 질문 창을 띄우지 않게

# 첫 부팅 때 unattended-upgrades가 apt 잠금을 잡고 있을 수 있어 최대 5분 기다립니다.
# 설정 파일로도 넣어서 get.docker.com 스크립트 안의 apt 호출에도 적용되게 합니다.
echo 'DPkg::Lock::Timeout "300";' > /etc/apt/apt.conf.d/99station-lock-timeout
APT="apt-get -o DPkg::Lock::Timeout=300 -y"

$APT update
$APT install ruby-full wget unzip curl

# 1) Docker
curl -fsSL https://get.docker.com | sh
usermod -aG docker ubuntu

# 2) AWS CLI v2 (ECR 로그인용)
curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip
unzip -q -o /tmp/awscliv2.zip -d /tmp
/tmp/aws/install --update

# 3) CodeDeploy 에이전트
cd /tmp
wget -q "https://aws-codedeploy-${REGION}.s3.${REGION}.amazonaws.com/latest/install" -O ./install
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
