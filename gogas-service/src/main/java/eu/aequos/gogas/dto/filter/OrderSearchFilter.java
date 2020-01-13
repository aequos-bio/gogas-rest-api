package eu.aequos.gogas.dto.filter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OrderSearchFilter {

    private String orderType;

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public LocalDate dueDateFrom;

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public LocalDate dueDateTo;

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public LocalDate deliveryDateFrom;

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public LocalDate deliveryDateTo;

    public Boolean inDelivery;
    public List<Integer> status;
    public Boolean paid;
}
