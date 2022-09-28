package bio.aequos.gogas.telegram.controllers;

import bio.aequos.gogas.telegram.dto.TelegramUpdateDTO;
import bio.aequos.gogas.telegram.persistence.model.UserChatEntity;
import bio.aequos.gogas.telegram.persistence.repository.UserChatRepo;
import bio.aequos.gogas.telegram.service.TelegramClient;
import bio.aequos.gogas.telegram.service.TokenHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("webhook/${telegram.bot-token}")
public class WebHookController {

    private final TokenHandler tokenHandler;
    private final UserChatRepo userChatRepo;
    private final TelegramClient telegramClient;

    @PostMapping
    public void incomingMessage(@RequestBody TelegramUpdateDTO updateMessage) throws Exception {
        log.info("Update received from telegram: " + updateMessage);

        TelegramUpdateDTO.TelegramMessage message = updateMessage.getMessage();
        if (message == null) {
            log.warn("No message found, skipping update");
            return;
        }

        String messageBody = message.getText();
        if (messageBody == null) {
            log.warn("Empty message found, skipping update");
            return;
        }

        if (messageBody.startsWith("/start")) {
            activateUser(message, messageBody);
        }
    }

    private void activateUser(TelegramUpdateDTO.TelegramMessage message, String messageBody) throws Exception {
        log.info("Start message, reading token");
        String[] messgeTokens = messageBody.split(" ");
        if (messgeTokens.length != 2) {
            log.warn("invalid start message format: " + messageBody);
            return;
        }

        String activationCode = messgeTokens[1];
        log.info("The activation code is {}", activationCode);
        TokenHandler.TokenInfo userByToken = tokenHandler.getUserByToken(activationCode);

        UserChatEntity userChatEntity = buildUserChat(message, userByToken);
        userChatRepo.save(userChatEntity);

        telegramClient.sendMessage(message.getChat().getId(), String.format("Ciao *%s*, benvenuto in _Go\\!Gas_\\. Riceverai aggiornamenti sui tuoi ordini\\.", message.getChat().getFirstName()));
    }

    private UserChatEntity buildUserChat(TelegramUpdateDTO.TelegramMessage message, TokenHandler.TokenInfo userByToken) {
        UserChatEntity userChatEntity = new UserChatEntity();
        userChatEntity.setChatId(message.getChat().getId());
        userChatEntity.setTenantId(userByToken.getTenantId());
        userChatEntity.setUserId(userByToken.getUserId());
        userChatEntity.setDateCreated(LocalDateTime.now());
        userChatEntity.setLastUpdate(LocalDateTime.now());
        return userChatEntity;
    }
}
