package com.ganzithon.station.room.entity;

import com.ganzithon.station.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상황 칩. 방마다 다르고 시드 데이터로만 들어갑니다.
 * 문구(label)는 어휘 검수(SF-02)로 바뀔 수 있으니, 코드에서는 문구 대신 code로 비교합니다.
 */
@Entity
@Table(name = "situation_chips")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SituationChip extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(nullable = false)
    private int sortOrder;

    @Builder
    private SituationChip(Room room, String code, String label, int sortOrder) {
        this.room = room;
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
    }
}
