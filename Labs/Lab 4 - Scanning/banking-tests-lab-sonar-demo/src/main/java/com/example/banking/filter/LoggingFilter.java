package com.example.banking.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

@Component
public class LoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        System.out.println("Request received: " + httpRequest.getMethod() + " " + httpRequest.getRequestURI());

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null) {
            System.out.println("Authorization header: " + authHeader);
        }

        String cookie = httpRequest.getHeader("Cookie");
        if (cookie != null) {
            System.out.println("Cookie: " + cookie);
        }

        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        for (String name : Collections.list(headerNames)) {
            System.out.println("  " + name + " = " + httpRequest.getHeader(name));
        }

        String query = httpRequest.getQueryString();
        if (query != null) {
            System.out.println("Query string: " + query);
        }

        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            System.out.println("Filter chain failed: " + e.getMessage());
            throw e;
        }
    }
}
