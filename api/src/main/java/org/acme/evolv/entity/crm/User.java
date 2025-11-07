package org.acme.evolv.entity.crm;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false)
    public UUID uuid = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    public String email;

    public String name;
    public String phone;
    public String provider;

    @Column(name = "provider_id")
    public String providerId;

    @Column(name = "password_hash")
    public String pwd;

    @Column(name = "profile_completed")
    public Boolean profileCompleted = false;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();
}