package eu.aequos.gogas.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@ApiModel("PasswordChange")
@Data
public class PasswordChangeDTO {

    @ApiModelProperty(required = true)
    @NotEmpty
    private String oldPassword;

    @ApiModelProperty(required = true)
    @NotEmpty
    private String newPassword;
}
