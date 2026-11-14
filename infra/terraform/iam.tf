############################################
# GitHub Actions 인증 방식 (var.auth_mode)
#   access_key (기본): IAM 사용자 station-github-deploy + 액세스 키(키는 Terraform 밖에서 발급)
#   oidc             : GitHub OIDC 공급자 + 역할 station-github-deploy (키 없음)
# 지금 계정은 조직 SCP가 iam:*OpenIDConnectProvider*를 막아서 access_key를 씁니다.
# 두 방식 모두 아래 같은 최소 권한 정책(github_deploy)을 붙입니다.
############################################
locals {
  use_oidc       = var.auth_mode == "oidc"
  use_access_key = var.auth_mode == "access_key"
}

############################################
# access_key 모드: IAM 사용자 (station-github-deploy)
############################################
resource "aws_iam_user" "github_deploy" {
  count = local.use_access_key ? 1 : 0

  name          = "${var.project}-github-deploy"
  force_destroy = true # destroy 때 사용자에 남은 액세스 키도 함께 삭제
}

resource "aws_iam_user_policy" "github_deploy" {
  count = local.use_access_key ? 1 : 0

  name   = "deploy"
  user   = aws_iam_user.github_deploy[0].name
  policy = data.aws_iam_policy_document.github_deploy.json
}

############################################
# oidc 모드: GitHub OIDC 공급자 + 역할 (station-github-deploy)
############################################
resource "aws_iam_openid_connect_provider" "github" {
  count = local.use_oidc ? 1 : 0

  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

data "aws_iam_policy_document" "github_trust" {
  count = local.use_oidc ? 1 : 0

  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github[0].arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # 이 리포지토리의 지정 브랜치(dev)에서 실행된 워크플로만 허용
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_org}/${var.github_repo}:ref:refs/heads/${var.github_branch}"]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  count = local.use_oidc ? 1 : 0

  name                 = "${var.project}-github-deploy"
  description          = "GitHub Actions deploy (${var.github_org}/${var.github_repo}@${var.github_branch})"
  assume_role_policy   = data.aws_iam_policy_document.github_trust[0].json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_deploy" {
  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"] # 리소스 단위로 제한할 수 없는 API
  }

  statement {
    sid = "EcrPush"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
      "ecr:PutImage",
    ]
    resources = [aws_ecr_repository.app.arn]
  }

  statement {
    sid       = "BundleUpload"
    actions   = ["s3:PutObject"]
    resources = ["${aws_s3_bucket.deploy.arn}/${local.bundle_prefix}*"]
  }

  statement {
    sid = "CodeDeployDeployment"
    actions = [
      "codedeploy:CreateDeployment",
      "codedeploy:GetDeployment",
    ]
    resources = [aws_codedeploy_deployment_group.dev.arn]
  }

  statement {
    sid = "CodeDeployRevision"
    actions = [
      "codedeploy:GetApplicationRevision",
      "codedeploy:RegisterApplicationRevision",
    ]
    resources = [aws_codedeploy_app.app.arn]
  }

  statement {
    sid       = "CodeDeployConfig"
    actions   = ["codedeploy:GetDeploymentConfig"]
    resources = ["arn:${local.partition}:codedeploy:${var.region}:${local.account_id}:deploymentconfig:*"]
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  count = local.use_oidc ? 1 : 0

  name   = "deploy"
  role   = aws_iam_role.github_deploy[0].id
  policy = data.aws_iam_policy_document.github_deploy.json
}

############################################
# EC2 역할 (station-ec2-role) + 인스턴스 프로파일
############################################
data "aws_iam_policy_document" "ec2_trust" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ec2" {
  name               = "${var.project}-ec2-role"
  description        = "EC2: ECR pull, Session Manager, deploy bundle read"
  assume_role_policy = data.aws_iam_policy_document.ec2_trust.json
}

resource "aws_iam_role_policy_attachment" "ec2_ecr_read" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:${local.partition}:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:${local.partition}:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "ec2_bundle_read" {
  statement {
    sid       = "ReadDeployBundle"
    actions   = ["s3:GetObject", "s3:GetObjectVersion"]
    resources = ["${aws_s3_bucket.deploy.arn}/${local.bundle_prefix}*"]
  }
}

resource "aws_iam_role_policy" "ec2_bundle_read" {
  name   = "deploy-bundle-read"
  role   = aws_iam_role.ec2.id
  policy = data.aws_iam_policy_document.ec2_bundle_read.json
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${var.project}-ec2-role"
  role = aws_iam_role.ec2.name
}

############################################
# CodeDeploy 서비스 역할 (station-codedeploy-role)
############################################
data "aws_iam_policy_document" "codedeploy_trust" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["codedeploy.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "codedeploy" {
  name               = "${var.project}-codedeploy-role"
  description        = "CodeDeploy service role"
  assume_role_policy = data.aws_iam_policy_document.codedeploy_trust.json
}

resource "aws_iam_role_policy_attachment" "codedeploy" {
  role       = aws_iam_role.codedeploy.name
  policy_arn = "arn:${local.partition}:iam::aws:policy/service-role/AWSCodeDeployRole"
}
