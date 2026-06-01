package dev.cerez.calisto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@SpringBootApplication
public class CalistoApplication {

    public static final Config CONFIG = new Config();
    public static final Path ROOT;
    public static final Path ROOT_CACHE;
    public static final String WEB;
    public static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);

    static {
        CONFIG.loadIsNotExitedOrLoaded();
        ROOT = CONFIG.getData().getPublicPath().toAbsolutePath().normalize();
        ROOT_CACHE = Path.of(CONFIG.getData().getPublicPath().toString() + "_cache").toAbsolutePath().normalize();
        WEB = CONFIG.getData().getWebSite();
        File root = ROOT.toFile();
        if (!root.exists()){
            //noinspection ResultOfMethodCallIgnored
            root.mkdirs();
        }
        File rootCache = ROOT_CACHE.toFile();
        if (!rootCache.exists()){
            //noinspection ResultOfMethodCallIgnored
            rootCache.mkdirs();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(CalistoApplication.class, args);
    }
}
