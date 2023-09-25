package eu.aequos.gogas.service;

import eu.aequos.gogas.attachments.AttachmentService;
import eu.aequos.gogas.dto.AttachmentDTO;
import eu.aequos.gogas.dto.OrderSynchroInfoDTO;
import eu.aequos.gogas.dto.ProductDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static eu.aequos.gogas.dto.SelectItemDTO.valueAsLabel;

@Slf4j
@Service
public class ProductService extends CrudService<Product, String> {

    private OrderTypeService orderTypeService;
    private ProductRepo productRepo;
    private PriceListSynchronizer priceListSynchronizer;
    private AequosIntegrationService aequosIntegrationService;
    private ExcelServiceClient excelServiceClient;
    private ExcelGenerationService reportService;
    private AttachmentService attachmentService;

    public ProductService(ProductRepo productRepo, OrderTypeService orderTypeService,
                          PriceListSynchronizer priceListSynchronizer,
                          AequosIntegrationService aequosIntegrationService,
                          ExcelServiceClient excelServiceClient, ExcelGenerationService reportService,
                          AttachmentService attachmentService) {

        super(productRepo, "product");

        this.orderTypeService = orderTypeService;
        this.productRepo = productRepo;
        this.priceListSynchronizer = priceListSynchronizer;
        this.aequosIntegrationService = aequosIntegrationService;
        this.excelServiceClient = excelServiceClient;
        this.reportService = reportService;
        this.attachmentService = attachmentService;
    }

    public List<Product> getProductsOnPriceList(String productType) {
        return fetchProducts(productType, null, true,false, true);
    }

    public List<Product> getProducts(Set<String> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        return productRepo.findByIdInOrderByPriceList(productIds);
    }

    public List<ProductDTO> searchProducts(String orderTypeId, String category,
                                           Boolean available, Boolean cancelled) throws ItemNotFoundException {

        OrderType orderType = orderTypeService.getRequired(orderTypeId);

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
        return syncPriceList(orderType);
    }

    public OrderSynchroInfoDTO syncPriceList(OrderType orderType) throws GoGasException {
        Integer aequosOrderId = Optional.ofNullable(orderType.getAequosOrderId())
                .orElseThrow(() -> new GoGasException("Impossibile sincronizzare il listino: il tipo di ordine non è collegato ad Aequos"));

        log.info("Updating aequos price list for type {} (aequos id {})", orderType.getId(), aequosOrderId);

        AequosPriceList aequosPriceList = aequosIntegrationService.getPriceList(aequosOrderId);
        LocalDateTime lastSynchro = priceListSynchronizer.syncPriceList(orderType, aequosPriceList);

        return new OrderSynchroInfoDTO(lastSynchro)
                .withAequosOrderId(aequosOrderId);
    }

    public OrderSynchroInfoDTO loadProductsFromExcel(String orderTypeId, byte[] excelFileStream, String extension) throws GoGasException {
        log.info("Updating price list from excel file for type {}", orderTypeId);
        OrderType orderType = orderTypeService.getRequired(orderTypeId);

        ExtractProductsResponse response = excelServiceClient.extractProducts(excelFileStream, extension);
        if (response.getError() != null)
            throw new GoGasException(response.getError().getCompleteMessage());

        ExcelPriceList excelPriceList = new ExcelPriceList(response.getPriceListItems());
        LocalDateTime lastSynchro = priceListSynchronizer.syncPriceList(orderType, excelPriceList);

        return new OrderSynchroInfoDTO(lastSynchro)
                .withUpdatedProducts(response.getPriceListItems().size());
    }

    public AttachmentDTO generateExcelPriceList(String productType) throws GoGasException {
        OrderType orderType = orderTypeService.getRequired(productType);

        byte[] excelContent = reportService.extractProductPriceList(productType);
        String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String fileName = attachmentService.buildFileName(orderType.getDescription(), LocalDate.now(), "", contentType);

        return new AttachmentDTO(excelContent, contentType, fileName);
    }

    public Optional<Product> getByExternalId(String productType, String externalId) {
        return productRepo.findByTypeAndExternalId(productType, externalId);
    }

    public List<SelectItemDTO> getAvailableUM(String productId) {
        Product product = getRequired(productId);

        if (product.getBoxUm() == null)
            return Collections.singletonList(valueAsLabel(product.getUm()));

        if (product.isBoxOnly())
            return Collections.singletonList(valueAsLabel(product.getBoxUm()));

        return Arrays.asList(valueAsLabel(product.getUm()), valueAsLabel(product.getBoxUm()));
    }
}
