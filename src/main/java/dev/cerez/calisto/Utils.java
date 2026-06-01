package dev.cerez.calisto;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static dev.cerez.calisto.CalistoApplication.*;

@UtilityClass
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public void zipFolder(Path folder, String parentName, ZipOutputStream zipOut) throws IOException {
        try (Stream<Path> paths = Files.list(folder)) {
            for (Path path : paths.toList()) {

                String entryName = parentName + "/" + path.getFileName();

                if (path.getFileName().toString().startsWith(".")) {
                    continue;
                }

                if (Files.isDirectory(path)) {
                    zipFolder(path, entryName, zipOut);
                } else {
                    zipOut.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zipOut);
                    zipOut.closeEntry();
                }
            }
        }
    }

    /**
     * Obtienes el tamaño total de una carpetas de forma recursiva
     * @param folder nombre de la carpeta
     * @return cantidad de byte
     */

    public long getFolderSize(@NotNull File folder) {
        long totalSize = 0;

        // Verificar que la ruta es un directorio
        if (!folder.isDirectory()) {
            return folder.length();
        }

        if (folder.exists()) {
            File[] files = folder.listFiles(); // Obtener lista de archivos

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        totalSize += file.length(); // Sumar tamaño del archivo
                    } else if (file.isDirectory()) {
                        totalSize += getFolderSize(file); // Recursividad para subdirectorios
                    }
                }
            }
        }

        return totalSize; // Retornar el tamaño total en bytes
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f%sB", bytes / Math.pow(1024, exp), pre);
    }

    private final Set<String> IMAGE_EXTENSIVE = Set.of(
            "png", "jpg", "jpeg", "gif", "bmp", "svg", "webp"
    );

    private final Set<String> VIDEO_EXTENSIVE = Set.of(
            "mp4", "mkv", "avi"
    );

    private final Set<String> AUDIO_EXTENSIVE = Set.of(
            "mp3", "flv", "wav"
    );

    public boolean isMedia(Path path) {
        File file = path.toFile();
        if (file.exists() && file.isFile() ) {
            String[] raw = file.getName().split("\\.");
            String ext = raw[raw.length - 1].toLowerCase();
            return IMAGE_EXTENSIVE.contains(ext) || VIDEO_EXTENSIVE.contains(ext) || AUDIO_EXTENSIVE.contains(ext);
        }{
            return false;
        }
    }

    public @NotNull TypeMedia getTypeMedia(Path path) {
        String[] raw = path.toFile().getName().split("\\.");
        String ext = raw[raw.length - 1].toLowerCase();
        if (IMAGE_EXTENSIVE.contains(ext)) {
            return TypeMedia.IMAGE;
        }
        if (VIDEO_EXTENSIVE.contains(ext)) {
            return TypeMedia.VIDEO;
        }
        if (AUDIO_EXTENSIVE.contains(ext)) {
            return TypeMedia.AUDIO;
        }
        throw new IllegalArgumentException("Unknown extension: " + ext);
    }

    public enum TypeMedia{
        IMAGE,
        VIDEO,
        AUDIO
    }

    public boolean checkPathForbidden(Path path) {
        if (path.startsWith(ROOT)){
            return false;
        }else {
            logger.warn("Acceso negado: {}", path);
            return true;
        }
    }

    public boolean isVisible(Path path) throws IOException {
        return Files.probeContentType(path) != null;
    }

    public boolean isHiddenPath(Path path) {
        return path.getFileName().toString().toLowerCase().startsWith(".");
    }

    public @NotNull Path nameToPath(@NotNull String name) {
        return ROOT.resolve(ROOT + name).normalize();
    }

    public @NotNull Path nameToPathCache(@NotNull String name) {
        return ROOT_CACHE.resolve(ROOT_CACHE + name).normalize();
    }

    public @NotNull String nameFileToNameFileCache(@NotNull String name, @NotNull Quality quality, Resolution resolution) {
        String[] split = name.split("/");
        String nameFile =  split[split.length - 1];
        return name + "/" + quality.name() + "_" + resolution + "_" + nameFile;
    }

    public @NotNull String nameFileCacheToNameFile(@NotNull String name) {
        String[] split = name.split("_");

        if  (split.length > 2) {
            try {
                Quality quality = Quality.valueOf(split[1]);
                Resolution resolution = Resolution.valueOf(split[2]);
                String formattedName = nameFileToNameFileCache("", quality, resolution);
                return name.replaceFirst(formattedName, "");
            }catch (Exception ignored) {}
        }
        return name;
    }
}
