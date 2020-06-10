package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.dto.SupplierDTO;
import eu.aequos.gogas.persistence.entity.Supplier;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.service.SupplierService;
import io.swagger.annotations.Api;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api("Suppliers")
@RestController
@RequestMapping("api/supplier")
public class SupplierController {

    private SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping(value = "select", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<SelectItemDTO> listSuppliersForSelect() {
        return supplierService.getSuppliersForSelect();
    }

    @IsAdmin
    @GetMapping(value = "list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<SupplierDTO> getAllSuppliers() {
        return supplierService.getAllSuppliers();
    }

    @IsAdmin
    @PostMapping
    public BasicResponseDTO createSupplier(@RequestBody SupplierDTO supplierDTO) {
        Supplier supplier = supplierService.create(supplierDTO);
        return new BasicResponseDTO(supplier.getId());
    }

    @IsAdmin
    @PutMapping(value = "{supplierId}")
    public BasicResponseDTO updateReason(@PathVariable String supplierId, @RequestBody SupplierDTO supplierDTO) {
        Supplier supplier =  supplierService.update(supplierId, supplierDTO);
        return new BasicResponseDTO(supplier.getId());
    }

    @IsAdmin
    @DeleteMapping(value = "{supplierId}")
    public BasicResponseDTO deleteReason(@PathVariable String supplierId) {
        supplierService.delete(supplierId);
        return new BasicResponseDTO("OK");
    }
}
