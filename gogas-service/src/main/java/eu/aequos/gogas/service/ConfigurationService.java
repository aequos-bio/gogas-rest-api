package eu.aequos.gogas.service;

import eu.aequos.gogas.persistence.entity.Configuration;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class ConfigurationService {

    private static final DateFormat format = new SimpleDateFormat("dd/MM/yyyy");

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


    public Date parseDate(String date) {
        if (date == null || date.isEmpty())
            return null;

        try {
            return format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static DateFormat getDateFormat() {
        return format;
    }
}
