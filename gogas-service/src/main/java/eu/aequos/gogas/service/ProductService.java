package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.ProductDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.repository.OrderTypeRepo;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.persistence.specification.ProductSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService extends CrudService<Product, String> {

    private OrderTypeRepo orderTypeRepo;
    private ProductRepo productRepo;

    public ProductService(ProductRepo productRepo, OrderTypeRepo orderTypeRepo) {
        super(productRepo, "product");

        this.orderTypeRepo = orderTypeRepo;
        this.productRepo = productRepo;
    }

    public List<Product> getProductsOnPriceList(String productType) {
        return fetchProducts(productType, null, true,false, true);
    }

    public List<Product> getProducts(Set<String> productIds) {
        return productRepo.findByIdInOrderByPriceList(productIds);
    }

    public List<ProductDTO> searchProducts(String productType, String category,
                                           Boolean available, Boolean cancelled) throws ItemNotFoundException {

        OrderType orderType = orderTypeRepo.findById(productType)
                .orElseThrow(() -> new ItemNotFoundException("Order type", productType));

        return fetchProducts(orderType.getId(), category, available, cancelled, false).stream()
                .map(p -> new ProductDTO().fromModel(p, orderType))
                .collect(Collectors.toList());
    }

    private List<Product> fetchProducts(String productType, String category,
                                       Boolean available, Boolean cancelled,
                                       boolean orderByPriceList) {

        Specification<Product> filter = new SpecificationBuilder<Product>()
                .withBaseFilter(ProductSpecs.type(productType, orderByPriceList))
                .and(ProductSpecs::category, category)
                .and(ProductSpecs::available, available)
                .and(ProductSpecs::cancelled, cancelled)
                .build();

        return productRepo.findAll(filter);
    }
}
