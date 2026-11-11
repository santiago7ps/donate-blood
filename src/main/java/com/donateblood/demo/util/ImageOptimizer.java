package com.donateblood.demo.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.web.multipart.MultipartFile;

/**
 * Reduce y comprime una imagen subida para que se vea bien pero pese poco.
 * Recorta en cuadrado, escala y guarda como JPEG con calidad controlada.
 */
public final class ImageOptimizer {

    private ImageOptimizer() {
    }

    public static void guardarOptimizada(MultipartFile file, Path destino,
                                         int lado, float calidad) throws IOException {
        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new IOException("El archivo no es una imagen válida");
        }

        // Recorte central en cuadrado para evitar deformación
        int menor = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - menor) / 2;
        int y = (original.getHeight() - menor) / 2;
        BufferedImage recorte = original.getSubimage(x, y, menor, menor);

        // Escalado de alta calidad
        BufferedImage salida = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salida.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, lado, lado);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(recorte, 0, 0, lado, lado, null);
        g.dispose();

        // Escritura JPEG con calidad controlada (0.8 = buen equilibrio)
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(calidad);

        Files.createDirectories(destino.getParent());
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(destino.toFile())) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(salida, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
