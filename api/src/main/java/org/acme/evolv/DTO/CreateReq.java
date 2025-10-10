package org.acme.evolv.DTO;

public record CreateReq(String companyId, String name, String stream, Integer port) {}