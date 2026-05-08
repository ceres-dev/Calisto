package xyz.cereshost.calisto;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;
import java.util.zip.ZipOutputStream;

@Controller
@SpringBootApplication
public class CalistoApplication {
    public static final Config CONFIG = new Config();
    public static final Path ROOT;
    public static final String WEB;

    static {
        CONFIG.loadIsNotExitedOrLoaded();
        ROOT = CONFIG.getData().getPublicPath().toAbsolutePath().normalize();
        WEB = CONFIG.getData().getWebSite();
    }

    public static void main(String[] args) {
        SpringApplication.run(CalistoApplication.class, args);
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "path", defaultValue = "/") String name, Model model) throws IOException {
        Path path = nametoPath(name);
        if (Utils.checkPathForbidden(path)) return "error/403";
        File file = path.toFile();

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return "error/400";
            StringBuilder builderFiles = getExplorerList(Arrays.asList(files), path);
            model.addAttribute("files", builderFiles.toString());
            StringBuilder builderPath = getExplorerPath(path);
            model.addAttribute("path", builderPath.toString());
            StringBuilder builderGalley = getExplorerGallery(path);
            model.addAttribute("gallery", builderGalley.toString());
            return "index";
        }else {
            return "error/404";
        }
    }

    private static StringBuilder getExplorerGallery(Path path) {
        StringBuilder builderPath = new StringBuilder();
        File file = path.toFile();
        if (!file.exists() || !file.isDirectory()) return builderPath;

        File[] files = file.listFiles();
        if (files == null) return builderPath;
        List<Path> dirs = Arrays.stream(files).map(File::toPath).filter(Utils::isMedia).toList();
        String html = "<a href=\"%s\">%s</a>";

        String img = "<img loading=\"lazy\" alt=\"%1$s\" title=\"%2$s\" class=mediaPreview src=\"%s\">";
        String video = "<video loading=\"lazy\" alt=\"%1$s\" title=\"%2$s\" class=mediaPreview controls><source src=\"%s\"></video>";
        String audio = "<audio loading=\"lazy\" alt=\"%1$s\" title=\"%2$s\" class=mediaPreview controls><source src=\"%s\"></audio>";

        for (Path dir : dirs) {
            String url = "/view?path=" + pathToName(dir);
            builderPath.append(switch (Utils.getTypeMedia(dir)){
                case IMAGE -> html.formatted(url, img.formatted(url, dir.toFile().getName()));
                case VIDEO -> html.formatted(url, video.formatted(url, dir.toFile().getName()));
                case AUDIO -> html.formatted(url, audio.formatted(url, dir.toFile().getName()));
            });
        }
        return builderPath;
    }

    private static @NonNull StringBuilder getExplorerPath(Path path) {
        StringBuilder builderPath = new StringBuilder();
        List<String> dirNames = Arrays.stream(pathToName(path).split("/")).toList();
        builderPath.append("<a href=\"").append("?path=/\">.</a>/");
        if (!dirNames.isEmpty()) {
            for (int i = 1; i < dirNames.size(); i++) {
                builderPath.append("<a href=\"").append("?path=")
                        .append(String.join("/", dirNames.subList(0, i +1)))
                        .append("\">").append(dirNames.get(i)).append("</a>").append("/");
            }
        }
        return builderPath;
    }

    private static @NotNull StringBuilder getExplorerList(@NotNull List<File> files, @NotNull Path path) throws IOException {
        StringBuilder builderFiles = new StringBuilder();
        if (!path.equals(ROOT)) builderFiles.append(buildRow(true, path.getParent().toFile()));
        for (File fil : files.stream().filter(f -> !Utils.isHiddenPath(f.toPath())).toList()) {
            builderFiles.append(buildRow(false, fil));
        }
        return builderFiles;
    }

    private static final WeakHashMap<Path, Long> sizeFiles = new WeakHashMap<>();

    private static @NotNull String buildRow(boolean isParent, File file) throws IOException {
        final String row = "<tr><td>%s</td><td>%s</td><td>%s</td><td><small>%s</small></td></tr>";
        String pathName = pathToName(file.toPath());
        String displayName = isParent ? ".." : file.getName();
        String name;

        if (file.isDirectory()) {
            name = "<a href=\"?path=%s\"><span>%s</span></a>"
                    .formatted(pathName, displayName);
        } else {
            if (Utils.isVisible(file.toPath())) {
                name = "<a href=\"/view?path=%s\"><span>%s</span></a>"
                        .formatted(pathName, displayName);

            }else {
                name = "<span>%s</span>"
                        .formatted(displayName);
            }

        }
        String type = "<img class=icon src=\"icons/%1$s.svg\" alt=\"%s\">".formatted(file.isDirectory() ? "folder" : "file");
        String download = "<a href=\"/download?path=%s\"</a><img class=\"icon iconDownload\" src=\"icons/download.svg\" alt=descargar>".formatted(pathName);
        String size = "<span>%s</span>".formatted(Utils.formatSize(sizeFiles.computeIfAbsent(file.toPath().normalize().toAbsolutePath(), (f) -> Utils.getFolderSize(f.toFile()))));

        return row.formatted(download, type, name, size);
    }

    private static @NotNull String pathToName(@NotNull Path path) {
        String name = (path.startsWith(".") ? path.toString().substring(1) : path).toString()
                .replace(ROOT.toString(), "").replace("\\", "/");
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
    public ResponseEntity<Resource> view(@RequestParam(value = "path", defaultValue = "/") String pathName) throws IOException {
        Path path = nametoPath(pathName).toAbsolutePath().normalize();
        if (Utils.checkPathForbidden(path)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

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
    public ResponseEntity<StreamingResponseBody> download(@RequestParam(value = "path", defaultValue = "/") String pathName) {
        Path path = nametoPath(pathName).toAbsolutePath().normalize();
        if (Utils.checkPathForbidden(path)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

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


}
