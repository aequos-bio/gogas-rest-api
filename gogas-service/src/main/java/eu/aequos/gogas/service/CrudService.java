package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.ConvertibleDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public abstract class CrudService<Model, ID> {

    CrudRepository<Model, ID> crudRepository;

    public CrudService(CrudRepository<Model, ID> crudRepository) {
        this.crudRepository = crudRepository;
    }

    public Model create(ConvertibleDTO<Model> dto) {
        return createOrUpdate(null, dto);
    }

    public Model update(ID id, ConvertibleDTO<Model> dto) throws ItemNotFoundException {
        Model existingModel = crudRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item"));

        return createOrUpdate(existingModel, dto);
    }

    public void delete(ID id) {
        crudRepository.deleteById(id);
    }

    private Model createOrUpdate(Model existingModel, ConvertibleDTO<Model> dto) {
        Model updatedModel = dto.toModel(Optional.ofNullable(existingModel));
        return crudRepository.save(updatedModel);
    }
}
