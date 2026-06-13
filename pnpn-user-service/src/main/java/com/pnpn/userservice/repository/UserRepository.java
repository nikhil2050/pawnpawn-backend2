package com.pnpn.userservice.repository;

import com.pnpn.shared.enums.UserType;
import com.pnpn.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data-access layer for the {@link User} entity.
 * <p>
 * Spring Data JPA automatically implements all methods declared here.
 * Extends {@link JpaRepository} which provides {@code save()},
 * {@code findById()}, {@code findAll()}, {@code delete()}, etc. out of the box.
 * <p>
 * Custom query methods rely on Spring Data's derived-query mechanism:
 * the method name is parsed to build the corresponding JPQL / SQL.
 *
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Used during login and registration (to check for duplicates).
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Used for phone-based OTP login flows.
     *
     * @param phone the phone number to search for
     * @return an {@link Optional} containing the user if found, empty otherwise
     */
    Optional<User> findByPhone(String phone);

    /**
     * Useful for authorization checks that need to verify a user's role.
     *
     * @param id   the user's UUID
     * @param type the expected {@link UserType}
     * @return an {@link Optional} containing the user if a match exists
     */
    Optional<User> findByIdAndType(UUID id, UserType type);

    /**
     * Used by admin flows to list verified / unverified influencers.
     *
     * @param verified {@code true} for verified users, {@code false} otherwise
     * @return list of matching users (may be empty)
     */
    List<User> findByVerified(Boolean verified);
}
