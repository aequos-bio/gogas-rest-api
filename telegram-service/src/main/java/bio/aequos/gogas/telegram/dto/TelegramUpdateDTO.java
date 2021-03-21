package bio.aequos.gogas.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TelegramUpdateDTO {
    private static final long serialVersionUID = 0L;

    @JsonProperty("update_id")
    private Integer updateId;

    private TelegramMessage message;

    @Data
    public static class TelegramMessage {
        @JsonProperty("message_id")
        private Integer messageId;

        private Integer date;

        private TelegramChat chat;

        private String text;

        private TelegramMessageEntity[] entities;
    }

    @Data
    public static class TelegramChat {
        private Long id;

        private String type;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;
    }

    @Data
    public static final class TelegramMessageEntity {
        private String type;
        private Integer offset;
        private Integer length;
        private String url;
        private String language;
    }
}
