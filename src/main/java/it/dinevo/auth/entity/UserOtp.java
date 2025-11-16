package it.dinevo.auth.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class UserOtp extends PanacheEntity {
    public String otp;
    public LocalDateTime createDate;
    public LocalDateTime expiryDate;
    public Boolean used = false;
    @JoinColumn(name = "FK_USER_ID")
    @ManyToOne(optional = false)
    public User user;

    public static Uni<UserOtp> of(User user, String code) {
        UserOtp otpEntity = new UserOtp();
        otpEntity.otp = code;
        otpEntity.createDate = LocalDateTime.now();
        otpEntity.expiryDate = otpEntity.createDate.plusMinutes(5);
        otpEntity.user = user;
        return otpEntity.persistAndFlush();
    }

    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiryDate);
    }

    public static Uni<UserOtp> findValidOtpForUser(Long userId, String otp) {
        return UserOtp.<UserOtp>find("user.id = ?1 and otp = ?2 and used = false and expiryDate > ?3",
                userId, otp, LocalDateTime.now())
            .firstResult();
    }

    public Uni<UserOtp> markAsUsed() {
        this.used = true;
        return this.persistAndFlush();
    }
}
