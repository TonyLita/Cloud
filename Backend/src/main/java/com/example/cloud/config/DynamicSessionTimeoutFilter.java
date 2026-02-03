package com.example.cloud.config;

import com.example.cloud.service.SystemConfigService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DynamicSessionTimeoutFilter implements Filter {

    private final SystemConfigService systemConfigService;

    public DynamicSessionTimeoutFilter(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpSession session = ((HttpServletRequest) request).getSession(false);
            if (session != null) {
                // Récupérer le timeout configuré et l'appliquer à la session actuelle
                int timeoutMinutes = systemConfigService.getSessionTimeoutMinutes();
                session.setMaxInactiveInterval(timeoutMinutes * 60);
            }
        }
        
        chain.doFilter(request, response);
    }
}
