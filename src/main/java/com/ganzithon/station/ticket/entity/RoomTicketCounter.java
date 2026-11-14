package com.ganzithon.station.ticket.entity;

import com.ganzithon.station.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대기실별 마지막 승차권 번호. 시드로 대기실마다 0이 들어갑니다.
 * 번호 발급은 이 엔티티를 읽고 고치지 않고, Repository의 네이티브 쿼리 한 문장(UPDATE ... RETURNING)으로 합니다.
 * 그래야 동시에 여러 명이 발급받아도 번호가 겹치지 않습니다.
 */
@Entity
@Table(name = "room_ticket_counters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomTicketCounter extends BaseTimeEntity {

    @Id
    @Column(name = "room_id")
    private Long roomId;

    @Column(nullable = false)
    private int lastNumber;
}
