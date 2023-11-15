package eu.aequos.gogas.service;

import eu.aequos.gogas.attachments.AttachmentService;
import eu.aequos.gogas.attachments.AttachmentType;
import eu.aequos.gogas.dto.AttachmentDTO;
import eu.aequos.gogas.dto.ConfigurationItemDTO;
import eu.aequos.gogas.dto.CredentialsDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.MissingOrInvalidParameterException;
import eu.aequos.gogas.persistence.entity.Configuration;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
    private static final String USER_POSITION_KEY = "users.position";

    private static final String BOX_ROUNDING_THRESOLD_KEY = "colli.soglia_arrotondamento";
    private static final String BOX_ROUNDING_THRESOLD_DEFAULT_STRING = "0.5";
    private static final BigDecimal BOX_ROUNDING_THRESOLD_DEFAULT = new BigDecimal(BOX_ROUNDING_THRESOLD_DEFAULT_STRING);

    private static final String GAS_NAME_KEY = "gas.nome";

    private static final String GAS_LOGO_TYPE = "gas.logo.type";

    @Getter
    @RequiredArgsConstructor
    public enum UserSorting {
        NAME_FIRST(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName)),
        SURNAME_FIRST(Comparator.comparing(User::getLastName).thenComparing(User::getFirstName));

        private final Comparator<User> comparator;
    }

    public enum RoundingMode {
        THRESHOLD,
        CEIL,
        FLOOR;

        public static RoundingMode getRoundingMode(int code) {
            try {
                return RoundingMode.values()[code];
            } catch (Exception ex) {
                throw new MissingOrInvalidParameterException(String.format("Invalid rounding mode: %s", code));
            }
        }
    }

    private final ConfigurationRepo configurationRepo;
    private final AttachmentService attachmentService;

    public UserSorting getUserSorting() {
        String sortingConf = configurationRepo.findValueByKey(USER_SORTING_KEY)
                .orElse(USER_SORTING_NAME_FIRST)
                .toUpperCase();

        return USER_SORTING_SURNAME_FIRST.equals(sortingConf) ? UserSorting.SURNAME_FIRST : UserSorting.NAME_FIRST;
    }

    public Comparator<User> getUserComparatorForOrderExport() {
        if (isUserPositionEnabled()) {
            return Comparator.comparing(User::getPosition);
        }

        return getUserSorting().getComparator();
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

    public boolean isUserPositionEnabled() {
        return configurationRepo.findValueByKey(USER_POSITION_KEY)
                .map(Boolean::parseBoolean)
                .orElse(false);
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

    public List<ConfigurationItemDTO> getGasProperties() {
        return configurationRepo.findByKeyLike("gas%").stream()
                .map(c -> new ConfigurationItemDTO().fromModel(c))
                .collect(Collectors.toList());
    }

    public boolean updateConfigurationItem(ConfigurationItemDTO configurationItem) throws GoGasException {
        if (configurationItem.getKey() == null || configurationItem.getValue() == null)
            throw new GoGasException("Configurazione non valida");

        int itemsUpdated = configurationRepo.updateConfiguration(configurationItem.getKey(), configurationItem.getValue());
        return itemsUpdated == 1;
    }

    public void storeLogo(byte[] logoFileContent, String contentType) {
        attachmentService.storeAttachment(logoFileContent, AttachmentType.LOGO, "logo");
        createOrUpdate(GAS_LOGO_TYPE, contentType, "Content type of logo image", false);
    }

    private Configuration createOrUpdate(String key, String value, String description, boolean visible) {
        Configuration contentTypeConfiguration = configurationRepo.findById(GAS_LOGO_TYPE)
                .orElseGet(() -> buildConfigurationItem(key, description, visible));

        contentTypeConfiguration.setValue(value);

        return configurationRepo.save(contentTypeConfiguration);
    }

    private Configuration buildConfigurationItem(String key, String description, boolean visible) {
        Configuration configuration = new Configuration();
        configuration.setKey(key);
        configuration.setVisible(visible);
        configuration.setDescription(description);
        return configuration;
    }

    public AttachmentDTO readLogo() {
        byte[] content = attachmentService.retrieveAttachment(AttachmentType.LOGO, "logo");
        String contentType = configurationRepo.findValueByKey(GAS_LOGO_TYPE)
                .orElseThrow(() ->new GoGasException("Content type not found for logo image"));

        return attachmentService.buildAttachmentDTO("logo", content, contentType);
    }

    public void removeLogo() {
        attachmentService.removeAttachment(AttachmentType.LOGO, "logo");
        deleteLogoTypeProperty();
    }

    //FOR TESTING PURPOSES
    public void resetProperties() {
        deleteLogoTypeProperty();
    }

    private void deleteLogoTypeProperty() {
        configurationRepo.findById(GAS_LOGO_TYPE)
                .ifPresent(configurationRepo::delete);
    }
}
