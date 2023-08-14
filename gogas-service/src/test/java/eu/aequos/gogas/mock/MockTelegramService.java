package eu.aequos.gogas.mock;

import eu.aequos.gogas.notification.telegram.client.TelegramActivationDTO;
import eu.aequos.gogas.notification.telegram.client.TelegramNotificationRequestDTO;
import eu.aequos.gogas.notification.telegram.client.TelegramNotificationResponseDTO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller("mock/telegram")
public class MockTelegramService {

    @GetMapping(value = "notification/{tenantId}/{userId}/activation")
    TelegramActivationDTO activateUser(@PathVariable String tenantId, @PathVariable String userId) {
        TelegramActivationDTO activationDTO = new TelegramActivationDTO();
        activationDTO.setCode("12345");
        return activationDTO;
    }

    @PostMapping(value = "notification/{tenantId}/send")
    TelegramNotificationResponseDTO sendNotifications(@PathVariable String tenantId, @RequestBody TelegramNotificationRequestDTO request) {
        TelegramNotificationResponseDTO responseDTO = new TelegramNotificationResponseDTO();
        responseDTO.setSentCount(1);
        return responseDTO;
    }
}
