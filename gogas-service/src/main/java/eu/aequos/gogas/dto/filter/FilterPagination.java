package eu.aequos.gogas.dto.filter;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FilterPagination {
    @ApiModelProperty("Page number")
    private int pageNumber;

    @ApiModelProperty("Number of results per page")
    private int pageSize;
}
