package dev.cerez.calisto;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipOutputStream;

@Controller
@SpringBootApplication
public class CalistoApplication {
    public static final Config CONFIG = new Config();
    public static final Path ROOT;
    public static final String WEB;
    public static final Logger logger = LoggerFactory.getLogger(CalistoApplication.class);

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
            StringBuilder builderFiles = SectionBuilder.getExplorerList(Arrays.asList(files), path);
            model.addAttribute("files", builderFiles.toString());
            StringBuilder builderPath = SectionBuilder.getExplorerPath(path);
            model.addAttribute("path", builderPath.toString());
            StringBuilder builderGalley = SectionBuilder.getExplorerGallery(path);
            model.addAttribute("gallery", builderGalley.toString());
            return "index";
        }else {
            return "error/404";
        }
    }

    private static @NotNull Path nametoPath(@NotNull String name) {
        return ROOT.resolve(ROOT + name).normalize();
    }

    @ResponseBody
    @GetMapping("/view")
    public ResponseEntity<Resource> view(
            @RequestParam(value = "path", defaultValue = "/") String pathName,
            @RequestParam(value = "quality", defaultValue = "ORIGINAL") String qualityName,
            @RequestParam(value = "resolution", defaultValue = "ORIGINAL") String resolutionName
    ) throws IOException {
        Quality quality;
        Resolution resolution;
        try {
            resolution = WebpBuilder.parseResolution(resolutionName);
            quality = Quality.valueOf(qualityName.toUpperCase());
        }catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        Path path = nametoPath(pathName).toAbsolutePath().normalize();

        if (Utils.checkPathForbidden(path)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (!Utils.isVisible(nametoPath(pathName))) return ResponseEntity.badRequest().build();

        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            return ResponseEntity.notFound().build();
        }else {
            if (WebpBuilder.shouldTransformToWebp(contentType, quality, resolution)) {
                ResponseEntity<Resource> transformed = WebpBuilder.buildWebpResponse(path, quality, resolution);
                if (transformed != null) {
                    return transformed;
                }
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }
    }

    private final DecimalFormat df = new DecimalFormat("###,###,##0.00s");

    @ResponseBody
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam(value = "path", defaultValue = "/") String pathName) {
        Path path = nametoPath(pathName).normalize().toAbsolutePath();
        if (Utils.checkPathForbidden(path)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        logger.info("Downloading: {}", path);
        long time = System.currentTimeMillis();
        if (Files.isDirectory(path)) {
            StreamingResponseBody stream = outputStream -> {
                try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                    zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
                    Utils.zipFolder(path, path.getFileName().toString(), zipOut);
                    logger.info("Download Zip Complete: {} {}", pathName, df.format((System.currentTimeMillis() - time) / 1000f));
                }catch (IOException e) {
                    logger.info("Download Zip Cancel ({}): {} {}", e.getMessage(), pathName, df.format((System.currentTimeMillis() - time) / 1000f));
                }

            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        } else {
            StreamingResponseBody stream = outputStream -> {
                try{
                    Files.copy(path, outputStream);
                }catch (IOException e) {
                    logger.info("Download File Cancel ({}): {} {}", e.getMessage(), pathName, df.format((System.currentTimeMillis() - time) / 1000f));
                }
                logger.info("Download File Complete: {} {}", pathName,df.format((System.currentTimeMillis() - time) / 1000f));
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(
                            SectionBuilder.sizeCacheFiles.computeIfAbsent(path.normalize().toAbsolutePath(), (f) ->
                                    Utils.getFolderSize(f.toFile())))
                    )
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        }
    }
}
