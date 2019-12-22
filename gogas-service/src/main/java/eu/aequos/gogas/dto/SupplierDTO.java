package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.Supplier;
import lombok.Data;

import java.util.Optional;

@Data
public class SupplierDTO implements ConvertibleDTO<Supplier> {

    private String id;

    @JsonProperty("ragionesociale")
    private String name;

    @JsonProperty("provincia")
    private String province;

    @JsonProperty("idesterno")
    private String externalId;

    @Override
    public SupplierDTO fromModel(Supplier supplier) {
        this.id = supplier.getId();
        this.name = supplier.getName();
        this.province = supplier.getProvince();
        this.externalId = supplier.getExternalId();

        return this;
    }

    @Override
    public Supplier toModel(Optional<Supplier> existingModel) {
        Supplier model = existingModel.orElse(new Supplier());
        model.setName(this.name);
        model.setProvince(this.province);
        model.setExternalId(this.externalId);

        return model;
    }
}
