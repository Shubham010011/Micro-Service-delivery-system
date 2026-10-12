package com.realtime.delivery.auth.repository;

import com.realtime.delivery.auth.model.User;
import com.realtime.delivery.common.constants.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmail(String email);
    List<User> findAllByRole(Role role);
}
