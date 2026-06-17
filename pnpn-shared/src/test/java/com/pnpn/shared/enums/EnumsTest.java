package com.pnpn.shared.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EnumsTest {

    @Test
    void testUserTypeValues() {
        UserType[] values = UserType.values();
        assertEquals(3, values.length);
        assertEquals(UserType.INFLUENCER, UserType.valueOf("INFLUENCER"));
        assertEquals(UserType.FOLLOWER, UserType.valueOf("FOLLOWER"));
        assertEquals(UserType.ADMIN, UserType.valueOf("ADMIN"));
    }

    @Test
    void testListingStatusValues() {
        ListingStatus[] values = ListingStatus.values();
        assertEquals(4, values.length);
        assertEquals(ListingStatus.ACTIVE, ListingStatus.valueOf("ACTIVE"));
        assertEquals(ListingStatus.SOLD, ListingStatus.valueOf("SOLD"));
        assertEquals(ListingStatus.WITHDRAWN, ListingStatus.valueOf("WITHDRAWN"));
        assertEquals(ListingStatus.RESERVED, ListingStatus.valueOf("RESERVED"));
    }

    @Test
    void testListingConditionValues() {
        ListingCondition[] values = ListingCondition.values();
        assertEquals(5, values.length);
        assertEquals(ListingCondition.NEW, ListingCondition.valueOf("NEW"));
        assertEquals(ListingCondition.LIKE_NEW, ListingCondition.valueOf("LIKE_NEW"));
        assertEquals(ListingCondition.GENTLY_USED, ListingCondition.valueOf("GENTLY_USED"));
        assertEquals(ListingCondition.USED, ListingCondition.valueOf("USED"));
        assertEquals(ListingCondition.FAIR, ListingCondition.valueOf("FAIR"));
    }

    @Test
    void testOrderStatusValues() {
        OrderStatus[] values = OrderStatus.values();
        assertEquals(6, values.length);
        assertEquals(OrderStatus.PENDING, OrderStatus.valueOf("PENDING"));
        assertEquals(OrderStatus.CONFIRMED, OrderStatus.valueOf("CONFIRMED"));
        assertEquals(OrderStatus.SHIPPED, OrderStatus.valueOf("SHIPPED"));
        assertEquals(OrderStatus.DELIVERED, OrderStatus.valueOf("DELIVERED"));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.valueOf("CANCELLED"));
        assertEquals(OrderStatus.REFUNDED, OrderStatus.valueOf("REFUNDED"));
    }

    @Test
    void testAuctionStatusValues() {
        AuctionStatus[] values = AuctionStatus.values();
        assertEquals(6, values.length);
        assertEquals(AuctionStatus.SCHEDULED, AuctionStatus.valueOf("SCHEDULED"));
        assertEquals(AuctionStatus.LIVE, AuctionStatus.valueOf("LIVE"));
        assertEquals(AuctionStatus.EXTENDED, AuctionStatus.valueOf("EXTENDED"));
        assertEquals(AuctionStatus.ENDED, AuctionStatus.valueOf("ENDED"));
        assertEquals(AuctionStatus.SOLD, AuctionStatus.valueOf("SOLD"));
        assertEquals(AuctionStatus.WITHDRAWN, AuctionStatus.valueOf("WITHDRAWN"));
    }

    @Test
    void testPaymentStatusValues() {
        PaymentStatus[] values = PaymentStatus.values();
        assertEquals(4, values.length);
        assertEquals(PaymentStatus.PENDING, PaymentStatus.valueOf("PENDING"));
        assertEquals(PaymentStatus.COMPLETED, PaymentStatus.valueOf("COMPLETED"));
        assertEquals(PaymentStatus.FAILED, PaymentStatus.valueOf("FAILED"));
        assertEquals(PaymentStatus.REFUNDED, PaymentStatus.valueOf("REFUNDED"));
    }

    @Test
    void testNotificationTypeValues() {
        NotificationType[] values = NotificationType.values();
        assertEquals(6, values.length);
        assertEquals(NotificationType.ORDER, NotificationType.valueOf("ORDER"));
        assertEquals(NotificationType.BID, NotificationType.valueOf("BID"));
        assertEquals(NotificationType.MESSAGE, NotificationType.valueOf("MESSAGE"));
        assertEquals(NotificationType.REVIEW, NotificationType.valueOf("REVIEW"));
        assertEquals(NotificationType.OFFER, NotificationType.valueOf("OFFER"));
        assertEquals(NotificationType.SYSTEM, NotificationType.valueOf("SYSTEM"));
    }

    @Test
    void testVerificationStatusValues() {
        VerificationStatus[] values = VerificationStatus.values();
        assertEquals(4, values.length);
        assertEquals(VerificationStatus.UNVERIFIED, VerificationStatus.valueOf("UNVERIFIED"));
        assertEquals(VerificationStatus.PENDING, VerificationStatus.valueOf("PENDING"));
        assertEquals(VerificationStatus.VERIFIED, VerificationStatus.valueOf("VERIFIED"));
        assertEquals(VerificationStatus.REJECTED, VerificationStatus.valueOf("REJECTED"));
    }

    @Test
    void testAllEnumsHaveNonNullName() {
        assertAll(
            () -> Arrays.stream(UserType.values()).forEach(e -> assertNotNull(e.name())),
            () -> Arrays.stream(ListingStatus.values()).forEach(e -> assertNotNull(e.name())),
            () -> Arrays.stream(ListingCondition.values()).forEach(e -> assertNotNull(e.name())),
            () -> Arrays.stream(OrderStatus.values()).forEach(e -> assertNotNull(e.name())),
            () -> Arrays.stream(AuctionStatus.values()).forEach(e -> assertNotNull(e.name())),
            () -> Arrays.stream(PaymentStatus.values()).forEach(e -> assertNotNull(e.name())),
            () -> Arrays.stream(NotificationType.values()).forEach(e -> assertNotNull(e.name())),
            () -> Arrays.stream(VerificationStatus.values()).forEach(e -> assertNotNull(e.name()))
        );
    }

    @Test
    void testAllEnumsHaveUniqueOrdinals() {
        assertAll(
            () -> assertEquals(UserType.values().length,
                Arrays.stream(UserType.values()).mapToInt(Enum::ordinal).distinct().count()),
            () -> assertEquals(ListingStatus.values().length,
                Arrays.stream(ListingStatus.values()).mapToInt(Enum::ordinal).distinct().count()),
            () -> assertEquals(ListingCondition.values().length,
                Arrays.stream(ListingCondition.values()).mapToInt(Enum::ordinal).distinct().count()),
            () -> assertEquals(OrderStatus.values().length,
                Arrays.stream(OrderStatus.values()).mapToInt(Enum::ordinal).distinct().count()),
            () -> assertEquals(AuctionStatus.values().length,
                Arrays.stream(AuctionStatus.values()).mapToInt(Enum::ordinal).distinct().count()),
            () -> assertEquals(PaymentStatus.values().length,
                Arrays.stream(PaymentStatus.values()).mapToInt(Enum::ordinal).distinct().count()),
            () -> assertEquals(NotificationType.values().length,
                Arrays.stream(NotificationType.values()).mapToInt(Enum::ordinal).distinct().count()),
            () -> assertEquals(VerificationStatus.values().length,
                Arrays.stream(VerificationStatus.values()).mapToInt(Enum::ordinal).distinct().count())
        );
    }

    @Test
    void testEnumNameMatchesToString() {
        assertAll(
            () -> Arrays.stream(UserType.values()).forEach(e -> assertEquals(e.name(), e.toString())),
            () -> Arrays.stream(ListingStatus.values()).forEach(e -> assertEquals(e.name(), e.toString())),
            () -> Arrays.stream(ListingCondition.values()).forEach(e -> assertEquals(e.name(), e.toString())),
            () -> Arrays.stream(OrderStatus.values()).forEach(e -> assertEquals(e.name(), e.toString())),
            () -> Arrays.stream(AuctionStatus.values()).forEach(e -> assertEquals(e.name(), e.toString())),
            () -> Arrays.stream(PaymentStatus.values()).forEach(e -> assertEquals(e.name(), e.toString())),
            () -> Arrays.stream(NotificationType.values()).forEach(e -> assertEquals(e.name(), e.toString())),
            () -> Arrays.stream(VerificationStatus.values()).forEach(e -> assertEquals(e.name(), e.toString()))
        );
    }
}
