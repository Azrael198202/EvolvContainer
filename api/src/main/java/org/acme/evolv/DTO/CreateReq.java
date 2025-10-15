package org.acme.evolv.dto;

public record CreateReq(String companyId, String name, String stream, Integer port) {}