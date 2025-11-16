package it.dinevo.auth.entity;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import it.dinevo.auth.entity.enums.UserStatus;
import it.dinevo.auth.entity.enums.UserType;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name="users")
public class User extends PanacheEntity {
    @Column(unique = true)
    public String email;
    public String phoneNumber;
    public String password;
    public String displayName;
    @Enumerated(EnumType.STRING)
    public UserStatus userStatus = UserStatus.AWAITING_CONFIRMATION;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public UserType userType = UserType.CUSTOMER;

    public void setPassword(String password) {
        this.password = BcryptUtil.bcryptHash(password);
    }

    public boolean checkPassword(String password) {
        return BcryptUtil.matches(password, this.password);
    }

    public boolean isVerified() {
        return userStatus.equals(UserStatus.CONFIRMED);
    }

    public static Uni<User> getUserByEmailOrPhone(String emailOrPhone) {
        return User.<User>find("email = :emailOrPhone or phoneNumber = :emailOrPhone", Parameters.with("emailOrPhone", emailOrPhone)).singleResult();
    }
    
    public Uni<List<Long>> getEstablishmentIds() {
        if (this.userType != UserType.MERCHANT) {
            return Uni.createFrom().item(List.of());
        }
        return UserEstablishment.getEstablishmentIdsByUserId(this.id);
    }
}
