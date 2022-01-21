package ymcruncher.core;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class YMCProperties {
    final static InputStream propInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("ymcruncher.properties");

    static Properties properties = new Properties();

    static {
        try {
            properties.load(propInputStream);
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }
}
