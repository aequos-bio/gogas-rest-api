package eu.aequos.gogas.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.aequos.gogas.dto.CredentialsDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.integration.api.*;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.derived.SupplierOrderBoxes;
import eu.aequos.gogas.service.ConfigurationService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AequosIntegrationService {

    private AequosApiClient aequosApiClient;
    private ConfigurationService configurationService;
    private ObjectMapper objectMapper;

    public AequosIntegrationService(AequosApiClient aequosApiClient, ConfigurationService configurationService) {
        this.aequosApiClient = aequosApiClient;
        this.configurationService = configurationService;
        this.objectMapper = new ObjectMapper();
    }

    public List<OrderType> synchronizeOrderTypes(Set<Integer> existingAequosOrderTypes) {
        return aequosApiClient.orderTypes().stream()
                .filter(type -> !existingAequosOrderTypes.contains(type.getId()))
                .map(this::createOrderType)
                .collect(Collectors.toList());
    }

    private OrderType createOrderType(AequosOrderType aequosOrderType) {
        OrderType orderType = new OrderType();
        orderType.setAequosOrderId(aequosOrderType.getId());
        orderType.setDescription(aequosOrderType.getDescription());
        orderType.setBilledByAequos(aequosOrderType.isBilledByAequos());
        orderType.setComputedAmount(true);
        orderType.setShowAdvance(true);
        orderType.setHasTurns(false);
        orderType.setSummaryRequired(false);
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

    public String createOrUpdateOrder(int aequosOrderType, String aequosOrderId, List<SupplierOrderBoxes> orderBoxes) throws GoGasException {
        Map<String, String> formParams = initParamsWithCredentials();
        formParams.put("tipo_ordine", Integer.toString(aequosOrderType));
        formParams.put("rows", extractAndSerializeOrderItems(orderBoxes));

        if (aequosOrderId != null)
            formParams.put("order_id", aequosOrderId);

        OrderCreatedResponse response = aequosApiClient.createOrder(formParams);

        if (response.isError())
            throw new GoGasException("Errore durante l'invio dell'ordine ad Aequos: " + response.getErrorMessage());

        BigDecimal sentBoxCount = orderBoxes.stream()
                .map(SupplierOrderBoxes::getBoxesCount)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        if (response.getTotalItems() != sentBoxCount.intValue())
            throw new GoGasException("Errore durante l'invio dell'ordine: i colli inseriti (" + response.getTotalItems() + ") non corrispondono a quelli inviati (" + sentBoxCount.intValue() + ")");

        return response.getOrderId();
    }

    public List<String> sendUpdatedWeights(String aequosOrderId, List<SupplierOrderBoxes> orderBoxes) throws GoGasException {
        Map<String, String> formParams = initParamsWithCredentials();
        formParams.put("order_id", aequosOrderId);
        formParams.put("rows", extractAndSerializeOrderItems(orderBoxes));

        WeightsUpdatedResponse response = aequosApiClient.updateWeight(formParams);

        if (response.isError())
            throw new GoGasException("Errore durante l'invio dei pesi per l'ordine aequos " + aequosOrderId + ": " + response.getErrorMessage());

        return response.getUpdatedItems();
    }

    public OrderSynchResponse synchronizeOrder(String aequosOrderId) throws GoGasException {
        Map<String, String> formParams = initParamsWithCredentials();
        formParams.put("order_id", aequosOrderId);

        OrderSynchResponse response = aequosApiClient.synchOrder(formParams);

        if (response.isError())
            throw new GoGasException("Errore durante l'invio dei pesi per l'ordine aequos " + aequosOrderId + ": " + response.getErrorMessage());

        return response;
    }

    private String extractAndSerializeOrderItems(List<SupplierOrderBoxes> orderBoxes) throws GoGasException {
        List<OrderCreationItem> orderItems = orderBoxes.stream()
                .map(this::convertToCreationItem)
                .collect(Collectors.toList());

        try {
            return objectMapper.writeValueAsString(orderItems);
        } catch (JsonProcessingException e) {
            throw new GoGasException("Errore durante l'invio dell'ordine ad Aequos:" + e.getClass() + " - " + e.getMessage());
        }
    }

    private OrderCreationItem convertToCreationItem(SupplierOrderBoxes supplierOrderBoxes) {
        OrderCreationItem orderCreationItem = new OrderCreationItem();
        orderCreationItem.setId(supplierOrderBoxes.getSupplierCode());
        orderCreationItem.setBoxesCount(supplierOrderBoxes.getBoxesCount());
        return orderCreationItem;
    }

    private Map<String, String> initParamsWithCredentials() throws GoGasException {
        CredentialsDTO credentials = configurationService.getAequosCredentials();

        Map<String, String> formParams = new HashMap<>();
        formParams.put("username", credentials.getUsername());
        formParams.put("password", credentials.getPassword());

        return formParams;
    }
}
