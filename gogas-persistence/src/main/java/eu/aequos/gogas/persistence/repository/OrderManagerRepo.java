package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.OrderManager;
import eu.aequos.gogas.persistence.entity.Product;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface OrderManagerRepo extends CrudRepository<OrderManager, String> {

    List<OrderManager> findByUser(String user);

    List<OrderManager> findByOrderType(String orderType);

    List<OrderManager> findByUserAndOrderType(String user, String orderType);
}
