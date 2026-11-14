output "elastic_ip" {
  description = "서버 고정 IP → DNS A 레코드 값"
  value       = aws_eip.app.public_ip
}

output "github_actions_role_arn" {
  description = "GitHub Secret AWS_ROLE_ARN 값"
  value       = aws_iam_role.github_deploy.arn
}

output "deploy_bucket_name" {
  description = "GitHub Secret DEPLOY_BUCKET 값"
  value       = aws_s3_bucket.deploy.bucket
}

output "ecr_repository_url" {
  description = "ECR 리포지토리 주소 (deploy.yml이 이 리포지토리에 push)"
  value       = aws_ecr_repository.app.repository_url
}

output "instance_id" {
  description = "EC2 인스턴스 ID"
  value       = aws_instance.app.id
}

output "instance_availability_zone" {
  description = "EC2가 있는 가용 영역"
  value       = aws_instance.app.availability_zone
}

output "ssm_session_command" {
  description = "Session Manager 접속 명령어 (AWS CLI + Session Manager 플러그인 필요. 콘솔 EC2 → 연결 → Session Manager도 가능)"
  value       = "aws ssm start-session --target ${aws_instance.app.id} --region ${var.region}"
}
