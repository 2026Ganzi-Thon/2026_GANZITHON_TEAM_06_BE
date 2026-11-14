terraform {
  # 작성 시점(2026-11) 안정 최신: Terraform 1.17.2, AWS provider 6.73.0
  # 정확한 provider 버전은 .terraform.lock.hcl에 고정되어 있습니다 (커밋 대상).
  required_version = ">= 1.17.0, < 2.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.73"
    }
  }

  # state는 로컬 파일(terraform.tfstate)에 둡니다. 커밋하지 않습니다 (.gitignore).
}
