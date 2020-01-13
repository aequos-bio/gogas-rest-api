package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.OrderSynchroInfoDTO;
import eu.aequos.gogas.dto.ProductDTO;
import eu.aequos.gogas.excel.ExcelServiceClient;
import eu.aequos.gogas.excel.products.ExcelPriceList;
import eu.aequos.gogas.excel.products.ExtractProductsResponse;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.integration.AequosIntegrationService;
import eu.aequos.gogas.integration.api.AequosPriceList;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.persistence.specification.ProductSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import eu.aequos.gogas.service.pricelist.PriceListSynchronizer;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private ExcelServiceClient excelServiceClient;

    public ProductService(ProductRepo productRepo, OrderTypeService orderTypeService,
                          PriceListSynchronizer priceListSynchronizer,
                          AequosIntegrationService aequosIntegrationService,
                          ExcelServiceClient excelServiceClient) {

        super(productRepo, "product");

        this.orderTypeService = orderTypeService;
        this.productRepo = productRepo;
        this.priceListSynchronizer = priceListSynchronizer;
        this.aequosIntegrationService = aequosIntegrationService;
        this.excelServiceClient = excelServiceClient;
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
        LocalDateTime lastSynchro = priceListSynchronizer.syncPriceList(orderType, aequosPriceList);

        return new OrderSynchroInfoDTO(lastSynchro)
                .withAequosOrderId(aequosOrderId);
    }

    public OrderSynchroInfoDTO loadProductsFromExcel(String orderTypeId, byte[] excelFileStream, String extension) throws GoGasException {
        OrderType orderType = orderTypeService.getRequired(orderTypeId);

        ExtractProductsResponse response = excelServiceClient.extractProducts(excelFileStream, extension);

        if (response.getError() != null) {
            throw new GoGasException(response.getError().getCompleteMessage());
        }

        ExcelPriceList excelPriceList = new ExcelPriceList(response.getPriceListItems());
        LocalDateTime lastSynchro = priceListSynchronizer.syncPriceList(orderType, excelPriceList);

        return new OrderSynchroInfoDTO(lastSynchro)
                .withUpdatedProducts(response.getPriceListItems().size());
    }
}
