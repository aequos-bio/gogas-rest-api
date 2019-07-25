package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.OrderType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderTypeRepo extends CrudRepository<OrderType, String> {

    List<OrderType> findAll();
}
