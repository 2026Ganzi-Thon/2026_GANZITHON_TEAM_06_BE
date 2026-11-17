package com.ganzithon.station.room.repository;

import com.ganzithon.station.room.entity.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternRepository extends JpaRepository<Pattern, Long> {
}
