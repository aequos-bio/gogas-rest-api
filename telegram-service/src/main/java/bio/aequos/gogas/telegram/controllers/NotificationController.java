package bio.aequos.gogas.telegram.controllers;

import bio.aequos.gogas.telegram.dto.ActivationDTO;
import bio.aequos.gogas.telegram.dto.NotificationDTO;
import bio.aequos.gogas.telegram.dto.NotificationResponseDTO;
import bio.aequos.gogas.telegram.persistence.model.UserChatEntity;
import bio.aequos.gogas.telegram.persistence.repository.UserChatRepo;
import bio.aequos.gogas.telegram.service.TelegramClient;
import bio.aequos.gogas.telegram.service.TokenHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("notification")
public class NotificationController {

    private final UserChatRepo userChatRepo;
    private final TelegramClient telegramClient;
    private final TokenHandler tokenHandler;

    @PostMapping("{tenantId}/send")
    public NotificationResponseDTO sendNotification(@PathVariable String tenantId, @RequestBody NotificationDTO notification) {
        List<Long> chatIds = notification.getUserIds().stream()
                .flatMap(userId -> userChatRepo.findByTenantIdAndUserId(tenantId, userId).stream())
                .map(UserChatEntity::getChatId)
                .collect(Collectors.toList());

        List<Boolean> collect = chatIds.stream()
                .map(chatId -> sendNotification(chatId, notification.getText()))
                .collect(Collectors.toList());

        return NotificationResponseDTO.builder()
                .sentCount(collect.stream().filter(x -> x).count())
                .errorCount(collect.stream().filter(x -> !x).count())
                .build();
    }

    private boolean sendNotification(long chatId, String text) {
        return telegramClient.sendMessage(chatId, text);
    }

    @GetMapping("{tenantId}/{userId}/activation")
    public ActivationDTO generateToken(@PathVariable String tenantId, @PathVariable String userId) {
        String code = tokenHandler.generateToken(tenantId, userId);
        return new ActivationDTO(code);
    }
}
