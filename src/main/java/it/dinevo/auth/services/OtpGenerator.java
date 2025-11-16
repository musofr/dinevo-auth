package it.dinevo.auth.services;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@ApplicationScoped
public class OtpGenerator {
    private static final int OTP_DIGITS = 6;
    private final SecureRandom secureRandom = new SecureRandom();

    public Uni<String> getOtp() {
        StringBuilder otpBuilder = new StringBuilder(OTP_DIGITS);
        for (int i = 0; i < OTP_DIGITS; i++) {
            otpBuilder.append(secureRandom.nextInt(10));
        }
        return Uni.createFrom().item(otpBuilder.toString());
    }
}
