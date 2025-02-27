package eu.aequos.gogas.notification.telegram;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelegramTemplate {
    private static final Pattern CHARS_TO_BE_ESCAPED_REGEX = Pattern.compile("([_*\\[\\]()~`>#+\\-=|{}.!])");

    private TelegramTemplate() {}

    public static String resolve(String templateText, String... params) {
        Object[] array = Arrays.stream(params)
                .map(TelegramTemplate::escapeText)
                .toArray();

        return String.format(templateText, array);
    }

    private static String escapeText(String originalText) {
        Matcher matcher = CHARS_TO_BE_ESCAPED_REGEX.matcher(originalText);
        return matcher.replaceAll("\\\\$1");
    }
}
