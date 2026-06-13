package com.pnpn.userservice.repository;

import com.pnpn.shared.enums.UserType;
import com.pnpn.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByIdAndType(UUID id, UserType type);

    List<User> findByVerified(Boolean verified);
}
