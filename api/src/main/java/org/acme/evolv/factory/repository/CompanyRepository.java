package org.acme.evolv.factory.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.evolv.entity.crm.*;

@ApplicationScoped
public class CompanyRepository implements PanacheRepository<Company> {
    public Company findByName(String name) {
        return find("name", name).firstResult();
    }
}