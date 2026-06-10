package ufms.facoffe.finance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                filterChain.doFilter(request, response);
                return;
            }
            String base64WithoutSignature = chunks[0] + "." + chunks[1] + ".";
            
            Claims claims = Jwts.parserBuilder()
                    .build()
                    .parseClaimsJwt(base64WithoutSignature)
                    .getBody();

            // Tenta pegar o Subject padrão, se for nulo usa o preferred_username do Keycloak
            String username = claims.getSubject();
            if (username == null && claims.containsKey("preferred_username")) {
                username = claims.get("preferred_username", String.class);
            }

            System.out.println("=== FILTRO PROCESSANDO ===");
            System.out.println("Usuário Identificado: " + username);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // Busca as roles no realm_access.roles do Keycloak
            if (claims.containsKey("realm_access")) {
                Map<?, ?> realmAccess = claims.get("realm_access", Map.class);
                if (realmAccess != null && realmAccess.containsKey("roles")) {
                    Object rolesClaim = realmAccess.get("roles");
                    if (rolesClaim instanceof List) {
                        for (Object role : (List<?>) rolesClaim) {
                            String roleStr = role.toString().toUpperCase();
                            authorities.add(new SimpleGrantedAuthority(roleStr));
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleStr));
                        }
                    }
                }
            }
            
            System.out.println("Autoridades Injetadas: " + authorities);
            System.out.println("=========================");

            if (username != null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}