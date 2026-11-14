# Terraform: 배포 인프라

`docs/INFRA.md`의 AWS 리소스를 코드로 만듭니다. state는 이 폴더의 로컬 파일(`terraform.tfstate`)에 저장하고 **커밋하지 않습니다.**

## 만드는 것

| 파일 | 리소스 |
|---|---|
| `ecr.tf` | ECR `station-be` (비공개, push 시 스캔, 최근 10개만 유지) |
| `s3.tf` | 배포 번들 버킷 `station-deploy-<계정ID>` (퍼블릭 차단, 암호화, `station/` 14일 후 삭제) |
| `iam.tf` | GitHub OIDC 공급자, `station-github-deploy`, `station-ec2-role`(+ 인스턴스 프로파일), `station-codedeploy-role` |
| `network.tf` | 기본 VPC, 보안 그룹 (인바운드 80/tcp, 443/tcp, 443/udp만) |
| `ec2.tf` | EC2 `station-dev` (Ubuntu 24.04, gp3 20GB 암호화, IMDSv2, 키 페어 없음), 탄력적 IP |
| `codedeploy.tf` | 애플리케이션 `station`, 배포 그룹 `station-dev` (In-place, AllAtOnce, 실패 시 롤백) |
| `budget.tf` | 월 예산 알림 (50/80/100%, 이메일) |

**만들지 않는 것**: 도메인 DNS 레코드(외부 서비스), 서버 `.env`(DB 비밀번호, JWT 키). 비밀값이 state 파일에 남지 않게 Terraform 밖에서 다룹니다.

버전: Terraform `>= 1.17.2`, AWS provider `6.73.0` (`.terraform.lock.hcl`에 고정, 커밋 대상)

## 사전 준비

- [Terraform](https://developer.hashicorp.com/terraform/install) 1.17.2 이상
- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) **2.32 이상** (`aws login` 지원)
- (선택) [Session Manager 플러그인](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html): 터미널에서 `aws ssm start-session`으로 접속할 때만 필요. 콘솔로 접속하면 필요 없음

```powershell
terraform version
aws --version
```

## 자격 증명

액세스 키는 만들지 않습니다. 브라우저로 로그인합니다.

```powershell
aws login --region ap-northeast-2
aws sts get-caller-identity          # 계정 ID가 나오면 성공
```

Terraform이 자격 증명을 못 읽으면(`No valid credential sources found` 등) 같은 PowerShell 창에서 임시 자격 증명을 환경변수로 넣고 다시 실행합니다.

```powershell
aws configure export-credentials --format powershell | Invoke-Expression
```

임시 자격 증명은 만료되므로, 시간이 지나 오류가 나면 `aws login`부터 다시 합니다.

## 적용 순서

```powershell
cd infra/terraform
Copy-Item terraform.tfvars.example terraform.tfvars   # budget_email 등 채우기
terraform init
terraform plan        # 만들어질 리소스 확인
terraform apply       # 'yes' 입력
terraform output      # 결과 확인
```

- 계정에 GitHub OIDC 공급자가 이미 있으면 apply가 `EntityAlreadyExists`로 실패합니다. `terraform.tfvars`에 `create_github_oidc_provider = false`를 넣고 다시 apply하세요.
- 예산 알림을 처음 만들면 이메일 확인 메일은 오지 않고, 기준을 넘을 때 바로 알림이 옵니다.
- apply가 끝나도 EC2 안에서 `deploy/setup-ec2.sh`(user_data)가 몇 분 더 돕니다. 서버에서 `sudo cloud-init status --wait`가 `done`이 될 때까지 기다리세요.

## 출력값을 어디에 넣나요

| 출력 | 넣는 곳 |
|---|---|
| `github_actions_role_arn` | GitHub → Settings → Secrets and variables → Actions → `AWS_ROLE_ARN` |
| `deploy_bucket_name` | 같은 곳 → `DEPLOY_BUCKET` |
| `elastic_ip` | 도메인 DNS의 **A 레코드** 값 (예: `api.도메인 → 이 IP`) |
| `ssm_session_command` | 서버 접속 명령어. 접속해서 `/home/ubuntu/station/.env` 작성 |
| `ecr_repository_url`, `instance_id` | 확인용 |

```powershell
terraform output -raw github_actions_role_arn
terraform output -raw deploy_bucket_name
terraform output -raw elastic_ip
```

## 서버 크기 바꾸기

```powershell
# terraform.tfvars
instance_type = "c7i-flex.large"
```

```powershell
terraform plan    # aws_instance.app 이 "update in-place" 1개인지 확인 (replace가 보이면 apply하지 말 것)
terraform apply
```

인스턴스가 **중지 → 유형 변경 → 시작**됩니다 (몇 분간 서버 중단). 디스크, DB 볼륨, 탄력적 IP는 그대로입니다.
교체되지 않도록 `ami`, `user_data`, `subnet_id`는 `ignore_changes`로 막아 두었고, 처음 만들 때 지금 유형과 `c7i-flex.large`를 **둘 다** 지원하는 가용 영역을 고릅니다.
이후 서버 `.env`의 메모리 값 조정은 `docs/INFRA.md`의 "서버 크기 바꾸기"를 보세요.

## 정리 (destroy)

```powershell
terraform destroy   # 'yes' 입력
```

EC2, 탄력적 IP, ECR(이미지 포함), S3(번들 포함), IAM, CodeDeploy, 예산 알림이 모두 삭제됩니다. **서버의 DB 데이터도 함께 사라집니다.**
DNS A 레코드는 Terraform이 만들지 않았으므로 도메인 서비스에서 직접 지웁니다.
