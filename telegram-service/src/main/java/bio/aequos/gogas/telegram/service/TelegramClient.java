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

    private static final String[] TO_BE_ESCAPED = new String[] {
            "_", "*", "[", "]", "(", ")", "~", "`", ">", "#", "+", "-", "=", "|", "{", "}", ".", "!"
    };

    private final TelegramBot bot;

    public TelegramClient(@Value("${telegram.bot-token}") String botToken) {
        this.bot = new TelegramBot(botToken);
    }

    public boolean sendMessage(long chatId, String message) {
        String escapedMessage = escapeMessage(message);

        SendMessage sendMessage = new SendMessage(chatId, escapedMessage)
                .parseMode(ParseMode.MarkdownV2);

        SendResponse execute = bot.execute(sendMessage);
        log.info("message sent {}", execute);

        if (!execute.isOk()) {
            log.error("Error while sending message: {}", execute.description());
        }

        return execute.isOk();
    }

    private String escapeMessage(String message) {
        for (String s : TO_BE_ESCAPED) {
            message = message.replace(s, "\\" + s);
        }
        return message;
    }
}
