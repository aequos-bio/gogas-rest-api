package eu.aequos.gogas.notification.telegram.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "telegramNotificationService", url = "http://localhost:8080", path="/")
public interface TelegramNotificationClient {

    @GetMapping(value = "notification/{tenantId}/{userId}/activation")
    String activateUser(@PathVariable String tenantId, @PathVariable String userId);

    @PostMapping(value = "notification/{tenantId}/send")
    TelegramNotificationResponseDTO sendNotifications(@PathVariable String tenantId, @RequestBody TelegramNotificationRequestDTO request);
}
