package br.com.forum_hub.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final FiltroTokenAcesso filtroTokenAcesso;

    public SecurityConfiguration(FiltroTokenAcesso filtroTokenAcesso) {
        this.filtroTokenAcesso = filtroTokenAcesso;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(requests -> {
                    requests.requestMatchers("/login", "/atualizar-token", "/registrar", "/verificar-conta").permitAll();
                    requests.requestMatchers(HttpMethod.GET, "/cursos").permitAll();
                    requests.requestMatchers(HttpMethod.GET, "/topicos/**").permitAll();
                    requests.requestMatchers(HttpMethod.POST, "/topicos").hasRole("ESTUDANTE");
                    requests.requestMatchers(HttpMethod.PUT, "/topicos").hasRole("ESTUDANTE");
                    requests.requestMatchers(HttpMethod.DELETE, "/topicos/**").hasRole("ESTUDANTE");
                    requests.requestMatchers(HttpMethod.PATCH, "/topicos/{idTopico}/respostas/**").hasAnyRole("INSTRUTOR", "ESTUDANTE");
                    requests.requestMatchers(HttpMethod.PATCH, "/topicos/**").hasRole("MODERADOR");
                    requests.requestMatchers(HttpMethod.PATCH, "/adicionar-perfil/**").hasRole("ADMIN");
                    requests.requestMatchers(HttpMethod.PATCH, "/reativar-conta/**").hasRole("ADMIN");
                    requests.anyRequest().authenticated();
                })
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(filtroTokenAcesso, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public RoleHierarchy hierarquiaPerfis() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("MODERADOR")
                .role("MODERADOR").implies("ESTUDANTE", "INSTRUTOR")
                .build();
    }
}
