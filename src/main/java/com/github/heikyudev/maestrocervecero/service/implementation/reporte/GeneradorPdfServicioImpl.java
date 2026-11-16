package com.github.heikyudev.maestrocervecero.service.implementation.reporte;

import com.github.heikyudev.maestrocervecero.service.interfaces.reporte.IGeneradorPdfServicio;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FSFontUseCase;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

/**
 * Implementación de {@link IGeneradorPdfServicio} con OpenHTMLtoPDF: Thymeleaf arma el XHTML del reporte y la librería
 * lo pagina y lo convierte a PDF, con los gráficos SVG dibujados como vectores.
 * <p>
 * La fuente de los reportes es Liberation Sans (equivalente métrico de Arial, con todos los caracteres del español),
 * que se toma del propio PDFBox y se incrusta en cada documento, de modo que el PDF se ve igual en cualquier equipo.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GeneradorPdfServicioImpl implements IGeneradorPdfServicio {

    private static final String RECURSO_FUENTE = "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf";
    private static final Locale LOCALE = Locale.forLanguageTag("es-AR");

    private final ITemplateEngine templateEngine;

    /** Los bytes de la fuente, leídos una sola vez del recurso. */
    private volatile byte[] fuenteIncrustada;

    @Override
    public byte[] generar(String plantilla, Map<String, Object> modelo) {
        // 1. Thymeleaf arma el XHTML del reporte
        String xhtml = templateEngine.process(plantilla, new Context(LOCALE, modelo));

        // 2. OpenHTMLtoPDF lo convierte a PDF con la fuente incrustada
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(() -> new ByteArrayInputStream(bytesDeLaFuente()), FUENTE, 400, FontStyle.NORMAL, true,
                    EnumSet.of(FSFontUseCase.DOCUMENT, FSFontUseCase.SVG));
            builder.useSVGDrawer(new BatikSVGDrawer());
            builder.withHtmlContent(xhtml, null);
            builder.toStream(salida);
            builder.run();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la plantilla " + plantilla, e);
        }
    }

    private byte[] bytesDeLaFuente() {
        byte[] bytes = fuenteIncrustada;
        if (bytes == null) {
            synchronized (this) {
                bytes = fuenteIncrustada;
                if (bytes == null) {
                    bytes = leerFuente();
                    fuenteIncrustada = bytes;
                }
            }
        }
        return bytes;
    }

    private static byte[] leerFuente() {
        try (InputStream fuente = GeneradorPdfServicioImpl.class.getResourceAsStream(RECURSO_FUENTE)) {
            if (fuente == null) {
                throw new IllegalStateException("No se encontró la fuente de los reportes impresos");
            }
            return fuente.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la fuente de los reportes impresos", e);
        }
    }
}
