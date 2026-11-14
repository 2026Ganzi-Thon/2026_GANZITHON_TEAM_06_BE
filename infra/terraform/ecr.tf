# 앱 이미지 저장소. GitHub Actions가 push, EC2가 pull 합니다.
resource "aws_ecr_repository" "app" {
  name                 = local.ecr_repository_name
  image_tag_mutability = "MUTABLE" # latest 태그를 배포마다 덮어씀
  force_delete         = true      # destroy 때 이미지가 남아 있어도 삭제

  image_scanning_configuration {
    scan_on_push = true
  }
}

# 무료 저장 용량을 넘지 않게 최근 이미지 10개만 유지
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}
