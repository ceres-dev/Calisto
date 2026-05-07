package xyz.cereshost.calisto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("./config.json");

    @Getter
    private Data data = null;

    public void load() {
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            data = GSON.fromJson(reader, Data.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(Objects.requireNonNullElse(data, new Data()), writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isLoaded() {
        return data != null;
    }

    public void loadIsNotExitedOrLoaded() {
        if (!CONFIG_FILE.exists()) {
            save();
        }
        if (!isLoaded()) {
            load();
        }
    }

    @Setter
    public static class Data {
        private String publicPath = Paths.get("./public").toString();

        @SuppressWarnings("ResultOfMethodCallIgnored")
        Data(){
            File file = Path.of(publicPath).normalize().toFile();
            if (!file.isDirectory()) {
                file.mkdirs();
            }
        }

        public Path getPublicPath() {
            return Path.of(publicPath);
        }
    }

}
