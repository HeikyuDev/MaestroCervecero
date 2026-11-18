package com.github.heikyudev.maestrocervecero.configuration.security;

import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.time.Duration;

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
@Configuration          // Le dice a Spring "esta clase define Beans, cárguela al arrancar"
@EnableWebSecurity      // Activa toda la infraestructura de Spring Security (filtros, etc)
@EnableMethodSecurity   // Permite usar @PreAuthorize("hasRole('ADMIN')") en los @Service o @Controller
public class SecurityConfig {

    /**
     * Cadena propia de la pantalla pública con la que los proveedores responden una solicitud de cotización
     * ({@code /cotizacion/**}). Se evalúa antes que la cadena general.
     * <p>
     * Es pública: el enlace del correo identifica al proveedor con su token, sin iniciar sesión. No usa la sesión de la
     * aplicación (un proveedor que deja el formulario abierto más de lo que dura una sesión no termina en la pantalla
     * de inicio de sesión): el token CSRF del formulario viaja en una cookie propia en lugar de la sesión, y la
     * protección CSRF sigue habilitada.
     * </p>
     */
    @Bean
    @Order(1)
    public SecurityFilterChain cotizacionProveedorFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/cotizacion/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.csrfTokenRepository(cookieCsrfCotizacion()))
                // El enlace lleva el token del proveedor: ningún pedido a otro sitio informa la dirección de la pantalla
                .headers(headers -> headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .build();
    }

    /**
     * Repositorio del token CSRF de la pantalla pública de cotización: una cookie propia, no legible desde JavaScript,
     * limitada a {@code /cotizacion} y enviada solo en navegación del mismo sitio. Lleva el atributo {@code Secure}
     * cuando el pedido llega por HTTPS; detrás de un proxy que termina el HTTPS, la aplicación debe confiar en sus
     * cabeceras {@code X-Forwarded-*} ({@code server.forward-headers-strategy}) para reconocerlo.
     */
    private static CookieCsrfTokenRepository cookieCsrfCotizacion() {
        CookieCsrfTokenRepository repositorio = new CookieCsrfTokenRepository();
        repositorio.setCookiePath("/cotizacion");
        repositorio.setCookieCustomizer(cookie -> cookie.sameSite("Strict"));
        return repositorio;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry,
                                                   SesionesCerradasPorRestablecimiento sesionesCerradasPorRestablecimiento) throws Exception {
        return http
                // === AUTORIZACIÓN: quién puede entrar a qué URL ===
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos (CSS/JS/imágenes): no requieren login,
                        // si no, ni siquiera se vería bien la pantalla de login
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/vendor/**", "/favicon.ico").permitAll()

                        // La página de login y la de error deben ser públicas,
                        // si no se genera un loop infinito (pide login para ver el login)
                        .requestMatchers("/login", "/error").permitAll()

                        // El restablecimiento de la contraseña olvidada lo usa quien no puede iniciar sesión
                        .requestMatchers("/recuperar-contrasena", "/restablecer-contrasena").permitAll()

                        // Todo lo demás exige estar autenticado
                        .anyRequest().authenticated()
                )

                // === CSRF: se deja HABILITADO a propósito (es el default de Spring Security) ===
                // En una API REST sin estado se suele deshabilitar porque no hay cookies de sesión
                // que un sitio malicioso pueda "montar" en un request falso. Aquí SÍ hay sesión
                // basada en cookie, así que CSRF es indispensable. No hace falta agregar el
                // input hidden a mano en login.html: Spring Security registra automáticamente
                // un RequestDataValueProcessor (CsrfRequestDataValueProcessor) que Thymeleaf usa
                // al renderizar cualquier <form th:action="...">, inyectando el token solo.

                // === LOGIN: formulario clásico (no API REST con tokens) ===
                .formLogin(form -> form
                        .loginPage("/login")              // Vista Thymeleaf de login
                        .defaultSuccessUrl("/home", true)  // El "true" fuerza SIEMPRE ir a /home,
                        // aunque el usuario haya intentado entrar
                        // antes a otra URL protegida
                        .failureUrl("/login?error=true")   // Redirige aquí si falla user/pass.
                        // Nota: DaoAuthenticationProvider oculta por defecto (hideUserNotFoundExceptions=true)
                        // si el problema fue "usuario inexistente" o "contraseña incorrecta": siempre
                        // llega aquí el mismo error genérico, evitando que alguien pueda enumerar usuarios.
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
                        // de morir) NO se configura aquí: es una propiedad del contenedor de servlets,
                        // ver server.servlet.session.timeout en application.properties.
                        .maximumSessions(1)                   // Un usuario, una sesión activa a la vez
                        .sessionRegistry(sessionRegistry)     // El mismo registro con el que se cierran las sesiones de un usuario
                        // A dónde va una sesión vencida: /login?restablecida si se cerró por un restablecimiento de
                        // contraseña, /login?expired si fue "expulsada" por una nueva
                        .expiredSessionStrategy(new EstrategiaSesionVencida(sesionesCerradasPorRestablecimiento))
                )
                .build();
    }

    // Registro de las sesiones activas de cada usuario. Lo usa maximumSessions(1) y, al restablecer una contraseña,
    // CierreSesionesServicioImpl para cerrar las sesiones abiertas con la contraseña anterior.
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // Sesiones cerradas por un restablecimiento de contraseña, para avisar el motivo al volver al inicio de sesión. Una
    // marca dura lo mismo que una sesión inactiva
    @Bean
    public SesionesCerradasPorRestablecimiento sesionesCerradasPorRestablecimiento(
            @Value("${server.servlet.session.timeout:30m}") Duration tiempoInactividadSesion) {
        return new SesionesCerradasPorRestablecimiento(tiempoInactividadSesion);
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

    // Aquí se le indica a Spring: "para autenticar, use MI UserDetailsService + este PasswordEncoder"
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
