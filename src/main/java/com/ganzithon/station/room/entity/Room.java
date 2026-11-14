package com.ganzithon.station.room.entity;

import com.ganzithon.station.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대기실. 시드 데이터로만 들어가고 사용자가 바꾸지 않습니다 (V2__seed.sql).
 */
@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 100)
    private String slogan;

    @Column(name = "retrospect_question_1", length = 100)
    private String retrospectQuestion1;

    @Column(name = "retrospect_question_2", length = 100)
    private String retrospectQuestion2;

    @Column(length = 100)
    private String npcEmpathyCopy;

    @Column(nullable = false)
    private int sortOrder;

    @Builder
    private Room(String code, String name, String slogan, String retrospectQuestion1,
                 String retrospectQuestion2, String npcEmpathyCopy, int sortOrder) {
        this.code = code;
        this.name = name;
        this.slogan = slogan;
        this.retrospectQuestion1 = retrospectQuestion1;
        this.retrospectQuestion2 = retrospectQuestion2;
        this.npcEmpathyCopy = npcEmpathyCopy;
        this.sortOrder = sortOrder;
    }
}
