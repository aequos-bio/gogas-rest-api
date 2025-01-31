package bio.aequos.gogas.telegram.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TelegramClient {

    private final TelegramBot bot;

    public TelegramClient(@Value("${telegram.bot-token}") String botToken) {
        this.bot = new TelegramBot(botToken);
    }

    public boolean sendMessage(long chatId, String message) {
        SendMessage sendMessage = new SendMessage(chatId, message)
                .parseMode(ParseMode.MarkdownV2);

        log.info("Sending message {} to chat {}", message, chatId);
        SendResponse execute = bot.execute(sendMessage);

        if (!execute.isOk()) {
            log.error("Error while sending message '{}': {}", message, execute.description());
        }

        return execute.isOk();
    }
}
