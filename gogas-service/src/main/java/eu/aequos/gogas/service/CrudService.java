package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.ConvertibleDTO;
import eu.aequos.gogas.exception.ItemNotDeletableException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

@Slf4j
public abstract class CrudService<M, K> {

    protected abstract CrudRepository<M, K> getCrudRepository();
    protected abstract String getType();

    public M getRequired(K key) throws ItemNotFoundException {
        return getCrudRepository().findById(key)
                .orElseThrow(() -> new ItemNotFoundException(getType(), key));
    }

    public M create(ConvertibleDTO<M> dto) {
        return createOrUpdate(null, dto);
    }

    public M update(K key, ConvertibleDTO<M> dto) throws ItemNotFoundException {
        return createOrUpdate(getRequired(key), dto);
    }

    public void delete(K key) {
        try {
            log.info("deleting " + getType() + " with id " + key);
            getCrudRepository().deleteById(key);
        } catch (EmptyResultDataAccessException ex) {
            throw new ItemNotFoundException(getType(), key);
        } catch (DataIntegrityViolationException ex) {
            log.info("Cannot delete " + getType() + " with id " + key + "(" + ex.getMessage() + ")");
            throw new ItemNotDeletableException(getType(), key);
        }
    }

    protected M createOrUpdate(M existingModel, ConvertibleDTO<M> dto) {
        M updatedModel = dto.toModel(Optional.ofNullable(existingModel));
        return getCrudRepository().save(updatedModel);
    }
}
