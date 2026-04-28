package com.paymentsserver.filter;

import com.paymentsserver.exception.ExpiredTokenException;
import com.paymentsserver.exception.InvalidTokenException;
import com.paymentsserver.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * Access Token을 검증하여 인증된 사용자 ID를 요청 속성에 저장합니다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private static final String ACCESS_TOKEN_COOKIE_NAME = "ACCESS_TOKEN";
    private static final String AUTHENTICATED_USER_ID_ATTRIBUTE = "authenticatedUserId";

    /**
     * Swagger 및 OPTIONS 요청은 인증 제외
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS 요청 허용 (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Swagger 관련 경로 인증 제외
        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs")) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 쿠키에서 Access Token 추출
            String accessToken = extractTokenFromCookie(request);

            if (accessToken == null) {
                sendUnauthorizedResponse(response, "Access token not found");
                return;
            }

            // 2. JWT 토큰 검증
            jwtUtil.validateToken(accessToken);

            // 3. 토큰에서 사용자 ID 추출
            Long userId = jwtUtil.getUserIdFromToken(accessToken);

            // 4. 요청 속성에 인증된 사용자 ID 저장
            request.setAttribute(AUTHENTICATED_USER_ID_ATTRIBUTE, userId);

            // 5. 다음 필터로 진행
            filterChain.doFilter(request, response);

        } catch (ExpiredTokenException e) {
            sendUnauthorizedResponse(response, "Access token has expired");
        } catch (InvalidTokenException e) {
            sendUnauthorizedResponse(response, "Invalid access token");
        } catch (Exception e) {
            sendUnauthorizedResponse(response, "Authentication error");
        }
    }

    /**
     * 쿠키에서 Access Token 추출
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 401 Unauthorized 응답 전송
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
