package com.github.heikyudev.maestrocervecero.configuration.security;

import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Configuración central de seguridad del ERP.
 * <p>
 * Este es un monolito renderizado del lado del servidor con Thymeleaf: NO es una API REST
 * y NO usa tokens JWT. El modelo de autenticación es el clásico de un sitio web tradicional
 * (formulario de login + cookie de sesión {@code JSESSIONID}), por eso toda esta clase está
 * pensada en función de eso: sesiones con estado, CSRF habilitado, y redirecciones HTTP
 * (no respuestas JSON 401/403) como mecanismo de control de acceso.
 * <p>
 * Nota de diseño: esta clase NO usa {@code @RequiredArgsConstructor} ni campos
 * {@code private final} porque no tiene ninguna dependencia inyectada como campo — todo lo
 * que necesita ({@code UserDetailServiceImpl}, {@code PasswordEncoder}) llega como parámetro
 * de los propios métodos {@code @Bean}. Eso es justamente la forma recomendada de inyectar
 * dependencias en una clase {@code @Configuration}, así que no hay nada que "corregir" ahí.
 */
@Configuration          // Le dice a Spring "esta clase define Beans, cargala al arrancar"
@EnableWebSecurity      // Activa toda la infraestructura de Spring Security (filtros, etc)
@EnableMethodSecurity   // Te permite usar @PreAuthorize("hasRole('ADMIN')") en tus @Service o @Controller
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // === AUTORIZACIÓN: quién puede entrar a qué URL ===
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos (CSS/JS/imágenes): no requieren login,
                        // si no, ni siquiera se vería bien la pantalla de login
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()

                        // La página de login y la de error deben ser públicas,
                        // si no generás un loop infinito (te pide login para ver el login)
                        .requestMatchers("/login", "/error").permitAll()

                        // Todo lo demás exige estar autenticado
                        .anyRequest().authenticated()
                )

                // === CSRF: se deja HABILITADO a propósito (es el default de Spring Security) ===
                // En una API REST sin estado se suele deshabilitar porque no hay cookies de sesión
                // que un sitio malicioso pueda "montar" en un request falso. Acá SÍ hay sesión
                // basada en cookie, así que CSRF es indispensable. No hace falta agregar el
                // input hidden a mano en login.html: Spring Security registra automáticamente
                // un RequestDataValueProcessor (CsrfRequestDataValueProcessor) que Thymeleaf usa
                // al renderizar cualquier <form th:action="...">, inyectando el token solo.

                // === LOGIN: formulario clásico (no API REST con tokens) ===
                .formLogin(form -> form
                        .loginPage("/login")              // Tu vista Thymeleaf de login
                        .defaultSuccessUrl("/home", true)  // El "true" fuerza SIEMPRE ir a /home,
                        // aunque el usuario haya intentado entrar
                        // antes a otra URL protegida
                        .failureUrl("/login?error=true")   // Redirige acá si falla user/pass.
                        // Nota: DaoAuthenticationProvider oculta por defecto (hideUserNotFoundExceptions=true)
                        // si el problema fue "usuario inexistente" o "contraseña incorrecta": siempre
                        // llega acá el mismo error genérico, evitando que alguien pueda enumerar usuarios.
                        .permitAll()                       // El login en sí debe ser accesible sin login (obvio pero hay que declararlo)
                )

                // === LOGOUT ===
                .logout(logout -> logout
                        .logoutUrl("/logout")                 // URL que dispara el logout (normalmente vía POST por CSRF)
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)          // Mata la sesión del lado servidor
                        .deleteCookies("JSESSIONID")          // Borra la cookie del navegador
                        .permitAll()
                )

                // === SESIONES ===
                // sessionCreationPolicy explícito: en un monolito con formLogin SIEMPRE hay sesión
                // de servidor (a diferencia de una API stateless con JWT), IF_REQUIRED es lo que
                // ya viene por default, pero se deja explícito para que quede documentada la intención.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/login?timeout")  // Si la cookie de sesión ya no es válida
                        // La DURACIÓN del timeout (cuánto tiempo de inactividad tolera una sesión antes
                        // de morir) NO se configura acá: es una propiedad del contenedor de servlets,
                        // ver server.servlet.session.timeout en application.properties.
                        .maximumSessions(1)                   // Un usuario, una sesión activa a la vez
                        .expiredUrl("/login?expired")         // A dónde va si su sesión fue "expulsada" por una nueva
                )
                .build();
    }

    // Sin este bean, Spring Security nunca se entera cuando una HttpSession muere de verdad
    // (cierre de navegador, expiración natural del contenedor): el SessionRegistry usado por
    // maximumSessions(1) seguiría creyendo que la sesión vieja sigue activa, y el usuario
    // quedaría bloqueado para volver a loguearse hasta que esa entrada se limpie sola.
    // Es un requisito documentado de Spring Security para que el control de concurrencia
    // de sesiones funcione correctamente, no un bean opcional.
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // Bean estándar que expone el AuthenticationManager para que Spring lo use internamente
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // Acá le decís a Spring: "para autenticar, usá MI UserDetailsService + este PasswordEncoder"
    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailServiceImpl userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }

    // BCrypt: algoritmo de hash estándar y recomendado para contraseñas (con salt automático)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
