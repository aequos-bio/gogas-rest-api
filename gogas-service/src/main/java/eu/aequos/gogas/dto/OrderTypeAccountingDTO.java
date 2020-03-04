package eu.aequos.gogas.dto;

import lombok.Data;

@Data
public class OrderTypeAccountingDTO {
    private final String id;
    private final String description;
    private final String accountingCode;
}
