package org.acme.evolv.DTO;

public record CreateAppResp(String name, int port, String container, String image, String url, String logs) {}