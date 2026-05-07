package xyz.cereshost.calisto;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@UtilityClass
public class Utils {
    public void zipFolder(Path folder, String parentName, ZipOutputStream zipOut) throws IOException {
        try (Stream<Path> paths = Files.list(folder)) {
            for (Path path : paths.toList()) {

                String entryName = parentName + "/" + path.getFileName();

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
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
