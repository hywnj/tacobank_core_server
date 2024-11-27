package com.almagest_dev.tacobank_core_server.infrastructure.security.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtAuthenticationFilter::doFilterInternal");

        // 토큰 추출
        String token = resolveToken(request);
        log.info("JwtAuthenticationFilter::doFilterInternal - token: " + token);


        // 토큰 유효성 검증
        if (token != null && jwtProvider.validateToken(token)) {
            String username = jwtProvider.getUsernameFromToken(token); // 클레임에서 사용자 정보 추출
            log.info("JwtAuthenticationFilter::doFilterInternal - username from token: " + username);

            // 권한 추출해서 request에 저장
            List<String> roles = jwtProvider.getRolesFromToken(token);
            request.setAttribute("roles", roles);
            Collection<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            log.info("JwtAuthenticationFilter::doFilterInternal - roles from token: " + roles);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Header 에서 토큰 추출 ('Bearer' 방식)
     */
    private String resolveToken(HttpServletRequest request) {
        log.info("JwtAuthenticationFilter::resolveToken");
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
