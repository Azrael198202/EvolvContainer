package org.acme.evolv.controls;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.acme.evolv.dto.ServiceCompanyDTO;
import org.acme.evolv.dto.ServiceDTO;
import org.acme.evolv.entity.services.ServiceCompany;
import org.acme.evolv.entity.services.ServiceEntity;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/services")
public class ServicesResource {

    @GET
    @Path("/list")
    public List<ServiceDTO> servicesList() {
        List<ServiceEntity> entities = ServiceEntity.findAll().list();

        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        List<ServiceDTO> dtoList = entities.stream()
                .map(this::convertServiceToDTO)
                .collect(Collectors.toList());

        return dtoList;
    }

    @GET
    @Path("/company/list")
    public List<ServiceCompanyDTO> companyList(@PathParam("companyUUID") String companyUUID) {
        List<ServiceCompany> s = ServiceCompany.findById(companyUUID);
        if (s == null)
            throw new NotFoundException();

        List<ServiceCompanyDTO> dtoList = s.stream()
                .map(this::convertServiceCompanyToDTO)
                .collect(Collectors.toList());

        return dtoList;
    }

    private ServiceDTO convertServiceToDTO(ServiceEntity entity) {
        ServiceDTO dto = new ServiceDTO(
            UUID.fromString(entity.service_id.toString()),
            entity.name,
            entity.url,
            entity.description,
            entity.price,
            entity.category_id,
            entity.supplier_id,
            entity.is_active
            );
        return dto;
    }

    private ServiceCompanyDTO convertServiceCompanyToDTO(ServiceCompany entity) {
        ServiceCompanyDTO dto = new ServiceCompanyDTO(
            UUID.fromString(entity.service.id.toString()),
            entity.service.name,
            entity.service.url,
            entity.service.description);
        return dto;
    }
}
