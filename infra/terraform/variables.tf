variable "region" {
  description = "AWS 리전. deploy.yml의 AWS_REGION과 같아야 합니다 (서버 스크립트는 리전을 IMDS와 이미지 주소에서 읽음)."
  type        = string
  default     = "ap-southeast-2"
}

variable "project" {
  description = "리소스 이름 접두사. ECR(station-be), CodeDeploy(station / station-dev), 역할 이름이 이 값으로 만들어지며 deploy.yml과 같아야 합니다."
  type        = string
  default     = "station"
}

variable "github_org" {
  description = "GitHub 조직(소유자) 이름"
  type        = string
  default     = "2026Ganzi-Thon"
}

variable "github_repo" {
  description = "GitHub 리포지토리 이름"
  type        = string
  default     = "2026_GANZITHON_TEAM_06_BE"
}

variable "github_branch" {
  description = "AWS 역할을 맡을 수 있는 브랜치 (이 브랜치의 워크플로만 배포 가능)"
  type        = string
  default     = "dev"
}

variable "instance_type" {
  description = "EC2 유형. 평소 t8i.small(2GB), 시연 전 c7i-flex.large(4GB). 바꿔서 apply하면 교체 없이 중지 → 변경 → 시작합니다."
  type        = string
  default     = "t8i.small"
}

variable "deploy_bucket_name" {
  description = "배포 번들 S3 버킷 이름. 비워 두면 \"<project>-deploy-<계정 ID>\""
  type        = string
  default     = null
}

variable "auth_mode" {
  description = "GitHub Actions의 AWS 로그인 방식. access_key: IAM 사용자(키는 apply 후 직접 발급), oidc: GitHub OIDC 역할(SCP가 OIDC 공급자를 허용할 때)"
  type        = string
  default     = "access_key"

  validation {
    condition     = contains(["access_key", "oidc"], var.auth_mode)
    error_message = "auth_mode는 \"access_key\" 또는 \"oidc\"여야 합니다."
  }
}

variable "budget_limit_usd" {
  description = "월 예산 한도(USD). 실제 사용액이 50/80/100%를 넘으면 이메일 알림"
  type        = number
  default     = 20
}

variable "budget_email" {
  description = "예산 알림을 받을 이메일"
  type        = string

  validation {
    condition     = can(regex("^[^@ ]+@[^@ ]+[.][^@ ]+$", var.budget_email))
    error_message = "budget_email은 이메일 주소여야 합니다."
  }
}
