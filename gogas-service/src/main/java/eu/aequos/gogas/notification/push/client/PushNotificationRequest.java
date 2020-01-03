package eu.aequos.gogas.notification.push.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Getter
public class PushNotificationRequest {

    @JsonProperty("app_id")
    private String appId;

    @JsonProperty("include_player_ids")
    private List<String> targetTokens;

    @JsonProperty("data")
    private Payload data;

    @JsonProperty("headings")
    private TranslatedMessage headings;

    @JsonProperty("contents")
    private TranslatedMessage contents;

    @JsonProperty("android_group")
    private String androidGroup;

    @JsonProperty("android_group_message")
    private TranslatedMessage androidGroupMessage;

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setTargetTokens(List<String> targetTokens) {
        this.targetTokens = targetTokens;
    }

    public void setOrderId(String orderId) {
        this.data = new Payload(orderId);
    }

    public void setHeadings(String headings) {
        this.headings = new TranslatedMessage(headings);
    }

    public void setContents(String contents) {
        this.contents = new TranslatedMessage(contents);
    }

    public void setAndroidGroupMessage(String androidGroupMessage) {
        this.androidGroupMessage = new TranslatedMessage(androidGroupMessage);
    }


    public void setAndroidGroup(String androidGroup) {
        this.androidGroup = androidGroup;
    }

    @Data
    public static class Payload {
        private final String orderId;
    }

    @Data
    public static class TranslatedMessage {
        private final String message;

        @JsonProperty("en")
        public String getEnMessage() {
            return message;
        }

        @JsonProperty("it")
        public String getItMessage() {
            return message;
        }
    }
}
