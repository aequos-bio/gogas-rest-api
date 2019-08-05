package eu.aequos.gogas.workflow;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.SupplierOrderItem;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.service.ConfigurationService;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReopenAction extends OrderStatusAction {

    public ReopenAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                        SupplierOrderItemRepo supplierOrderItemRepo, Order order) {

        super(orderItemRepo, orderRepo, supplierOrderItemRepo, order, Order.OrderStatus.Opened);
    }

    @Override
    protected boolean isActionValid() {
        return order.getStatus() == Order.OrderStatus.Closed;
    }

    @Override
    protected void processOrder() {
        orderItemRepo.deleteByOrderAndSummary(order.getId(), true);
        supplierOrderItemRepo.deleteByOrderId(order.getId());
    }
}
