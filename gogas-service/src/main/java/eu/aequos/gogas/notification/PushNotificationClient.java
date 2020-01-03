package eu.aequos.gogas.notification;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "pushNotificationService", url = "https://onesignal.com")
@RequestMapping(value = "api/v1")
public interface PushNotificationClient {

    @PostMapping(value = "notifications")
    String sendNotifications(@RequestHeader(value = "Authorization") String authorizationHeader, @RequestBody PushNotificationRequest request);
}
