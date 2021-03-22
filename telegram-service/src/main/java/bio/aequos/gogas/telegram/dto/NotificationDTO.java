package bio.aequos.gogas.telegram.dto;

import lombok.Data;

import java.util.List;

@Data
public class NotificationDTO {
    private List<String> userIds;
    private String text;
}
