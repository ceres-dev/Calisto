package dev.cerez.calisto;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

@UtilityClass
public class SectionBuilder {

    public @NotNull StringBuilder getExplorerList(@NotNull List<File> files, @NotNull Path path) throws IOException {
        StringBuilder builderFiles = new StringBuilder();
        if (!path.equals(CalistoApplication.ROOT)) builderFiles.append(buildRow(true, path.getParent().toFile()));
        for (File fil : files.stream().sorted(Comparator.comparing(File::isFile).thenComparing(
                file -> file.getName().toLowerCase()
        )).filter(f -> !Utils.isHiddenPath(f.toPath())).toList()) {
            builderFiles.append(buildRow(false, fil));
        }
        return builderFiles;
    }


    public StringBuilder getExplorerGallery(Path path) {
        StringBuilder builderPath = new StringBuilder();
        File file = path.toFile();
        if (!file.exists() || !file.isDirectory()) return builderPath;

        File[] files = file.listFiles();
        if (files == null) return builderPath;
        List<Path> dirs = Arrays.stream(files).map(File::toPath).filter(Utils::isMedia).toList();
        String html = "<div class=\"galleryItem\"> <a href=\"%s\">%s</a>%s</div>";

        String img = "<img loading=\"lazy\" alt=\"%1$s\" title=\"%2$s\" class=mediaPreview src=\"%1$s\">";
        String video = "<video loading=\"lazy\" alt=\"%1$s\" title=\"%2$s\" class=mediaPreview controls><source src=\"%1$s\"></video>";
        String audio = "<audio loading=\"lazy\" alt=\"%1$s\" title=\"%2$s\" class=mediaPreview controls><source src=\"%1$s\"></audio>";

        for (Path dir : dirs) {
            String url = "/view?path=" + pathToName(dir);
            String urlLow = "/view?path=" + pathToName(dir) + "&resolution=SD&quality=HIGH";
            String download =
                    "<a href=\"/download?path=%s\"><img class=\"icon iconDownload downloadBtn\" src=\"icons/download.svg\" alt=descargar></a>"
                            .formatted(pathToName(dir));
            builderPath.append(switch (Utils.getTypeMedia(dir)){
                case IMAGE -> html.formatted(url, img.formatted(urlLow, dir.toFile().getName()), download);
                case VIDEO -> html.formatted(url, video.formatted(urlLow, dir.toFile().getName()), download);
                case AUDIO -> html.formatted(url, audio.formatted(urlLow, dir.toFile().getName()), download);
            });
        }
        return builderPath;
    }

    public @NonNull StringBuilder getExplorerPath(Path path) {
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

    public final WeakHashMap<Path, Long> sizeCacheFiles = new WeakHashMap<>(32, 0.5f);

    private @NotNull String buildRow(boolean isParent, File file) throws IOException {
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
        String size = "<span>%s</span>".formatted(Utils.formatSize(sizeCacheFiles.computeIfAbsent(file.toPath().normalize().toAbsolutePath(), (f) -> Utils.getFolderSize(f.toFile()))));

        return row.formatted(download, type, name, size);
    }

    private @NotNull String pathToName(@NotNull Path path) {
        String name = (path.startsWith(".") ? path.toString().substring(1) : path).toString()
                .replace(CalistoApplication.ROOT.toString(), "").replace("\\", "/");
        if (name.isEmpty()) {
            return "/";
        } else {
            return name;
        }
    }
}
