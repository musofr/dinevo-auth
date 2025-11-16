package it.dinevo.auth.services.annotations;

import it.dinevo.auth.services.enums.ChannelType;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface OtpChannel {
    ChannelType value();

    class Literal extends AnnotationLiteral<OtpChannel> implements OtpChannel {
        private final ChannelType channelType;

        public static Literal of(ChannelType channelType) {
            return new Literal(channelType);
        }

        private Literal(ChannelType channelType) {
            this.channelType = channelType;
        }

        @Override
        public ChannelType value() {
            return channelType;
        }
    }
}
