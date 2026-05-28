package dev.cerez.calisto;

import com.luciad.imageio.webp.WebPWriteParam;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;

@UtilityClass
public class WebpBuilder {

    public boolean shouldTransformToWebp(String contentType, Quality quality, Resolution resolution) {
        return contentType.startsWith("image/")
                && (quality != Quality.ORIGINAL || resolution != Resolution.ORIGINAL);
    }

    public Resolution parseResolution(String value) {
        String normalized = value.toUpperCase(Locale.ROOT);
        if ("FHD".equals(normalized)) {
            return Resolution.FULL_HD;
        }
        return Resolution.valueOf(normalized);
    }

    public static @NotNull Resource buildWebpResource(Path path, Quality quality, Resolution resolution) throws IOException {
        BufferedImage source = ImageIO.read(path.toFile());
        if (source == null) {
            throw new IOException("Failed to read image from: " + path);
        }

        BufferedImage imageForWrite = resizeByResolutionIfNeeded(source, resolution);
        byte[] data = encodeWebp(imageForWrite, quality.getQuality());
        return new ByteArrayResource(data);
    }

    private static BufferedImage resizeByResolutionIfNeeded(BufferedImage source, Resolution resolution) {
        if (resolution == Resolution.ORIGINAL) {
            return source;
        }

        long originalPixels = (long) source.getWidth() * source.getHeight();
        long targetPixels = Math.min(originalPixels, resolution.getPixels());
        if (targetPixels >= originalPixels) {
            return source;
        }

        double scale = Math.sqrt((double) targetPixels / originalPixels);
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

        if (width == source.getWidth() && height == source.getHeight()) {
            return source;
        }

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }

    private byte[] encodeWebp(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new IOException("No WEBP writer available");
        }

        ImageWriter writer = writers.next();
        WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType(writeParam.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
        writeParam.setCompressionQuality(quality);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            writer.write(null, new IIOImage(image, null, null), writeParam);
            imageOutputStream.flush();
        } finally {
            writer.dispose();
        }
        return outputStream.toByteArray();
    }
}
