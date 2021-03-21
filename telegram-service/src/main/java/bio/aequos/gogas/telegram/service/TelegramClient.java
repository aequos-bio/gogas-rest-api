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

    public void sendMessage(long chatId, String message) {
        SendMessage sendMessage = new SendMessage(chatId, message)
                .parseMode(ParseMode.MarkdownV2);

        SendResponse execute = bot.execute(sendMessage);
        log.info("message sent {}", execute);
    }
}
