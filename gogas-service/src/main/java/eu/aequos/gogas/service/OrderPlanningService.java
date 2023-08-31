package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.OrderDTO;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.repository.OrderTypeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class OrderPlanningService {

    private final OrderTypeRepo orderTypeRepo;

    public Stream<OrderDTO> getWeeklyOrders() {
        List<OrderType> weeklyOrderTypes = orderTypeRepo.findByHasTurns(true);
        LocalDate now = LocalDate.now();

        return weeklyOrderTypes.stream()
                .map(type  -> toOrderDTO(type, now));
    }

    private OrderDTO toOrderDTO(OrderType orderType, LocalDate openingDate) {
        LocalDate dueDate = openingDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate deliveryDate = dueDate.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderTypeId(orderType.getId());
        orderDTO.setOrderTypeName(orderType.getDescription());
        orderDTO.setOpeningDate(openingDate);
        orderDTO.setDueDate(dueDate);
        orderDTO.setDeliveryDate(deliveryDate);
        return orderDTO;
    }
}
