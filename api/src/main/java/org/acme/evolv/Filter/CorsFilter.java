package org.acme.evolv.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CorsFilter implements ContainerResponseFilter {
  @Override public void filter(ContainerRequestContext req, ContainerResponseContext res) {
    res.getHeaders().putSingle("Access-Control-Allow-Origin", "http://localhost:3000");
    res.getHeaders().putSingle("Access-Control-Allow-Methods", "GET,POST,OPTIONS,DELETE,PUT");
    res.getHeaders().putSingle("Access-Control-Expose-Headers", "Content-Type,Cache-Control");
    res.getHeaders().putSingle("Access-Control-Allow-Headers", "accept, authorization, content-type, x-requested-with,Authorization");
    res.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
    res.getHeaders().putSingle("Access-Control-Max-Age", "86400");
    res.getHeaders().putSingle("Vary", "Origin");
  }
}


    