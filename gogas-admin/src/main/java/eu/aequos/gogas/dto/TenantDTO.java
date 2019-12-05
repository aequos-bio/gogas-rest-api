package eu.aequos.gogas.dto;

import lombok.Data;

@Data
public class TenantDTO {
    private String tenantId;
    private String gasName;
    private String iisSite;

    public TenantDTO() {}

    public TenantDTO(String tenantId, String gasName, String iisSite) {
        this.tenantId = tenantId;
        this.gasName = gasName;
        this.iisSite = iisSite;
    }
}
