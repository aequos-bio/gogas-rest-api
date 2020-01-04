package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.OrderSynchroInfoDTO;
import eu.aequos.gogas.dto.ProductDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.security.annotations.IsOrderTypeManager;
import eu.aequos.gogas.service.ExcelGenerationService;
import eu.aequos.gogas.service.ProductService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/products")
public class ProductController {

    private ProductRepo productRepo;
    private ProductService productService;
    private ExcelGenerationService reportService;

    public ProductController(ProductRepo productRepo, ProductService productService, ExcelGenerationService reportService) {
        this.productRepo = productRepo;
        this.productService = productService;
        this.reportService = reportService;
    }

    @GetMapping(value = "list/{productType}/available", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<Product> getAvailableProducts(@PathVariable String productType) {
        return productRepo.findAvailableByTypeOrderByPriceList(productType);
    }

    @GetMapping(value = "list/{productType}")
    public List<ProductDTO> listProducts(@PathVariable String productType,
                                         @RequestParam String category,
                                         @RequestParam Boolean available,
                                         @RequestParam Boolean cancelled) throws ItemNotFoundException {

        return productService.searchProducts(productType, category, available, cancelled);
    }

    @GetMapping(value = "list")
    public List<ProductDTO> listProducts() {
        return new ArrayList<>();
    }

    @PostMapping()
    public String create(@RequestBody ProductDTO productDTO) {
        return productService.create(productDTO).getId();
    }

    @PutMapping(value = "{productId}")
    public String update(@PathVariable String productId, @RequestBody ProductDTO productDTO) throws ItemNotFoundException {
        return productService.update(productId, productDTO).getId();
    }

    @DeleteMapping(value = "{productId}")
    public void delete(@PathVariable String productId) {
        productService.delete(productId);
    }


    //@IsOrderTypeManager TODO: find a way to authenticate for download
    @GetMapping(value = "list/{productType}/export")
    public void generateProductsExcel(HttpServletResponse response, @PathVariable String productType) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.getOutputStream().write(reportService.extractProductPriceList(productType));
        response.getOutputStream().flush();
    }

    @IsOrderTypeManager
    @PutMapping(value = "{productType}/sync")
    public OrderSynchroInfoDTO syncExternalProducts(@PathVariable String productType) throws GoGasException {
        return productService.syncPriceList(productType);
    }

    @IsOrderTypeManager
    @PostMapping(value = "{productType}/import")
    public OrderSynchroInfoDTO importProductsFromExcel(@PathVariable String productType, @RequestParam("file") MultipartFile excelFile) throws IOException, GoGasException {
        byte[] excelFileContent = IOUtils.toByteArray(excelFile.getInputStream());
        String excelType = FilenameUtils.getExtension(excelFile.getOriginalFilename());
        return productService.loadProductsFromExcel(productType, excelFileContent, excelType);
    }
}
