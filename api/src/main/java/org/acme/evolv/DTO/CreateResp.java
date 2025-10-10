package org.acme.evolv.DTO;

public record CreateResp(
        String companyId,
        String name,
        int port,
        String container,
        String image,
        String url,
        String logs
) {}