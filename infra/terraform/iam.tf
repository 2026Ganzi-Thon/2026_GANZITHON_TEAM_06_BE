############################################
# GitHub OIDC 공급자
############################################
locals {
  github_oidc_url = "https://token.actions.githubusercontent.com"
  github_oidc_provider_arn = (
    var.create_github_oidc_provider
    ? aws_iam_openid_connect_provider.github[0].arn
    : data.aws_iam_openid_connect_provider.github[0].arn
  )
}

resource "aws_iam_openid_connect_provider" "github" {
  count = var.create_github_oidc_provider ? 1 : 0

  url            = local.github_oidc_url
  client_id_list = ["sts.amazonaws.com"]
}

# 계정에 이미 있으면 (create_github_oidc_provider = false) 조회해서 씁니다.
data "aws_iam_openid_connect_provider" "github" {
  count = var.create_github_oidc_provider ? 0 : 1

  url = local.github_oidc_url
}

############################################
# GitHub Actions 역할 (station-github-deploy)
############################################
data "aws_iam_policy_document" "github_trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [local.github_oidc_provider_arn]
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
  name                 = "${var.project}-github-deploy"
  description          = "GitHub Actions deploy (${var.github_org}/${var.github_repo}@${var.github_branch})"
  assume_role_policy   = data.aws_iam_policy_document.github_trust.json
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
  name   = "deploy"
  role   = aws_iam_role.github_deploy.id
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
