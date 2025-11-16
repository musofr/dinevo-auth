package it.dinevo.auth.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "user_establishments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "establishment_id"})
})
public class UserEstablishment extends PanacheEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;
    
    @Column(name = "establishment_id", nullable = false)
    public Long establishmentId;
    
    @Column(nullable = false)
    public String role; // OWNER, MANAGER, EMPLOYEE
    
    public static Uni<List<UserEstablishment>> findByUserId(Long userId) {
        return list("user.id", userId);
    }
    
    public static Uni<List<Long>> getEstablishmentIdsByUserId(Long userId) {
        return find("SELECT ue.establishmentId FROM UserEstablishment ue WHERE ue.user.id = ?1", userId)
            .project(Long.class)
            .list();
    }
}
