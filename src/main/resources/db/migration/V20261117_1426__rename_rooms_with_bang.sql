-- 대기실 이름을 와이어프레임 표기로 맞춥니다 (프론트 확인 11/17: 서버 이름에 "방"을 붙이기로).
-- 기다림 이름(상황 칩 없이 끝난 승차권)에도 이 이름이 쓰입니다.
update rooms set name = '결과 발표 방', updated_at = now() where code = 'RESULT';
update rooms set name = '읽씹 방', updated_at = now() where code = 'NO_REPLY';
update rooms set name = '간지톤 결선 방', updated_at = now() where code = 'GANZITHON_FINAL';
