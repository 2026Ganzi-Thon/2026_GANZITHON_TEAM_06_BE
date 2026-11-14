package com.ganzithon.station.room.repository;

import com.ganzithon.station.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    List<Room> findAllByOrderBySortOrderAsc();
}
