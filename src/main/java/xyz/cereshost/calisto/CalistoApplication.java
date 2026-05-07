package xyz.cereshost.calisto;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;
import java.util.zip.ZipOutputStream;

@Controller
@SpringBootApplication
public class CalistoApplication {

    private static final Path ROOT = Paths.get("./").toAbsolutePath().normalize();
    private static final String WEB = "http://localhost:8080";

    public static void main(String[] args) {
        SpringApplication.run(CalistoApplication.class, args);
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "file", defaultValue = "/") String name, Model model) throws IOException {
        Path path = nametoPath(name);
        if (checkPathForbidden(path)) return "error/403";
        File file = path.toFile();

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return "error/400";
            StringBuilder builderFiles = getExplorerList(Arrays.asList(files), path);
            model.addAttribute("files", builderFiles.toString());
            StringBuilder builderPath = getExplorerPath(path);
            model.addAttribute("path", builderPath.toString());
            return "index";
        }else {
            return "error/404";
        }
    }

    private static @org.jspecify.annotations.NonNull StringBuilder getExplorerPath(Path path) {
        StringBuilder builderPath = new StringBuilder();
        List<String> dirNames = Arrays.stream(pathToName(path).split("/")).toList();
        builderPath.append("<a href=\"").append(WEB).append("/?file=/\">.</a>");
        if (dirNames.isEmpty()) {
            builderPath.append("/");
        }else {
            for (int i = 0; i < dirNames.size(); i++) {
                builderPath.append("<a href=\"").append(WEB).append("?file=")
                        .append(String.join("/", dirNames.subList(0, i +1)))
                        .append("\">").append(dirNames.get(i)).append("</a>").append("/");
            }
        }
        return builderPath;
    }

    private static @NotNull StringBuilder getExplorerList(@NotNull List<File> files, @NotNull Path path) throws IOException {
        StringBuilder builderFiles = new StringBuilder();
        if (!path.equals(ROOT)) builderFiles.append(buildRow(true, path.getParent().toFile()));
        for (File fil : files) {
            builderFiles.append(buildRow(false, fil));
        }
        return builderFiles;
    }

    private static final WeakHashMap<Path, Long> sizeFiles = new WeakHashMap<>();

    private static @NotNull String buildRow(boolean isParent, File file) throws IOException {
        final String row = "<tr><td>%s</td><td>%s</td><td><small>%s</small></td></tr>";
        String pathName = pathToName(file.toPath());
        String displayName = isParent ? ".." : file.getName();
        String first;

        if (file.isDirectory()) {
            first = "<a href=?file=%s><img class=icon src=\"icons/%2$s.svg\" alt=\"%s\"><span>%s</span></a>"
                    .formatted( pathName, "folder", displayName);
        } else {
            if (isVisible(file.toPath())) {
                first = "<a href=/view?file=%s><img class=icon src=\"icons/%2$s.svg\" alt=\"%s\"><span>%s</span></a>"
                        .formatted(pathName, "file", displayName);

            }else {
                first = "<span><img class=icon src=\"icons/%1$s.svg\" alt=\"%s\"><span>%s</span></span>"
                        .formatted("file", displayName);
            }

        }
        String second = "<a href=\"/download?file=%s\"</a><img class=\"icon iconDownload\" src=\"icons/download.svg\" alt=descargar>".formatted(pathName);
        String threed = "<span>%s</span>".formatted(Utils.formatSize(sizeFiles.computeIfAbsent(file.toPath().normalize().toAbsolutePath(), (f) -> Utils.getFolderSize(f.toFile()))));

        return row.formatted(first, second, threed);
    }

    private static @NotNull String pathToName(@NotNull Path path) {
        String name = (path.startsWith(".") ? path.toString().substring(1) : path).toString().replace(ROOT.toString(), "").replace("\\", "/");
        if (name.isEmpty()) {
            return "/";
        } else {
            return name;
        }
    }

    private static @NotNull Path nametoPath(@NotNull String name) {
        return ROOT.resolve(ROOT + name).normalize();
    }

    @ResponseBody
    @GetMapping("/view")
    public ResponseEntity<Resource> view(@RequestParam String file) throws IOException {
        Path path = nametoPath(file).toAbsolutePath().normalize();
        if (checkPathForbidden(path)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            return ResponseEntity.notFound().build();
        }else {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }
    }

    @ResponseBody
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String file) {
        Path path = nametoPath(file).toAbsolutePath().normalize();
        if (checkPathForbidden(path)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (Files.isDirectory(path)) {
            StreamingResponseBody stream = outputStream -> {
                try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                    Utils.zipFolder(path, path.getFileName().toString(), zipOut);
                }
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + path.getFileName() + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        } else {
            StreamingResponseBody stream = outputStream -> Files.copy(path, outputStream);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + path.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        }
    }

    private static boolean checkPathForbidden(Path path) {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        return !path.startsWith(root);
    }

    public static boolean isVisible(Path path) throws IOException {
        return Files.probeContentType(path) != null;
    }
}
