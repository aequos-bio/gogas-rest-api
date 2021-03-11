package eu.aequos.gogas.excel.order;

import lombok.Data;

import java.util.List;

@Data
public class OrderExportRequest {
    List<ProductExport> products;
    List<UserExport> users;
    List<OrderItemExport> userOrder;
    List<SupplierOrderItemExport> supplierOrder;
    boolean friends;
    boolean addWeightColumns;
}
