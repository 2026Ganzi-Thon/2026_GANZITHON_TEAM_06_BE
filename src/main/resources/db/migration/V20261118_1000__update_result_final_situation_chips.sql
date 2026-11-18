-- 결과 발표 방·간지톤 결선 방 상황 칩 문구를 기획 확정본(11/18)으로 맞춥니다 (SF-02). V2는 이미 적용돼서 여기서 UPDATE합니다.
-- 코드와 id는 그대로라 승차권의 마지막 상황 칩 연결은 유지됩니다. 결과 발표 방은 '합격 발표'와 '발표를 앞둔 아침'의 순서도 바꿉니다.
update situation_chips set label = '면접 결과를 기다리는 중', updated_at = now() where code = 'RESULT_AFTER_INTERVIEW';
update situation_chips set label = '서류 결과를 기다리는 중', updated_at = now() where code = 'RESULT_DOCUMENT';
update situation_chips set label = '시험 결과를 기다리는 중', updated_at = now() where code = 'RESULT_EXAM';
update situation_chips set label = '합격 발표를 기다리는 중', sort_order = 4, updated_at = now() where code = 'RESULT_ACCEPTANCE';
update situation_chips set label = '발표를 앞둔 아침', sort_order = 5, updated_at = now() where code = 'RESULT_ANNOUNCE_MORNING';
update situation_chips set label = '피드백을 기다리는 중', updated_at = now() where code = 'RESULT_FEEDBACK';
update situation_chips set label = '회신을 기다리는 중', updated_at = now() where code = 'RESULT_REPLY';

update situation_chips set label = '발표를 마치고 결과를 기다리는 중', updated_at = now() where code = 'FINAL_AFTER_PRESENTATION';
update situation_chips set label = '심사 결과를 기다리는 중', updated_at = now() where code = 'FINAL_JUDGING';
update situation_chips set label = '밤샘 작업을 마치고 기다리는 중', updated_at = now() where code = 'FINAL_ALL_NIGHTER';
update situation_chips set label = '데모를 마치고 기다리는 중', updated_at = now() where code = 'FINAL_AFTER_DEMO';
update situation_chips set label = '마지막 커밋을 마치고 기다리는 중', updated_at = now() where code = 'FINAL_LAST_COMMIT';
