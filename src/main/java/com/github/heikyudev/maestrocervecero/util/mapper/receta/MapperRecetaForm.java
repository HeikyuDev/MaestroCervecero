package com.github.heikyudev.maestrocervecero.util.mapper.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLupuloFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleMaltaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.RecetaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.VersionRecetaFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;

import java.util.ArrayList;

/**
 * MapperRecetaForm tiene la responsabilidad de convertir una {@link RecetaResponseDTO} en el
 * {@link RecetaFormDTO} con el que se precarga el formulario de modificación: los datos de la
 * versión vigente quedan como valores iniciales de cada campo y de cada fila de las tablas.
 */
public class MapperRecetaForm {

    private MapperRecetaForm() {
    }

    /**
     * Convierte la versión vigente de una receta en el DTO de formulario equivalente.
     *
     * @param receta Receta con su versión vigente.
     * @return DTO de formulario precargado, o {@code null} si la receta es nula. Si la receta no
     * tiene versión vigente, devuelve un formulario vacío.
     */
    public static RecetaFormDTO toFormDTO(RecetaResponseDTO receta) {
        if (receta == null) {
            return null;
        }
        VersionRecetaResponseDTO version = receta.getVersion();
        if (version == null) {
            return new RecetaFormDTO();
        }

        VersionRecetaFormDTO versionForm = VersionRecetaFormDTO.builder()
                .nombre(version.getNombre())
                .volumenBase(version.getVolumenBase())
                .relacionDeEmpaste(version.getRelacionDeEmpaste())
                .ogObjetivo(version.getOgObjetivo())
                .fgObjetivo(version.getFgObjetivo())
                .ibuObjetivo(version.getIbuObjetivo())
                .duracionMaceracion(version.getDuracionMaceracion())
                .duracionHervido(version.getDuracionHervido())
                .duracionFermentacion(version.getDuracionFermentacion())
                .duracionMaduracion(version.getDuracionMaduracion())
                .detallesMalta(new ArrayList<>())
                .detallesLupulo(new ArrayList<>())
                .detallesLevadura(new ArrayList<>())
                .build();

        version.getDetallesMalta().forEach(detalle -> versionForm.getDetallesMalta().add(
                DetalleMaltaFormDTO.builder()
                        .idMalta(detalle.getMalta().getId())
                        .cantidad(detalle.getCantidad())
                        .build()));

        version.getDetallesLupulo().forEach(detalle -> versionForm.getDetallesLupulo().add(mapDetalleLupulo(detalle)));

        version.getDetallesLevadura().forEach(detalle -> versionForm.getDetallesLevadura().add(
                DetalleLevaduraFormDTO.builder()
                        .idLevadura(detalle.getLevadura().getId())
                        .cantidad(detalle.getCantidad())
                        .build()));

        return RecetaFormDTO.builder().version(versionForm).build();
    }

    /**
     * El tiempo de hervor solo aplica al uso HERVOR: para los demás usos la entidad lo guarda en 0
     * y el formulario lo deja vacío.
     */
    private static DetalleLupuloFormDTO mapDetalleLupulo(DetalleLupuloResponseDTO detalle) {
        return DetalleLupuloFormDTO.builder()
                .idLupulo(detalle.getLupulo().getId())
                .cantidad(detalle.getCantidad())
                .uso(detalle.getUso())
                .etapaDeUso(detalle.getEtapaDeUso())
                .tiempoDeHervor(detalle.getUso() == UsoLupulo.HERVOR ? detalle.getTiempoDeHervor() : null)
                .build();
    }
}
