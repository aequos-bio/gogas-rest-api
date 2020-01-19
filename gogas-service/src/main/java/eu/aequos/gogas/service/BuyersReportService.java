package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.BuyersReportDTO;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.specification.OrderSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BuyersReportService {

    private OrderRepo orderRepo;
    private OrderItemService orderItemService;
    private UserService userService;

    public BuyersReportService(OrderRepo orderRepo, OrderItemService orderItemService, UserService userService) {
        this.orderRepo = orderRepo;
        this.orderItemService = orderItemService;
        this.userService = userService;
    }

    public BuyersReportDTO generateBuyersReport(String orderTypeId, LocalDate dateFrom, LocalDate dateTo) {
        List<Order> orderList = searchOrdersBetweenDates(orderTypeId, dateFrom, dateTo);
        List<Buyer> buyerList = extractBuyers(orderList);

        return new BuyersReportDTO.Builder()
                .withSortedOrderDates(orderList)
                .withSortedBuyers(buyerList)
                .build();
    }

    private List<Buyer> extractBuyers(List<Order> orderList) {
        Set<String> orderIds = ListConverter.fromList(orderList).extractIds(Order::getId);
        Map<String, List<String>> buyersMap = orderItemService.getBuyersInOrderIds(orderIds);
        Map<String, String> userFullNamesMap = userService.getUsersFullNameMap(buyersMap.keySet());

        return buyersMap.entrySet().stream()
                .map(e -> new Buyer(userFullNamesMap.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(Buyer::getName))
                .collect(Collectors.toList());
    }

    private List<Order> searchOrdersBetweenDates(String orderTypeId, LocalDate dateFrom, LocalDate dateTo) {
        Specification<Order> filter = new SpecificationBuilder<Order>()
                .withBaseFilter(OrderSpecs.select())
                .and(OrderSpecs::type, orderTypeId)
                .and(OrderSpecs::deliveryDateFrom, dateFrom)
                .and(OrderSpecs::deliveryDateTo, dateTo)
                .build();

        List<Order> orderList = orderRepo.findAll(filter);
        orderList.sort(Comparator.comparing(Order::getDeliveryDate));

        return orderList;
    }

    @Data
    public static class Buyer {
        private final String name;
        private final List<String> orderDates;

        public int hasOrder(String orderDate) {
            if (orderDates.contains(orderDate))
                return 1;

            return 0;
        }
    }
}
