package eu.aequos.gogas.integration;

import eu.aequos.gogas.integration.api.AequosApiClient;
import eu.aequos.gogas.integration.api.AequosOpenOrder;
import eu.aequos.gogas.integration.api.AequosOrderType;
import eu.aequos.gogas.integration.api.AequosPriceList;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.service.OrderTypeService;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AequosIntegrationService {

    private AequosApiClient aequosApiClient;
    private OrderTypeService orderTypeService;

    public AequosIntegrationService(AequosApiClient aequosApiClient, OrderTypeService orderTypeService) {
        this.aequosApiClient = aequosApiClient;
        this.orderTypeService = orderTypeService;
    }

    public void synchronizeOrderTypes() {
        Set<Integer> existingAequosOrderTypes = orderTypeService.getAequosOrderTypes();

        List<OrderType> orderTypesToBeCreated = aequosApiClient.orderTypes().stream()
                .filter(type -> !existingAequosOrderTypes.contains(type.getId()))
                .map(this::createOrderType)
                .collect(Collectors.toList());

        orderTypeService.createAll(orderTypesToBeCreated);
    }

    private OrderType createOrderType(AequosOrderType aequosOrderType) {
        OrderType orderType = new OrderType();
        orderType.setAequosOrderId(aequosOrderType.getId());
        orderType.setDescription(aequosOrderType.getDescription());
        orderType.setHasTurns(false);
        orderType.setSummaryRequired(true);
        orderType.setComputedAmount(true);
        orderType.setShowAdvance(true);
        orderType.setShowBoxCompletion(false);
        orderType.setExcelAllUsers(false);
        orderType.setExcelAllProducts(false);
        return orderType;
    }

    public List<AequosOpenOrder> getOpenOrders() {
        return aequosApiClient.openOrders();
    }

    public AequosPriceList getPriceList(int orderTypeId) {
        return aequosApiClient.getPriceList(orderTypeId);
    }
}
