package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.SupplierOrderItem;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SupplierOrderItemRepo extends CrudRepository<SupplierOrderItem, String> {

    List<SupplierOrderItem> findByOrderId(String orderId);
}
