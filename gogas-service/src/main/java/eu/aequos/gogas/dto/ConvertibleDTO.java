package eu.aequos.gogas.dto;

import java.util.Optional;

public interface ConvertibleDTO<Model> {

    ConvertibleDTO fromModel(Model model);

    Model toModel(Optional<Model> existingModel);
}
