package eu.aequos.gogas.notification;

import eu.aequos.gogas.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserNotificationsCacheUnitTest {

    private UserNotificationsCache userNotificationsCache;

    @BeforeEach
    void setUp() {
        userNotificationsCache = new UserNotificationsCache();
        TenantContext.setTenantId("aTenant");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clearTenantId();
    }

    @Test
    void givenANotSentOrder_whenCheckingIfIsNotificationAlreadySent_thenFalseIsReturned() {
        boolean alreadySent = userNotificationsCache.isNotificationAlreadySent("order1", OrderEvent.Expiration);
        assertFalse(alreadySent);
    }

    @Test
    void givenASentOrder_whenCheckingIfIsNotificationAlreadySent_thenFalseIsReturned() {
        userNotificationsCache.addNotificationSent("order1", OrderEvent.Expiration);
        boolean alreadySent = userNotificationsCache.isNotificationAlreadySent("order1", OrderEvent.Expiration);
        assertTrue(alreadySent);
    }

    @Test
    void givenASentOrderForDifferentEvent_whenCheckingIfIsNotificationAlreadySent_thenFalseIsReturned() {
        userNotificationsCache.addNotificationSent("order1", OrderEvent.Expiration);
        boolean alreadySent = userNotificationsCache.isNotificationAlreadySent("order1", OrderEvent.Delivery);
        assertFalse(alreadySent);
    }

    @Test
    void givenADifferentSentOrderForSameEvent_whenCheckingIfIsNotificationAlreadySent_thenFalseIsReturned() {
        userNotificationsCache.addNotificationSent("order1", OrderEvent.Expiration);
        boolean alreadySent = userNotificationsCache.isNotificationAlreadySent("order2", OrderEvent.Expiration);
        assertFalse(alreadySent);
    }

    @Test
    void givenADifferentTenantSentForSameOrderForSameEvent_whenCheckingIfIsNotificationAlreadySent_thenFalseIsReturned() {
        userNotificationsCache.addNotificationSent("order1", OrderEvent.Expiration);

        TenantContext.setTenantId("anotherTenant");
        boolean alreadySent = userNotificationsCache.isNotificationAlreadySent("order1", OrderEvent.Expiration);
        assertFalse(alreadySent);
    }
}