package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.ConfigurationItemDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.persistence.entity.Configuration;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConfigurationService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String USER_SORTING_KEY = "visualizzazione.utenti";
    private static final String USER_SORTING_NAME_FIRST = "NC";
    private static final String USER_SORTING_SURNAME_FIRST = "CN";

    private static final String BOX_ROUNDING_THRESOLD_KEY = "colli.soglia_arrotondamento";
    private static final String BOX_ROUNDING_THRESOLD_DEFAULT_STRING = "0.5";
    private static final BigDecimal BOX_ROUNDING_THRESOLD_DEFAULT = new BigDecimal(BOX_ROUNDING_THRESOLD_DEFAULT_STRING);

    public enum UserSorting {
        NameFirst,
        SurnameFirst;
    }

    public enum RoundingMode {
        Threshold,
        Ceil,
        Floor;

        public static RoundingMode getRoundingMode(int code) {
            return RoundingMode.values()[code];
        }
    }

    private ConfigurationRepo configurationRepo;

    public ConfigurationService(ConfigurationRepo configurationRepo) {
        this.configurationRepo = configurationRepo;
    }

    public UserSorting getUserSorting() {
        String sortingConf = configurationRepo.findById(USER_SORTING_KEY)
                .map(Configuration::getValue)
                .orElse(USER_SORTING_NAME_FIRST)
                .toUpperCase();

        return USER_SORTING_SURNAME_FIRST.equals(sortingConf) ? UserSorting.SurnameFirst : UserSorting.NameFirst;
    }

    public BigDecimal getBoxRoundingThreshold() {
        try {
            String boxRoundingThreshold = configurationRepo.findById(BOX_ROUNDING_THRESOLD_KEY)
                    .map(Configuration::getValue)
                    .orElse("0.5");

            return new BigDecimal(boxRoundingThreshold);
        } catch (Exception ex) {
            return BOX_ROUNDING_THRESOLD_DEFAULT;
        }
    }

    public LocalDate parseLocalDate(String date) {
        if (date == null || date.isEmpty())
            return null;

        try {
            return LocalDate.parse(date, formatter);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String formatDate(LocalDate date) {
        return formatter.format(date);
    }

    public List<ConfigurationItemDTO> getVisibleConfigurationItems() {
        return configurationRepo.findByVisibleOrderByKey(true).stream()
                .map(c -> new ConfigurationItemDTO().fromModel(c))
                .collect(Collectors.toList());
    }

    public boolean updateConfigurationItem(ConfigurationItemDTO configurationItem) throws GoGasException {
        if (configurationItem.getKey() == null || configurationItem.getValue() == null)
            throw new GoGasException("Configurazione non valida");

        int itemsUpdated = configurationRepo.updateConfiguration(configurationItem.getKey(), configurationItem.getValue());
        return itemsUpdated == 1;
    }
}
