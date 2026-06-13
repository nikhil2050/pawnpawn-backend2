package com.pnpn.userservice.repository;

import com.pnpn.shared.enums.UserType;
import com.pnpn.shared.enums.VerificationStatus;
import com.pnpn.userservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User influencer;
    private User follower;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        influencer = userRepository.save(User.builder()
                .type(UserType.INFLUENCER)
                .name("Alice")
                .email("alice@example.com")
                .phone("+111111111")
                .passwordHash("hash1")
                .verified(true)
                .verificationStatus(VerificationStatus.VERIFIED)
                .followerCount(100)
                .reputationScore(BigDecimal.valueOf(4.8))
                .totalReviews(20)
                .build());

        follower = userRepository.save(User.builder()
                .type(UserType.FOLLOWER)
                .name("Bob")
                .email("bob@example.com")
                .passwordHash("hash2")
                .verified(false)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .followerCount(0)
                .reputationScore(BigDecimal.ZERO)
                .totalReviews(0)
                .build());
    }

    @Test
    void shouldSaveAndFindById() {
        Optional<User> found = userRepository.findById(influencer.getId());

        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().getName());
        assertEquals(UserType.INFLUENCER, found.get().getType());
    }

    @Test
    void shouldFindByEmail() {
        Optional<User> found = userRepository.findByEmail("bob@example.com");

        assertTrue(found.isPresent());
        assertEquals("Bob", found.get().getName());
    }

    @Test
    void shouldReturnEmptyForUnknownEmail() {
        Optional<User> found = userRepository.findByEmail("unknown@example.com");

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindByPhone() {
        Optional<User> found = userRepository.findByPhone("+111111111");

        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().getName());
    }

    @Test
    void shouldReturnEmptyForUnknownPhone() {
        Optional<User> found = userRepository.findByPhone("+999999999");

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindByIdAndType() {
        Optional<User> found = userRepository.findByIdAndType(influencer.getId(), UserType.INFLUENCER);

        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().getName());
    }

    @Test
    void shouldNotFindByIdAndTypeMismatch() {
        Optional<User> found = userRepository.findByIdAndType(influencer.getId(), UserType.FOLLOWER);

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindVerifiedUsers() {
        List<User> verified = userRepository.findByVerified(true);

        assertEquals(1, verified.size());
        assertEquals("Alice", verified.getFirst().getName());
    }

    @Test
    void shouldFindUnverifiedUsers() {
        List<User> unverified = userRepository.findByVerified(false);

        assertEquals(1, unverified.size());
        assertEquals("Bob", unverified.getFirst().getName());
    }

    @Test
    void shouldEnforceUniqueEmail() {
        User duplicate = User.builder()
                .type(UserType.FOLLOWER)
                .name("Charlie")
                .email("alice@example.com")
                .passwordHash("hash3")
                .verified(false)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .followerCount(0)
                .reputationScore(BigDecimal.ZERO)
                .totalReviews(0)
                .build();

        assertThrows(Exception.class, () -> userRepository.saveAndFlush(duplicate));
    }

    @Test
    void shouldDeleteUser() {
        userRepository.delete(influencer);

        Optional<User> found = userRepository.findById(influencer.getId());
        assertTrue(found.isEmpty());
    }
}
