-- 읽씹 방 상황 칩 문구를 와이어프레임 S06 확정본으로 맞춥니다 (SF-02). V2는 이미 적용돼서 고치지 않고 여기서 UPDATE합니다.
-- id는 그대로라 승차권의 마지막 상황 칩(last_situation_chip_id) 연결은 유지됩니다.
-- 뜻이 바뀐 2개는 code도 함께 바꿉니다: WAITING_CONTACT → OPEN_CLOSE_CHAT, ONE_LINE → CHECK_SENT_TIME
update situation_chips set label = '답장을 기다리는 중', updated_at = now()
where code = 'NO_REPLY_WAITING_REPLY';

update situation_chips set label = '보낸 메시지에 소식이 없는 중', updated_at = now()
where code = 'NO_REPLY_QUIET_MESSAGE';

update situation_chips set code = 'NO_REPLY_OPEN_CLOSE_CHAT', label = '대화창을 열었다 닫았다 하는 중', updated_at = now()
where code = 'NO_REPLY_WAITING_CONTACT';

update situation_chips set code = 'NO_REPLY_CHECK_SENT_TIME', label = '보낸 시간을 자꾸 다시 보는 중', updated_at = now()
where code = 'NO_REPLY_ONE_LINE';
