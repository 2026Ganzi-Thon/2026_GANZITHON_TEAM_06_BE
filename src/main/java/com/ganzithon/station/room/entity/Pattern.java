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
 * 도안 (담요 조각의 무늬). code는 프론트가 그림을 찾는 키입니다. 크기·칸 수·그림은 프론트가 관리합니다.
 */
@Entity
@Table(name = "patterns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pattern extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 30)
    private String name;

    @Builder
    private Pattern(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
