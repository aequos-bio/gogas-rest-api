package eu.aequos.gogas.workflow;

import eu.aequos.gogas.order.GoGasOrder;
import eu.aequos.gogas.order.OrderStatus;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ShippingCostRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.service.OrderItemService;

import static eu.aequos.gogas.workflow.ActionValidity.notValid;
import static eu.aequos.gogas.workflow.ActionValidity.valid;

public class ReopenAction extends OrderStatusAction {

    private ShippingCostRepo shippingCostRepo;

    public ReopenAction(OrderItemService orderItemService, OrderRepo orderRepo,
                        SupplierOrderItemRepo supplierOrderItemRepo, GoGasOrder order,
                        ShippingCostRepo shippingCostRepo) {

        super(orderItemService, orderRepo, supplierOrderItemRepo, order, OrderStatus.Opened);
        this.shippingCostRepo = shippingCostRepo;
    }

    @Override
    protected ActionValidity isActionValid() {
        if (order.getStatus() != OrderStatus.Closed)
            return notValid("Invalid order status");

        return valid();
    }

    @Override
    protected void processOrder() {
        order.clearOrderManagerData();
        supplierOrderItemRepo.deleteByOrderId(order.getId());
    }
}
