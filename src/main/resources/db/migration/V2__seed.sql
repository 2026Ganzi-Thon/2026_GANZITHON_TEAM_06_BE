-- 시드 데이터: 대기실, 상황 칩, 도안(임시), 승차권 번호 카운터
-- 칩 문구는 발표 전 어휘 검수(SF-02)에서 바뀔 수 있습니다. 문구는 여기서만 관리하고 코드에 쓰지 않습니다.
-- (바꿀 때는 이 파일을 고치지 말고 새 마이그레이션 파일에 UPDATE를 씁니다)

-- 대기실 (회고 질문, NPC 공감 문구는 미정이라 null)
insert into rooms (code, name, slogan, sort_order)
values ('RESULT', '결과발표', '결과를 기다리는 자리, 잠시 마음을 가만히 내려놓아도 돼요.', 1),
       ('NO_REPLY', '읽씹', '서두르지 않아도 괜찮아요. 여기서는 마음의 속도를 낮춰도 돼요.', 2),
       ('GANZITHON_FINAL', '간지톤 결선', '밤을 지새운 모든 열정이 잠시 쉬어가는 대합실이에요', 3);

-- 상황 칩 (방별, 나열 순서 = sort_order)
insert into situation_chips (room_id, code, label, sort_order)
select r.id, c.code, c.label, c.sort_order
from (values ('RESULT', 'RESULT_AFTER_INTERVIEW', '면접 보고 기다리는 중', 1),
             ('RESULT', 'RESULT_DOCUMENT', '서류 결과 기다리는 중', 2),
             ('RESULT', 'RESULT_EXAM', '시험 결과 기다리는 중', 3),
             ('RESULT', 'RESULT_ANNOUNCE_MORNING', '발표일 아침', 4),
             ('RESULT', 'RESULT_ACCEPTANCE', '합격 발표 기다리는 중', 5),
             ('RESULT', 'RESULT_FEEDBACK', '피드백 기다리는 중', 6),
             ('RESULT', 'RESULT_REPLY', '회신 기다리는 중', 7),

             ('NO_REPLY', 'NO_REPLY_READ_LONG_AGO', '읽음 표시 뜬 지 한참째', 1),
             ('NO_REPLY', 'NO_REPLY_WAITING_REPLY', '답장 기다리는 중', 2),
             ('NO_REPLY', 'NO_REPLY_QUIET_MESSAGE', '보낸 메시지가 조용한 중', 3),
             ('NO_REPLY', 'NO_REPLY_WAITING_CONTACT', '연락 오기를 기다리는 중', 4),
             ('NO_REPLY', 'NO_REPLY_UNREAD_ONE', '아직 1이 안 사라진 중', 5),
             ('NO_REPLY', 'NO_REPLY_ONE_LINE', '답장 한 줄을 기다리는 중', 6),

             ('GANZITHON_FINAL', 'FINAL_AFTER_PRESENTATION', '발표 마치고 기다리는 중', 1),
             ('GANZITHON_FINAL', 'FINAL_JUDGING', '심사 결과 기다리는 중', 2),
             ('GANZITHON_FINAL', 'FINAL_ALL_NIGHTER', '밤새운 뒤 기다리는 중', 3),
             ('GANZITHON_FINAL', 'FINAL_AFTER_DEMO', '데모 끝난 뒤 앉아 있는 중', 4),
             ('GANZITHON_FINAL', 'FINAL_LAST_COMMIT', '마지막 커밋 뒤 기다리는 중', 5))
         as c(room_code, code, label, sort_order)
         join rooms r on r.code = c.room_code;

-- 도안 (임시 3개. 프론트·디자인과 코드 확정 후 교체)
insert into patterns (code, name)
values ('PATTERN_01', '임시 도안 1'),
       ('PATTERN_02', '임시 도안 2'),
       ('PATTERN_03', '임시 도안 3');

-- 승차권 번호 카운터 (대기실마다 0에서 시작)
insert into room_ticket_counters (room_id, last_number)
select id, 0
from rooms;
