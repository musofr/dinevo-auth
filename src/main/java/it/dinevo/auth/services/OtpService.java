package it.dinevo.auth.services;

import io.smallrye.mutiny.Uni;
import it.dinevo.auth.services.annotations.OtpChannel;
import it.dinevo.auth.services.enums.ChannelType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class OtpService {
    @Inject
    @Any
    private Instance<OtpSender> senders;
    @Inject
    private OtpGenerator otpGenerator;

    public Uni<Void> sendOtp(ChannelType channel, String recipient, String code) {
        OtpSender sender = senders.select(OtpChannel.Literal.of(channel)).get();
        return sender.sendOtp(recipient, code);
    }
}
