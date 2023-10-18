package eu.aequos.gogas.validation;

import eu.aequos.gogas.dto.ProductDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.MissingOrInvalidParameterException;
import eu.aequos.gogas.persistence.repository.ProductCategoryRepo;
import eu.aequos.gogas.service.OrderTypeService;
import eu.aequos.gogas.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class ProductValidator {

    private final SupplierService supplierService;
    private final OrderTypeService orderTypeService;
    private final ProductCategoryRepo productCategoryRepo;

    public void validate(ProductDTO productDTO) {
        validateBoxUM(productDTO.getUm(), productDTO.getBoxUm(), productDTO.getBoxWeight());
        validateExternalKeys(productDTO.getSupplierId(), productDTO.getTypeId(), productDTO.getCategoryId());
    }

    private void validateBoxUM(String unitUM, String boxUM, BigDecimal boxWeight) {
        if (unitUM.equals(boxUM)) {
            throw new MissingOrInvalidParameterException("l'Unità di misura della cassa non può essere uguale a quella dei singoli pezzi");
        }

        if (boxWeight.doubleValue() > 1.0 && boxUM == null) {
            throw new MissingOrInvalidParameterException("l'unità di misura della cassa deve essere specificata");
        }
    }

    private void validateExternalKeys(String supplierId, String orderTypeId, String categoryId) {
        try {
            supplierService.getRequired(supplierId);
            orderTypeService.getRequired(orderTypeId);

            productCategoryRepo.findById(categoryId)
                    .orElseThrow(() -> new ItemNotFoundException("category", categoryId));
        } catch (ItemNotFoundException ex) {
            throw new MissingOrInvalidParameterException(String.format("Invalid %s", ex.getItemType()));
        }
    }
}
