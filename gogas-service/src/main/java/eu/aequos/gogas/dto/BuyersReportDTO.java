package eu.aequos.gogas.dto;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.BuyersReportService;
import eu.aequos.gogas.service.ConfigurationService;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Getter
@AllArgsConstructor
public class BuyersReportDTO {

    private String[] orderDates;
    private String[] buyers;
    private int[][] reportBody;

    public static class Builder {
        private List<Order> orders;
        private List<BuyersReportService.Buyer> buyers;

        public Builder() {
            buyers = new ArrayList<>();
        }

        public Builder withSortedOrderDates(List<Order> orderDates) {
            this.orders = orderDates;
            return this;
        }

        public Builder withSortedBuyers(List<BuyersReportService.Buyer> buyers) {
            this.buyers = buyers;
            return this;
        }

        public BuyersReportDTO build() {
            String[] ordersArray = orders.stream().map(this::formatDeliveryDate)
                    .toArray(String[]::new);

            String[] buyersArray = buyers.stream().map(BuyersReportService.Buyer::getName)
                    .toArray(String[]::new);

            int[][] reportBody = generateReportBody();

            return new BuyersReportDTO(ordersArray, buyersArray, reportBody);
        }

        private String formatDeliveryDate(Order order) {
            return ConfigurationService.getDateFormat().format(order.getDeliveryDate());
        }

        private int[][] generateReportBody() {
            return buyers.stream()
                    .map(this::generateReportRow)
                    .toArray(int[][]::new);
        }

        private int[] generateReportRow(BuyersReportService.Buyer buyer) {
            IntStream ordersCount = IntStream.of(buyer.getOrderDates().size());

            IntStream orderFlags = orders.stream()
                    .mapToInt(order -> buyer.hasOrder(order.getId()));

            return IntStream.concat(ordersCount, orderFlags)
                    .toArray();
        }
    }
}
