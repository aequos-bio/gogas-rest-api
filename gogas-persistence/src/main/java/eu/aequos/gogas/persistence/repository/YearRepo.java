package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Year;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface YearRepo extends CrudRepository<Year, Integer> {
    @Override
    List<Year> findAll();

    List<Year> findYearsByClosedFalseOrderByYearDesc();

    boolean existsYearByYearAndClosed(int year, boolean closed);
}
