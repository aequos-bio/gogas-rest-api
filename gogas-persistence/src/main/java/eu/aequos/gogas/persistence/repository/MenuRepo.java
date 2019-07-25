package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.MenuByRole;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MenuRepo extends CrudRepository<MenuByRole, String> {

    List<MenuByRole> findByIdRole(String role);
}
