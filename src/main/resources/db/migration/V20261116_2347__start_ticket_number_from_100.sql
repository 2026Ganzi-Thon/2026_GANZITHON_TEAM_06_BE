-- 실제 승차권 번호를 100번부터 발급합니다 (RM-05). 1~99는 가상 승객(room VirtualPassenger) 번호입니다.
-- greatest: 이미 99 넘게 발급된 방은 그대로 둡니다.
update room_ticket_counters
set last_number = greatest(last_number, 99),
    updated_at  = now();
