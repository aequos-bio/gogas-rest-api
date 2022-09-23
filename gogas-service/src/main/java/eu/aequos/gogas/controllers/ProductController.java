package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.security.annotations.IsOrderTypeManager;
import eu.aequos.gogas.security.annotations.IsProductManager;
import eu.aequos.gogas.service.ProductService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Api("Products")
@RestController
@RequestMapping("api/products")
public class ProductController {

    private final ProductService productService;

    @ApiOperation(
        value = "Get available products by order type",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="?", description = "?") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = Product.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @GetMapping(value = "list/{productTypeId}/available", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<ProductDTO> getAvailableProducts(@PathVariable String productTypeId) {
        return productService.searchProducts(productTypeId, null, null, null);
    }

    @ApiOperation(
        value = "Search products",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin"), @AuthorizationScope(scope ="order manager", description = "order manager") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = ProductDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @IsOrderTypeManager
    @GetMapping(value = "list/{productTypeId}")
    public List<ProductDTO> listProducts(@PathVariable String productTypeId,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) Boolean available,
                                         @RequestParam(required = false) Boolean cancelled) throws ItemNotFoundException {

        return productService.searchProducts(productTypeId, category, available, cancelled);
    }

    @ApiOperation(
        value = "Create product",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin"), @AuthorizationScope(scope ="order manager", description = "order manager") }) }
    )
    @IsProductManager
    @PostMapping()
    public BasicResponseDTO createProduct(@RequestBody ProductDTO productDTO) {
        String productId = productService.create(productDTO).getId();
        return new BasicResponseDTO(productId);
    }

    @ApiOperation(
        value = "Modify product",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin"), @AuthorizationScope(scope ="order manager", description = "order manager") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: product, Id: <productId>")
    })
    @IsProductManager
    @PutMapping(value = "{productId}")
    public BasicResponseDTO updateProduct(@PathVariable String productId, @RequestBody ProductDTO productDTO) throws ItemNotFoundException {
        String updatedProductId = productService.update(productId, productDTO).getId();
        return new BasicResponseDTO(updatedProductId);
    }

    @ApiOperation(
        value = "Delete product",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin"), @AuthorizationScope(scope ="order manager", description = "order manager") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: product, Id: <productId>")
    })
    @IsProductManager
    @DeleteMapping(value = "{productId}")
    public BasicResponseDTO deleteProduct(@PathVariable String productId) {
        productService.delete(productId);
        return new BasicResponseDTO("OK");
    }

    @ApiOperation(
        value = "Export products to excel file",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin"), @AuthorizationScope(scope ="order manager", description = "order manager") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = String.class),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @IsOrderTypeManager
    @GetMapping(value = "list/{productTypeId}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void generateProductsExcel(HttpServletResponse response, @PathVariable String productTypeId) throws IOException, GoGasException {
        AttachmentDTO excelPriceListAttachment = productService.generateExcelPriceList(productTypeId);
        excelPriceListAttachment.writeToHttpResponse(response);
    }

    @ApiOperation(
        value = "Synchronize products with Aequos",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin"), @AuthorizationScope(scope ="order manager", description = "order manager") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = OrderSynchroInfoDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @IsOrderTypeManager
    @PutMapping(value = "{productTypeId}/sync")
    public OrderSynchroInfoDTO syncExternalProducts(@PathVariable String productTypeId) throws GoGasException {
        return productService.syncPriceList(productTypeId);
    }

    @ApiOperation(
        value = "Import products from excel file",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin"), @AuthorizationScope(scope ="order manager", description = "order manager") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = OrderSynchroInfoDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @IsOrderTypeManager
    @PostMapping(value = "{productTypeId}/import")
    public OrderSynchroInfoDTO importProductsFromExcel(@PathVariable String productTypeId, @RequestParam("file") MultipartFile excelFile) throws IOException, GoGasException {
        byte[] excelFileContent = IOUtils.toByteArray(excelFile.getInputStream());
        String excelType = FilenameUtils.getExtension(excelFile.getOriginalFilename());
        return productService.loadProductsFromExcel(productTypeId, excelFileContent, excelType);
    }

    @ApiOperation(
            value = "Get Unit of Measure available for the product",
            authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin"), @AuthorizationScope(scope ="order manager", description = "order manager") }) }
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = SelectItemDTO.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Item not found. Type: product, Id: <productId>")
    })
    @GetMapping(value = "{productId}/um")
    public List<SelectItemDTO> getProductAvailableUM(@PathVariable String productId) {
        return productService.getAvailableUM(productId);
    }
}
