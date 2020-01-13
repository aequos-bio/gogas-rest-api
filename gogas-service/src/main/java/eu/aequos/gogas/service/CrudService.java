package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.ConvertibleDTO;
import eu.aequos.gogas.exception.ItemNotDeletableException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

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
            crudRepository.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            throw new ItemNotDeletableException(type, id);
        }
    }

    private Model createOrUpdate(Model existingModel, ConvertibleDTO<Model> dto) {
        Model updatedModel = dto.toModel(Optional.ofNullable(existingModel));
        return crudRepository.save(updatedModel);
    }
}
