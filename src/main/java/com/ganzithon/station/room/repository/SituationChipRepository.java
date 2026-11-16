package com.ganzithon.station.room.repository;

import com.ganzithon.station.room.entity.Room;
import com.ganzithon.station.room.entity.SituationChip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SituationChipRepository extends JpaRepository<SituationChip, Long> {

    /** 방까지 조건에 넣어서, 코드가 있어도 다른 방의 칩이면 찾지 못하게 합니다. */
    Optional<SituationChip> findByRoomAndCode(Room room, String code);

    List<SituationChip> findAllByRoomOrderBySortOrderAsc(Room room);
}
