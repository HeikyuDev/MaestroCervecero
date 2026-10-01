package com.github.heikyudev.maestrocervecero.presentation.controller.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.auditoria.IAuditLogService;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller de la bitácora de auditoría. Es de solo lectura (consulta con filtros y paginación) y
 * solo la ve el ADMINISTRADOR.
 */
@Controller
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditLogController {

    private static final String VISTA_LISTA = "auditoria/auditoria-lista";
    private static final int TAMANIO_PAGINA = 20;

    /**
     * La bitácora es transaccional: se lista con la entrada más reciente arriba. El id desempata
     * entradas de un mismo instante para que el orden sea siempre estable.
     */
    private static final Sort ORDEN_MAS_RECIENTE_PRIMERO = Sort.by(
            Sort.Order.desc("fechaHora"),
            Sort.Order.desc("id"));

    /**
     * Fin del día elegido en "Hasta". Se usan milisegundos (y no {@code LocalTime.MAX}) porque la
     * base de datos guarda microsegundos y redondearía 23:59:59.999999999 al día siguiente.
     */
    private static final int NANOS_FIN_DE_DIA = 999_000_000;

    private final IAuditLogService auditLogService;

    /**
     * Lista las entradas de la bitácora, paginadas de a {@value #TAMANIO_PAGINA}, la más reciente
     * primero. Los criterios son opcionales y se combinan entre sí. Las fechas se informan sin hora:
     * "Desde" incluye todo ese día desde las 00:00 y "Hasta" lo incluye hasta el final.
     * <p>
     * Si el rango de fechas está invertido, se muestra el aviso del service y una lista vacía en lugar
     * de redirigir, para no perder los filtros que el usuario ya eligió.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String listarAuditoria(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                                  @RequestParam(required = false) String username,
                                  @RequestParam(required = false) AccionAuditoria accion,
                                  @RequestParam(required = false) ConceptoAuditoria concepto,
                                  @RequestParam(defaultValue = "0") int pagina,
                                  Model model) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, ORDEN_MAS_RECIENTE_PRIMERO);
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(23, 59, 59, NANOS_FIN_DE_DIA) : null;

        Page<AuditLogResponseDTO> logs;
        try {
            logs = auditLogService.filtrarAuditLogs(desde, hasta, accion, concepto, vacioANulo(username), pageable);
        } catch (ReglaNegocioException e) {
            logs = new PageImpl<>(List.of(), pageable, 0);
            model.addAttribute("mensaje", e.getMessage());
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }

        model.addAttribute("logs", logs);
        model.addAttribute("acciones", AccionAuditoria.values());
        model.addAttribute("conceptos", ConceptoAuditoria.values());
        model.addAttribute("filtroFechaDesde", fechaDesde);
        model.addAttribute("filtroFechaHasta", fechaHasta);
        model.addAttribute("filtroUsername", username);
        model.addAttribute("filtroAccion", accion);
        model.addAttribute("filtroConcepto", concepto);
        return VISTA_LISTA;
    }

    private static String vacioANulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
