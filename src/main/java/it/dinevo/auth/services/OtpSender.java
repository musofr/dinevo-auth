package it.dinevo.auth.services;

import io.smallrye.mutiny.Uni;
import it.dinevo.auth.services.enums.ChannelType;
import jakarta.enterprise.util.AnnotationLiteral;

public interface OtpSender {
    Uni<Void> sendOtp(String recipient, String code);
}
