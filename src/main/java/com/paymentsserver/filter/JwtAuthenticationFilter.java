package com.paymentsserver.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHENTICATED_USER_ID_ATTRIBUTE = "authenticatedUserId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) return true;
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) return true;
        // Toss 결제 승인 콜백은 사용자 세션이 끊긴 상태(토큰 만료, 다른 탭 등)에서도
        // orderId 기반으로 처리되어야 하므로 인증을 요구하지 않는다.
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/payments/confirm")) return true;

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"Unauthorized\"}");
            return;
        }

        request.setAttribute(AUTHENTICATED_USER_ID_ATTRIBUTE, Long.parseLong(userIdHeader));
        filterChain.doFilter(request, response);
    }
}
