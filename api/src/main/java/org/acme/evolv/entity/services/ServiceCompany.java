package org.acme.evolv.entity.services;

import java.time.LocalDateTime;

import org.acme.evolv.entity.crm.Company;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "service_company", uniqueConstraints = @UniqueConstraint(columnNames = {"service_id", "company_id"}))
public class ServiceCompany extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "service_id")
    public ServiceEntity service;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "company_id")
    public Company company;

    public boolean is_active = false;

    @Column(name = "start_active")
    public LocalDateTime startDate = LocalDateTime.now();

    @Column(name = "expiry")
    public LocalDateTime expiryDate = LocalDateTime.now();
}
