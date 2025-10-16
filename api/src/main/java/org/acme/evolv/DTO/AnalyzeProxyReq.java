package org.acme.evolv.dto;

public record AnalyzeProxyReq(
        String companyId,
        String fileName,            // fileName
        String model                // Python Model
) {}