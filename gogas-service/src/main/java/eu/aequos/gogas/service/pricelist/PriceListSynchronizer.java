package eu.aequos.gogas.service.pricelist;

import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.ProductCategory;
import eu.aequos.gogas.persistence.entity.Supplier;
import eu.aequos.gogas.persistence.repository.OrderTypeRepo;
import eu.aequos.gogas.persistence.repository.ProductCategoryRepo;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.persistence.repository.SupplierRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PriceListSynchronizer {

    private ProductRepo productRepo;
    private SupplierRepo supplierRepo;
    private OrderTypeRepo orderTypeRepo;
    private ProductCategoryRepo productCategoryRepo;

    public PriceListSynchronizer(ProductRepo productRepo, SupplierRepo supplierRepo,
                                 OrderTypeRepo orderTypeRepo, ProductCategoryRepo productCategoryRepo) {

        this.productRepo = productRepo;
        this.supplierRepo = supplierRepo;
        this.orderTypeRepo = orderTypeRepo;
        this.productCategoryRepo = productCategoryRepo;
    }

    @Transactional
    public LocalDateTime syncPriceList(OrderType orderType, ExternalPriceList externalPriceList) {
        Map<String, Supplier> suppliersMap = externalPriceList.getProducts().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> createOrUpdateSupplier(e.getKey(), e.getValue().get(0))));

        Map<String, ProductCategory> categoriesMap = externalPriceList.getCategories().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(ExternalPriceListCategory::getDescription, c -> createOrUpdateCategory(orderType.getId(), c), (v1, v2) -> v2));

        Set<String> synchedProductIds = externalPriceList.getProducts().values().stream()
                .flatMap(Collection::stream)
                .map(p -> createOrUpdateProduct(p, suppliersMap.get(p.getSupplierExternalId()), categoriesMap, orderType.getId()))
                .collect(Collectors.toSet());

        productRepo.setNotAvailableByTypeAndIdNotIn(orderType.getId(), synchedProductIds);

        return updateLastSynchroDate(orderType);
    }

    private Supplier createOrUpdateSupplier(String externalCode, ExternalPriceListItem product) {
        Supplier supplier = supplierRepo.findByExternalId(externalCode)
                .orElse(new Supplier());

        supplier.setName(product.getSupplierName());
        supplier.setProvince(product.getSupplierProvince());
        supplier.setExternalId(externalCode);

        return supplierRepo.save(supplier);
    }

    private ProductCategory createOrUpdateCategory(String orderTypeId, ExternalPriceListCategory externalCategory) {
        ProductCategory category = productCategoryRepo.findByOrderTypeIdAndDescription(orderTypeId, externalCategory.getDescription())
                .orElse(new ProductCategory(orderTypeId, externalCategory.getDescription()));

        category.setPriceListPosition(externalCategory.getPriceListOrder());
        category.setPriceListColor(externalCategory.getPriceListColor());

        return productCategoryRepo.save(category);
    }

    private ProductCategory createOrUpdateCategory(Map<String, ProductCategory> categoriesMap,
                                                   String orderTypeId, String description) {

        ProductCategory category = categoriesMap.get(description);

        if (category == null) {
            category = productCategoryRepo.save(new ProductCategory(orderTypeId, description));
            categoriesMap.put(description, category);
        }

        return category;
    }

    private String createOrUpdateProduct(ExternalPriceListItem externalProduct, Supplier supplier,
                                         Map<String, ProductCategory> categoriesMap, String orderTypeId) {

        Product product = productRepo.findByExternalId(externalProduct.getExternalId())
                .orElse(new Product());

        product.setExternalId(externalProduct.getExternalId());
        product.setDescription(externalProduct.getName());
        product.setUm(externalProduct.getUnitOfMeasure());
        product.setBoxWeight(externalProduct.getBoxWeight());
        product.setPrice(externalProduct.getUnitPrice());
        product.setType(orderTypeId);
        product.setBoxUm(externalProduct.getBoxWeight().compareTo(BigDecimal.ONE) > 0 ? "Cassa" : null);
        product.setSupplier(supplier);
        product.setCategory(createOrUpdateCategory(categoriesMap, orderTypeId, externalProduct.getCategory()));
        product.setCancelled(false);
        product.setAvailable(true);
        product.setNotes(externalProduct.getNotes());
        product.setFrequency(externalProduct.getFrequency());
        product.setBoxOnly(externalProduct.isWholeBoxesOnly());
        product.setMultiple(externalProduct.getMultiple());

        return productRepo.save(product).getId();
    }

    private LocalDateTime updateLastSynchroDate(OrderType orderType) {
        LocalDateTime lastSynchro = LocalDateTime.now();
        orderTypeRepo.setLastSynchroById(orderType.getId(), lastSynchro);
        return lastSynchro;
    }
}
