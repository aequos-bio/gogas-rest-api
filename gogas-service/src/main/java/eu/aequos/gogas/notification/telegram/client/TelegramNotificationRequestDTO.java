package eu.aequos.gogas.notification.telegram.client;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class TelegramNotificationRequestDTO {
    private Set<String> userIds;
    private String text;
}
