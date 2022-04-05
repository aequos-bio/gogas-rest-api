package eu.aequos.gogas.notification.push.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "pushNotificationService", url = "https://onesignal.com", path="/api/v1")
public interface PushNotificationClient {

    @PostMapping(value = "notifications")
    String sendNotifications(@RequestHeader(value = "Authorization") String authorizationHeader, @RequestBody PushNotificationRequest request);
}
