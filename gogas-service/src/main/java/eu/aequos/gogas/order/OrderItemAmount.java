package eu.aequos.gogas.order;

import java.math.BigDecimal;
import java.util.Optional;

public class OrderItemAmount {

    private BigDecimal orderRequestedQty;
    private BigDecimal orderDeliveredQty;
    private String boxUnitOfMeasure;
    private String orderUnitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal boxWeight;

    private OrderItemAmount() {}

    public static OrderItemAmount builder() {
        return new OrderItemAmount();
    }

    public OrderItemAmount withOrderRequestedQty(BigDecimal orderRequestedQty) {
        this.orderRequestedQty = orderRequestedQty;
        return this;
    }

    public OrderItemAmount withOrderDeliveredQty(BigDecimal orderDeliveredQty) {
        this.orderDeliveredQty = orderDeliveredQty;
        return this;
    }

    public OrderItemAmount withBoxUnitOfMeasure(String boxUnitOfMeasure) {
        this.boxUnitOfMeasure = boxUnitOfMeasure;
        return this;
    }

    public OrderItemAmount withOrderUnitOfMeasure(String orderUnitOfMeasure) {
        this.orderUnitOfMeasure = orderUnitOfMeasure;
        return this;
    }

    public OrderItemAmount withUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    public OrderItemAmount withBoxWeight(BigDecimal boxWeight) {
        this.boxWeight = boxWeight;
        return this;
    }

    public BigDecimal compute() {
        return computeOrderAmountFromDeliveredQty()
                .orElse(computeOrderAmountFromRequestedQty());
    }

    private Optional<BigDecimal> computeOrderAmountFromDeliveredQty() {
        if (orderDeliveredQty == null)
            return Optional.empty();

        return Optional.of(unitPrice.multiply(orderDeliveredQty));
    }

    private BigDecimal computeOrderAmountFromRequestedQty() {
        if (orderRequestedQty == null)
            return null;

        //requested quantity is expressed in boxes
        if (boxUnitOfMeasure != null && boxUnitOfMeasure.equals(orderUnitOfMeasure))
            return unitPrice.multiply(orderRequestedQty).multiply(boxWeight);

        return unitPrice.multiply(orderRequestedQty);
    }
}
