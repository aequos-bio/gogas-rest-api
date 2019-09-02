package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.OrderSynchroInfoDTO;
import eu.aequos.gogas.dto.ProductDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.integration.AequosIntegrationService;
import eu.aequos.gogas.integration.api.AequosPriceList;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.persistence.specification.ProductSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService extends CrudService<Product, String> {

    private OrderTypeService orderTypeService;
    private ProductRepo productRepo;
    private PriceListSynchronizer priceListSynchronizer;
    private AequosIntegrationService aequosIntegrationService;

    public ProductService(ProductRepo productRepo, OrderTypeService orderTypeService,
                          PriceListSynchronizer priceListSynchronizer,
                          AequosIntegrationService aequosIntegrationService) {

        super(productRepo, "product");

        this.orderTypeService = orderTypeService;
        this.productRepo = productRepo;
        this.priceListSynchronizer = priceListSynchronizer;
        this.aequosIntegrationService = aequosIntegrationService;
    }

    public List<Product> getProductsOnPriceList(String productType) {
        return fetchProducts(productType, null, true,false, true);
    }

    public List<Product> getProducts(Set<String> productIds) {
        return productRepo.findByIdInOrderByPriceList(productIds);
    }

    public List<ProductDTO> searchProducts(String productType, String category,
                                           Boolean available, Boolean cancelled) throws ItemNotFoundException {

        OrderType orderType = orderTypeService.getRequired(productType);

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

    public OrderSynchroInfoDTO syncPriceList(String orderTypeId) throws GoGasException {
        OrderType orderType = orderTypeService.getRequired(orderTypeId);

        Integer aequosOrderId = Optional.ofNullable(orderType.getAequosOrderId())
                .orElseThrow(() -> new GoGasException("Impossibile sincronizzare il listino: il tipo di ordine non è collegato ad Aequos"));

        AequosPriceList aequosPriceList = aequosIntegrationService.getPriceList(aequosOrderId);
        Date lastSynchro = priceListSynchronizer.syncPriceList(orderType, aequosPriceList);

        return new OrderSynchroInfoDTO(aequosOrderId, lastSynchro);
    }
}
