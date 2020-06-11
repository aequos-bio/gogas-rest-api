package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.ProductCategory;
import eu.aequos.gogas.persistence.repository.ProductCategoryRepo;
import eu.aequos.gogas.security.annotations.IsAdmin;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Api("Product categories")
@RestController
@RequestMapping("api/category")
public class ProductCategoryController {

    private ProductCategoryRepo productCategoryRepo;

    public ProductCategoryController(ProductCategoryRepo productCategoryRepo) {
        this.productCategoryRepo = productCategoryRepo;
    }

    @ApiOperation(
        value = "List for dropdown selection",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="any role", description = "any role") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = SelectItemDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @GetMapping(value = "list/{productTypeId}")
    public List<SelectItemDTO> list(@PathVariable String productTypeId) {
         return productCategoryRepo.findByOrderTypeId(productTypeId).stream()
                 .map(c -> new SelectItemDTO(c.getId(), c.getDescription()))
                 .collect(Collectors.toList());
    }

    @ApiOperation(
        value = "Create category",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @IsAdmin
    @PostMapping(value = "{orderTypeId}")
    public BasicResponseDTO createCategory(@PathVariable String orderTypeId, @RequestBody String category) {
        ProductCategory productCategory = new ProductCategory();
        productCategory.setDescription(category);
        productCategory.setOrderTypeId(orderTypeId);

        String productCategoryId = productCategoryRepo.save(productCategory).getId();
        return new BasicResponseDTO(productCategoryId);
    }

    @ApiOperation(
        value = "Update category",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @IsAdmin
    @PutMapping(value = "{orderTypeId}")
    public BasicResponseDTO updateCategory(@PathVariable String orderTypeId, @RequestBody SelectItemDTO category) {
        ProductCategory productCategory = new ProductCategory();
        productCategory.setId(category.getId());
        productCategory.setDescription(category.getDescription());
        productCategory.setOrderTypeId(orderTypeId);

        String productCategoryId = productCategoryRepo.save(productCategory).getId();
        return new BasicResponseDTO(productCategoryId);
    }

    @ApiOperation(
        value = "Delete category",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @IsAdmin
    @DeleteMapping(value = "{categoryId}")
    public BasicResponseDTO deleteCategory(@PathVariable String categoryId) {
        productCategoryRepo.deleteById(categoryId);
        return new BasicResponseDTO("OK");
    }
}
