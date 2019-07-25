package eu.aequos.gogas.service;

import eu.aequos.gogas.persistence.entity.Configuration;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import org.springframework.stereotype.Service;

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

    public enum UserSorting {
        NameFirst,
        SurnameFirst;
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
}
