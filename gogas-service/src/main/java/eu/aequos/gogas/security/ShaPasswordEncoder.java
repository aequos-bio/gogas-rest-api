package eu.aequos.gogas.security;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ShaPasswordEncoder implements PasswordEncoder {


    @Override
    public String encode(CharSequence charSequence) {
        return DigestUtils.sha1Hex(charSequence.toString());
    }

    @Override
    public boolean matches(CharSequence charSequence, String encodedPassword) {
        return encode(charSequence).equalsIgnoreCase(encodedPassword);
    }
}
