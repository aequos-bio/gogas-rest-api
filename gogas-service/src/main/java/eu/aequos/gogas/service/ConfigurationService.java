package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.ConfigurationItemDTO;
import eu.aequos.gogas.dto.CredentialsDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.MissingOrInvalidParameterException;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ConfigurationService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String USER_SORTING_KEY = "visualizzazione.utenti";
    private static final String USER_SORTING_NAME_FIRST = "NC";
    private static final String USER_SORTING_SURNAME_FIRST = "CN";

    private static final String BOX_ROUNDING_THRESOLD_KEY = "colli.soglia_arrotondamento";
    private static final String BOX_ROUNDING_THRESOLD_DEFAULT_STRING = "0.5";
    private static final BigDecimal BOX_ROUNDING_THRESOLD_DEFAULT = new BigDecimal(BOX_ROUNDING_THRESOLD_DEFAULT_STRING);

    private static final String GAS_NAME_KEY = "gas.nome";

    public enum UserSorting {
        NameFirst,
        SurnameFirst;
    }

    public enum RoundingMode {
        Threshold,
        Ceil,
        Floor;

        public static RoundingMode getRoundingMode(int code) {
            try {
                return RoundingMode.values()[code];
            } catch (Exception ex) {
                throw new MissingOrInvalidParameterException(String.format("Invalid rounding mode: %s", code));
            }
        }
    }

    private final ConfigurationRepo configurationRepo;

    public UserSorting getUserSorting() {
        String sortingConf = configurationRepo.findValueByKey(USER_SORTING_KEY)
                .orElse(USER_SORTING_NAME_FIRST)
                .toUpperCase();

        return USER_SORTING_SURNAME_FIRST.equals(sortingConf) ? UserSorting.SurnameFirst : UserSorting.NameFirst;
    }

    public BigDecimal getBoxRoundingThreshold() {
        try {
            String boxRoundingThreshold = configurationRepo.findValueByKey(BOX_ROUNDING_THRESOLD_KEY)
                    .orElse("0.5");

            return new BigDecimal(boxRoundingThreshold);
        } catch (Exception ex) {
            return BOX_ROUNDING_THRESOLD_DEFAULT;
        }
    }

    public CredentialsDTO getAequosCredentials() throws GoGasException {
        Optional<String> usernameConf = configurationRepo.findValueByKey("aequos.username");
        Optional<String> passwordConf = configurationRepo.findValueByKey("aequos.password");

        if (usernameConf.isEmpty() || passwordConf.isEmpty() || usernameConf.get().isEmpty() || passwordConf.get().isEmpty())
            throw new GoGasException("Credenziali per Aequos non trovate o non valide, controllare la configurazione");

        CredentialsDTO credentials = new CredentialsDTO();
        credentials.setUsername(usernameConf.get());
        credentials.setPassword(passwordConf.get());
        return credentials;
    }

    public String getGasName() {
        return configurationRepo.findValueByKey(GAS_NAME_KEY)
                .orElse("");
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
