package eu.aequos.gogas.excel.order;

import lombok.Data;

@Data
public class UserExport {
    private String id;
    private String fullName;
    private String role;
    private String phone;
    private String email;
}
