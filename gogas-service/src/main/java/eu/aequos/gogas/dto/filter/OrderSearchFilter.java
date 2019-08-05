package eu.aequos.gogas.dto.filter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OrderSearchFilter {

    private String orderType;

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public Date dueDateFrom;

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public Date dueDateTo;

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public Date deliveryDateFrom;

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public Date deliveryDateTo;

    public Boolean inDelivery;
    public List<Integer> status;
    public Boolean paid;
}
