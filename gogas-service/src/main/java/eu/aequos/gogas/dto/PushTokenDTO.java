package eu.aequos.gogas.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class PushTokenDTO {
    @NotEmpty
    private String token;

    private String deviceId;
}
