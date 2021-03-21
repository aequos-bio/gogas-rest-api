package bio.aequos.gogas.telegram.controllers;

import bio.aequos.gogas.telegram.dto.ActivationDTO;
import bio.aequos.gogas.telegram.dto.NotificationDTO;
import bio.aequos.gogas.telegram.persistence.repository.UserChatRepo;
import bio.aequos.gogas.telegram.service.TelegramClient;
import bio.aequos.gogas.telegram.service.TokenHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("notification")
public class NotificationController {

    private final UserChatRepo userChatRepo;
    private final TelegramClient telegramClient;
    private final TokenHandler tokenHandler;

    @PostMapping("{tenantId}/{userId}/send")
    public String sendNotification(@PathVariable String tenantId, @PathVariable String userId, @RequestBody NotificationDTO notification) {
        log.info("Sending notification to {}/{}", tenantId, userId);

        userChatRepo.findByTenantIdAndUserId(tenantId, userId)
                .forEach(userChat -> sendNotification(userChat.getChatId(), notification.getText()));

        return "OK";
    }

    private void sendNotification(long chatId, String text) {
        telegramClient.sendMessage(chatId, text);
    }

    @GetMapping("{tenantId}/{userId}/activation")
    public ActivationDTO generateToken(@PathVariable String tenantId, @PathVariable String userId) {
        String code = tokenHandler.generateToken(tenantId, userId);
        return new ActivationDTO(code);
    }
}
