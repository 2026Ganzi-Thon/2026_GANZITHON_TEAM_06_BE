# 월 예산 알림. 실제 사용액(ACTUAL)이 한도의 50/80/100%를 넘으면 이메일을 보냅니다.
# 알림만 보내고 리소스를 멈추지는 않습니다.
resource "aws_budgets_budget" "monthly" {
  name         = "${var.project}-monthly"
  budget_type  = "COST"
  limit_amount = format("%.2f", var.budget_limit_usd)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  # 크레딧으로 상쇄되기 전 금액 기준으로 봅니다 (크레딧을 포함하면 사용액이 0으로 보여 알림이 오지 않음).
  cost_types {
    include_credit = false
    include_refund = false
  }

  dynamic "notification" {
    for_each = [50, 80, 100]

    content {
      comparison_operator        = "GREATER_THAN"
      threshold                  = notification.value
      threshold_type             = "PERCENTAGE"
      notification_type          = "ACTUAL"
      subscriber_email_addresses = [var.budget_email]
    }
  }
}
