package org.acme.evolv.dto;

public record CreateResp(
        String companyId,
        String name,
        int port,
        String container,
        String image,
        String url,
        String logs
) {}