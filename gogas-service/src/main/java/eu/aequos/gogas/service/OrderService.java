package eu.aequos.gogas.service;

import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import org.springframework.stereotype.Service;

@Service
public class OrderService extends CrudService<Order, String> {

    private OrderRepo orderRepo;

    public OrderService(OrderRepo orderRepo) {
        super(orderRepo, "order");
        this.orderRepo = orderRepo;
    }

    public Order getRequiredWithType(String id) throws ItemNotFoundException {
        return orderRepo.findByIdWithType(id)
                .orElseThrow(() -> new ItemNotFoundException(type, id));
    }
}
