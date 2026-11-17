package com.ganzithon.station.user.repository;

import com.ganzithon.station.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
