package topology;

import backtype.storm.Config;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Intern04 on 14/8/2014.
 */
public class StormConfigManager {
    public static Config readConfig(String path) throws FileNotFoundException {
        Config config = new Config();
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(new File(path));
        HashMap<String, Object> map = (HashMap<String, Object>)yaml.load(inputStream);
        config.putAll(map);
        return config;
    }
    public static int getInt(Map<String, Object> config, String key) {
        Object obj = config.get(key);
        if (obj instanceof Integer)
            return (Integer)obj;
        return ((Long)config.get(key)).intValue();
    }
    public static String getString(Map<String, Object> config, String key) {
        return (String)config.get(key);
    }
    public static List<String> getListOfStrings(Map<String, Object> config, String key) {
        return (List<String>)config.get(key);
    }
}
