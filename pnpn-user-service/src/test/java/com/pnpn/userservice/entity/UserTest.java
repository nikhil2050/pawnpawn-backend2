package com.pnpn.userservice.entity;

import com.pnpn.shared.enums.UserType;
import com.pnpn.shared.enums.VerificationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserUsingBuilder() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .type(UserType.INFLUENCER)
                .name("Jane Doe")
                .email("jane@example.com")
                .passwordHash("hashed123")
                .verified(false)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .followerCount(0)
                .reputationScore(BigDecimal.ZERO)
                .totalReviews(0)
                .build();

        assertEquals(id, user.getId());
        assertEquals(UserType.INFLUENCER, user.getType());
        assertEquals("Jane Doe", user.getName());
        assertEquals("jane@example.com", user.getEmail());
        assertFalse(user.getVerified());
        assertEquals(VerificationStatus.UNVERIFIED, user.getVerificationStatus());
    }

    @Test
    void shouldMutateFieldsViaSetters() {
        User user = new User();
        user.setName("John");
        user.setPhone("+1234567890");
        user.setBio("A bio");
        user.setVerified(true);
        user.setReputationScore(BigDecimal.valueOf(4.5));

        assertEquals("John", user.getName());
        assertEquals("+1234567890", user.getPhone());
        assertEquals("A bio", user.getBio());
        assertTrue(user.getVerified());
        assertEquals(0, BigDecimal.valueOf(4.5).compareTo(user.getReputationScore()));
    }

    @Test
    void defaultValuesShouldBeNullInPojo() {
        User user = new User();
        assertNull(user.getVerified());
        assertNull(user.getFollowerCount());
    }

    @Test
    void shouldHandleEquality() {
        UUID id = UUID.randomUUID();
        User user1 = User.builder().id(id).email("a@b.com").build();
        User user2 = User.builder().id(id).email("a@b.com").build();

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }
}
