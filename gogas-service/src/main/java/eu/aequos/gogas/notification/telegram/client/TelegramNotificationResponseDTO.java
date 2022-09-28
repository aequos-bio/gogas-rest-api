package eu.aequos.gogas.notification.telegram.client;

import lombok.Data;

@Data
public class TelegramNotificationResponseDTO {
    private long sentCount;
    private long errorCount;
}
