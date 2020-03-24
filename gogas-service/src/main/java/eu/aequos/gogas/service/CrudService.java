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
public abstract class CrudService<Model, ID> {

    CrudRepository<Model, ID> crudRepository;
    String type;

    public CrudService(CrudRepository<Model, ID> crudRepository, String type) {
        this.crudRepository = crudRepository;
        this.type = type;
    }

    public Model getRequired(ID id) throws ItemNotFoundException {
        return crudRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(type, id));
    }

    public Model create(ConvertibleDTO<Model> dto) {
        return createOrUpdate(null, dto);
    }

    public Model update(ID id, ConvertibleDTO<Model> dto) throws ItemNotFoundException {
        return createOrUpdate(getRequired(id), dto);
    }

    public void delete(ID id) {
        try {
            log.info("deleting " + type + " with id " + id);
            crudRepository.deleteById(id);
        } catch (EmptyResultDataAccessException ex) {
            throw new ItemNotFoundException(type, id);
        } catch (DataIntegrityViolationException ex) {
            log.info("Cannot delete " + type + " with id " + id + "(" + ex.getMessage() + ")");
            throw new ItemNotDeletableException(type, id);
        }
    }

    protected Model createOrUpdate(Model existingModel, ConvertibleDTO<Model> dto) {
        Model updatedModel = dto.toModel(Optional.ofNullable(existingModel));
        return crudRepository.save(updatedModel);
    }
}
