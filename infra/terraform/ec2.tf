# Ubuntu 24.04 LTS amd64 최신 (Canonical 공식 계정)
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "app" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  subnet_id              = data.aws_subnet.app.id
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name
  # 키 페어 없음. 접속은 Session Manager로만 합니다.

  # 첫 부팅 때 한 번 실행 (Docker, AWS CLI, CodeDeploy 에이전트, 스왑, 시간대)
  # Windows에서 체크아웃하면 CRLF일 수 있어 LF로 바꿔서 넣습니다.
  user_data                   = replace(file("${path.module}/../../deploy/setup-ec2.sh"), "\r\n", "\n")
  user_data_replace_on_change = false

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required" # IMDSv2만 허용
    http_put_response_hop_limit = 1
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 20
    encrypted             = true
    delete_on_termination = true
  }

  tags = {
    Name = local.instance_name # CodeDeploy 배포 그룹이 이 태그로 서버를 찾습니다
  }

  lifecycle {
    # instance_type만 바꿔 apply하면 교체 없이 중지 → 변경 → 시작으로 끝나야 합니다.
    # - ami: 새 Ubuntu AMI가 나와도 교체하지 않음
    # - user_data: 첫 부팅 뒤에는 의미가 없으므로 setup-ec2.sh를 고쳐도 교체·재시작하지 않음
    # - subnet_id: 유형이 바뀌어 가용 영역 조회 결과가 달라져도 그대로 둠
    ignore_changes = [ami, user_data, subnet_id]
  }
}

# 고정 공인 IP. DNS A 레코드가 이 주소를 가리킵니다. 인스턴스를 중지·시작해도 바뀌지 않습니다.
resource "aws_eip" "app" {
  domain   = "vpc"
  instance = aws_instance.app.id

  tags = {
    Name = local.instance_name
  }
}
