package eu.aequos.gogas.security;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.PasswordGenerator;

import java.util.Arrays;
import java.util.List;

import static org.passay.EnglishCharacterData.*;

public class RandomPassword {

    private static final CharacterData SPECIAL_CHARACTERS = new CharacterData() {
        public String getErrorCode() {
            return "";
        }

        public String getCharacters() {
            return "!@#$%^&*()_+";
        }
    };

    private static final List<CharacterRule> CHARACTER_RULES = Arrays.asList(
        new CharacterRule(LowerCase, 2),
        new CharacterRule(UpperCase, 2),
        new CharacterRule(Digit, 2),
        new CharacterRule(SPECIAL_CHARACTERS, 4)
    );

    public static String generate() {
        PasswordGenerator gen = new PasswordGenerator();
        String password = gen.generatePassword(10, CHARACTER_RULES);
        return password;
    }
}
