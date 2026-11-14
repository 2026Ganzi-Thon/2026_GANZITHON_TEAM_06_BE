# 기본 VPC를 씁니다 (VPC를 따로 만들지 않음).
data "aws_vpc" "default" {
  default = true
}

# 지금 유형과 시연용 유형(c7i-flex.large)을 모두 지원하는 가용 영역을 고릅니다.
# 그래야 나중에 유형만 바꿔 apply할 때 "이 AZ에서 지원하지 않는 유형" 오류가 나지 않습니다.
locals {
  resize_target_type = "c7i-flex.large"
  az_check_types     = distinct([var.instance_type, local.resize_target_type])

  offered_azs = {
    for t in local.az_check_types : t => toset([
      for i, az in data.aws_ec2_instance_type_offerings.az.locations : az
      if data.aws_ec2_instance_type_offerings.az.instance_types[i] == t
    ])
  }
  usable_azs = sort(tolist(setintersection(values(local.offered_azs)...)))
}

data "aws_ec2_instance_type_offerings" "az" {
  location_type = "availability-zone"

  filter {
    name   = "instance-type"
    values = local.az_check_types
  }
}

data "aws_subnet" "app" {
  vpc_id            = data.aws_vpc.default.id
  availability_zone = try(local.usable_azs[0], null)
  default_for_az    = true

  lifecycle {
    precondition {
      condition     = length(local.usable_azs) > 0
      error_message = "instance_type과 c7i-flex.large를 모두 지원하는 가용 영역이 이 리전에 없습니다."
    }
  }
}

resource "aws_security_group" "app" {
  name        = "${var.project}-dev-web"
  description = "HTTP/HTTPS only. SSH, DB and app ports stay closed (use Session Manager)."
  vpc_id      = data.aws_vpc.default.id
}

resource "aws_vpc_security_group_ingress_rule" "http" {
  security_group_id = aws_security_group.app.id
  description       = "HTTP - certificate challenge, redirect to HTTPS"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
}

resource "aws_vpc_security_group_ingress_rule" "https" {
  security_group_id = aws_security_group.app.id
  description       = "HTTPS, WSS"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}

# Caddy가 HTTP/3(QUIC)를 443/udp로 받습니다 (deploy/docker-compose.yml의 443:443/udp).
# 막혀 있어도 브라우저가 TCP로 돌아가므로 동작에는 문제가 없습니다.
resource "aws_vpc_security_group_ingress_rule" "https_udp" {
  security_group_id = aws_security_group.app.id
  description       = "HTTP/3 (QUIC)"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "udp"
  from_port         = 443
  to_port           = 443
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.app.id
  description       = "All outbound"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}
