-- 털실 톡톡 도안을 디자인 확정본(디자인 V0.2, MVP 9개)으로 바꿉니다 (YT-01). V2는 이미 적용돼서 여기서 UPDATE·INSERT합니다.
-- 임시 도안 3개는 승차권·담요 조각이 id로 참조하고 있어서 지우지 않고 코드와 이름만 바꿉니다.
-- 프론트는 이 코드 하나로 기본 격자 도안·털실 톡톡 완성 이미지·담요 조각 이미지 3종을 연결합니다.
update patterns set code = 'PATTERN_HEART', name = '하트', updated_at = now() where code = 'PATTERN_01';
update patterns set code = 'PATTERN_CLOUD', name = '구름', updated_at = now() where code = 'PATTERN_02';
update patterns set code = 'PATTERN_STAR',  name = '별',   updated_at = now() where code = 'PATTERN_03';

insert into patterns (code, name)
values ('PATTERN_MOUNTAIN', '산'),
       ('PATTERN_SPROUT', '새싹'),
       ('PATTERN_FOREST', '숲'),
       ('PATTERN_TRAIN', '열차'),
       ('PATTERN_MOON', '달'),
       ('PATTERN_FLOWER', '꽃');
