package it.dinevo.auth.services;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import it.dinevo.auth.services.annotations.OtpChannel;
import it.dinevo.auth.services.enums.ChannelType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@OtpChannel(ChannelType.EMAIL)
@Named
public class EmailOtpSender implements OtpSender{
    @Inject
    ReactiveMailer mailer;

    @Override
    public Uni<Void> sendOtp(String recipient, String code) {
        // TODO: Make the email more beautiful
        return mailer
                .send(Mail.withText(recipient, "Codice di verifica Dinevo: " + code, "Il tuo codice di verifica per Dinevo: " + code)
                .setFrom("noreply@dinevo.it"));
    }
}
