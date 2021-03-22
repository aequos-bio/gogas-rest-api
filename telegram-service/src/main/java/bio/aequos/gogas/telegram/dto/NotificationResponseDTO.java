package bio.aequos.gogas.telegram.dto;

import lombok.Builder;

@Builder
public class NotificationResponseDTO {
    private long sentCount;
    private long errorCount;
}
