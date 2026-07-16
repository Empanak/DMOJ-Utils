package com.empanak.dmojutils.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final Path FILE_PATH = Paths.get(System.getProperty("user.home"), ".dmojutils", "config.yaml");
    private static AppConfig appConfig;

    private static final List<ConfigChangeListener> listeners = new ArrayList<>();
    public static void addConfigChangeListener(ConfigChangeListener listener) {
        listeners.add(listener);
    }

    public static boolean saveConfig(AppConfig config) {
        DumperOptions options = new DumperOptions();
        Representer representer = new Representer(options);
        representer.addClassTag(AppConfig.class, Tag.MAP);
        Yaml yaml = new Yaml(representer, options);
        try{
            if(FILE_PATH.getParent() != null){
                Files.createDirectories(FILE_PATH.getParent());
            }
            try(FileWriter writer = new FileWriter(FILE_PATH.toFile())){
                yaml.dump(config, writer);
                for(ConfigChangeListener listener : listeners){
                    listener.onConfigChanged(config);
                }
                return true;
            }
        }
        catch (IOException e) {
            return false;
        }
    }

    public static AppConfig getConfig() {
        if (appConfig == null) {
            appConfig = loadFromDisk();
        }
        return appConfig;
    }

    private static AppConfig loadFromDisk() {
        File file = FILE_PATH.toFile();
        if(!file.exists()){
            return createDefaultConfig();
        }

        LoaderOptions options = new LoaderOptions();
        TagInspector inspector = tag -> tag.getClassName().equals(AppConfig.class.getName());
        options.setTagInspector(inspector);
        Yaml yaml = new Yaml(new Constructor(AppConfig.class, options));
        try(InputStream input = new FileInputStream(file)){
            AppConfig config = yaml.loadAs(input, AppConfig.class);
            return config != null ? config : createDefaultConfig();
        } catch (Exception e) {
            System.out.println("Error al cargar la configuración. Usando configuración por defecto");
            return createDefaultConfig();
        }
    }

    private static AppConfig createDefaultConfig(){
        AppConfig config = new AppConfig();
        config.setApiURL("");
        config.setApiToken("");
        return config;
    }
}
