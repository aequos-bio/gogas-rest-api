package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.dto.SupplierDTO;
import eu.aequos.gogas.persistence.entity.Supplier;
import eu.aequos.gogas.persistence.repository.SupplierRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SupplierService extends CrudService<Supplier, String> {

    private SupplierRepo supplierRepo;

    public SupplierService(SupplierRepo supplierRepo) {
        super(supplierRepo,"supplier");

        this.supplierRepo = supplierRepo;
    }

    public List<SupplierDTO> getAllSuppliers() {
        return supplierRepo.findAllByOrderByName().stream()
                .map(s -> new SupplierDTO().fromModel(s))
                .collect(Collectors.toList());
    }

    public List<SelectItemDTO> getSuppliersForSelect() {
        Stream<Supplier> reasons = supplierRepo.findAllByOrderByName().stream();

        return ListConverter.fromStream(reasons)
                .toSelectItems(this::toSelectItem, true, "Selezionare un produttore...");
    }

    private SelectItemDTO toSelectItem(Supplier supplier) {
        return new SelectItemDTO(supplier.getId(), supplier.getName());
    }
}
