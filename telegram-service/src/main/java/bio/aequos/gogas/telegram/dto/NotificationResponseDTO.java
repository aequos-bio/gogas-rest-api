package bio.aequos.gogas.telegram.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponseDTO {
    private long sentCount;
    private long errorCount;
}
