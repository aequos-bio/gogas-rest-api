package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.MenuByRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MenuRepo extends CrudRepository<MenuByRole, String> {

    List<MenuByRole> findByIdRole(String role);

    @Query(value = "SELECT COUNT(m.id) FROM Menu m WHERE m.url = '#AMICI#'")
    int countFriendsMenu();
}
