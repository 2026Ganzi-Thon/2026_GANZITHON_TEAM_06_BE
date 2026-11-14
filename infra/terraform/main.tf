locals {
  account_id = data.aws_caller_identity.current.account_id
  partition  = data.aws_partition.current.partition

  # 이름은 배포 파일(deploy.yml, appspec, 문서)과 같아야 합니다.
  ecr_repository_name   = "${var.project}-be"  # station-be
  codedeploy_app_name   = var.project          # station
  codedeploy_group_name = "${var.project}-dev" # station-dev
  instance_name         = "${var.project}-dev" # station-dev (CodeDeploy가 Name 태그로 찾음)
  bundle_prefix         = "${var.project}/"    # station/ (deploy.yml이 올리는 경로)
  deploy_bucket_name    = coalesce(var.deploy_bucket_name, "${var.project}-deploy-${local.account_id}")
}
