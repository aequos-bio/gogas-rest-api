package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.Supplier;
import eu.aequos.gogas.persistence.entity.ProductCategory;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.Optional;

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;

@ApiModel("Product")
@Data
public class ProductDTO implements ConvertibleDTO<Product> {

    @ApiModelProperty(accessMode = READ_ONLY)
    private String id;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty("descrizione")
    private String description;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty("unitamisura")
    private String um;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty("pesocassa")
    private BigDecimal boxWeight;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty("prezzounitario")
    private BigDecimal price;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty("idtipo")
    private String typeId;

    @ApiModelProperty(accessMode = READ_ONLY)
    @JsonProperty("nometipo")
    private String typeName;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty("ordinabile")
    private boolean available;

    @JsonProperty("unitamisuracollo")
    private String boxUm;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty("idproduttore")
    private String supplierId;

    @ApiModelProperty(accessMode = READ_ONLY)
    @JsonProperty("nomeproduttore")
    private String supplierName;

    @JsonProperty("annullato")
    private boolean cancelled;

    @JsonProperty("note")
    private String notes;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty("idcategoria")
    private String categoryId;

    @ApiModelProperty(accessMode = READ_ONLY)
    @JsonProperty("nomecategoria")
    private String categoryName;

    @JsonProperty("cadenza")
    private String frequency;

    @JsonProperty("codiceesterno")
    private String externalId;

    @JsonProperty("solocollointero")
    private boolean boxOnly;

    @JsonProperty("multiplo")
    private BigDecimal multiple;

    @JsonProperty("ordineaequos")
    private boolean aequosOrder;

    @Override
    public ProductDTO fromModel(Product product) {
        aequosOrder = false; //TODO: set proper value
        available = product.isAvailable();
        boxOnly = product.isBoxOnly();
        boxUm = product.getBoxUm();
        boxWeight = product.getBoxWeight();
        cancelled = product.isCancelled();
        categoryId = product.getCategory().getId().toLowerCase();
        categoryName = product.getCategory().getDescription();
        supplierId = product.getSupplier().getId().toLowerCase();
        supplierName = product.getSupplier().getName();
        description = product.getDescription();
        externalId = product.getExternalId();
        frequency = product.getFrequency();
        id = product.getId().toLowerCase();
        multiple = product.getMultiple();
        notes = product.getNotes();
        price = product.getPrice();
        um = product.getUm();
        typeId = product.getType().toLowerCase();

        return this;
    }

    public ProductDTO fromModel(Product product, OrderType orderType) {
        fromModel(product);
        typeName = orderType.getDescription();

        return this;
    }

    @Override
    public Product toModel(Optional<Product> existingProduct) {
        Product model = existingProduct.orElse(new Product());
        
        model.setAvailable(available);
        model.setBoxOnly(boxOnly);
        model.setBoxUm(boxUm);
        model.setBoxWeight(boxWeight);
        model.setCancelled(cancelled);
        model.setCategory(new ProductCategory().withId(categoryId));
        model.setDescription(description);
        model.setExternalId(externalId);
        model.setFrequency(frequency);
        model.setMultiple(multiple);
        model.setNotes(notes);
        model.setPrice(price);
        model.setUm(um);
        model.setType(typeId);
        model.setSupplier(new Supplier().withId(supplierId));

        return model;
    }
}
