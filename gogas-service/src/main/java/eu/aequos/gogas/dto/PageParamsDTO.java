package eu.aequos.gogas.dto;

import lombok.Data;

@Data
public class PageParamsDTO {
    private final int pageNumber;
    private final int pageSize;
}
