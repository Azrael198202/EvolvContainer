package org.acme.evolv.entity.services;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "service")
public class ServiceEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "service_id", unique = true, nullable = false)
    public UUID service_id = UUID.randomUUID();

    @Column(nullable = false)
    public String name;

    public String url;

    public String description;

    public Long price;

    public Long category_id;

    public Long supplier_id;

    public boolean is_active;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();
}
