resource "aws_codedeploy_app" "app" {
  name             = local.codedeploy_app_name # station
  compute_platform = "Server"                  # EC2/온프레미스
}

resource "aws_codedeploy_deployment_group" "dev" {
  app_name               = aws_codedeploy_app.app.name
  deployment_group_name  = local.codedeploy_group_name # station-dev
  service_role_arn       = aws_iam_role.codedeploy.arn
  deployment_config_name = "CodeDeployDefault.AllAtOnce"

  # 현재 위치(In-place), 로드 밸런서 없음
  deployment_style {
    deployment_type   = "IN_PLACE"
    deployment_option = "WITHOUT_TRAFFIC_CONTROL"
  }

  # Name = station-dev 태그가 붙은 EC2에 배포
  ec2_tag_set {
    ec2_tag_filter {
      key   = "Name"
      type  = "KEY_AND_VALUE"
      value = local.instance_name
    }
  }

  # 배포가 실패하면 마지막 성공 버전으로 자동 롤백
  auto_rollback_configuration {
    enabled = true
    events  = ["DEPLOYMENT_FAILURE"]
  }

  # 서비스 역할 권한이 붙은 뒤에 만들어야 생성 검증을 통과합니다.
  depends_on = [aws_iam_role_policy_attachment.codedeploy]
}
