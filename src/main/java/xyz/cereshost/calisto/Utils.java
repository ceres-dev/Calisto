package xyz.cereshost.calisto;

import lombok.experimental.UtilityClass;

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
}
