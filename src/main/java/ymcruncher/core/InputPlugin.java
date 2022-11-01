package ymcruncher.core;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Abstract class to be extended by further main.Plugins such as those used
 * to get data from YM or VTX files
 * <p>
 * This class is an attempt to reach an architecture a bit more
 * plugin-friendly than it was ;)
 *
 * @author F-key/RevivaL
 */

public abstract class InputPlugin extends Plugin {

    // Logger
    private static final Logger LOGGER = LogManager.getLogger(InputPlugin.class.getName());

    // List of plugins
    // TODO Move into InputPluginFactory class
    private static final List<InputPlugin> plugins = initPlugins();

    public String strFileType = "unknown";

    /**
     * Return available input plugins
     */
    public static List<InputPlugin> getInputPlugins() {
        return plugins;
    }

    private static List<InputPlugin> initPlugins() {
        ServiceLoader<InputPlugin> serviceLoader = ServiceLoader.load(InputPlugin.class);
        return serviceLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
    }

    /**
     * Return a Chiptune from the inputed <Byte>ArrayList
     *
     * @param arrRawChiptune <Byte>ArrayList of uncompressed chiptune data
     * @param strExt         String Extension of the file (i.e. "mod")
     * @return Chiptune
     */
    abstract protected boolean getPreProcessedChiptune(Chiptune chiptune, ArrayList<Byte> arrRawChiptune, String strExt);

    public boolean loadChiptune(Chiptune chiptune, ArrayList<Byte> arrData, String strExt) {

        // Reject if no file reference in Chiptune
        if (chiptune == null || chiptune.getFile() == null) {
            LOGGER.error("Attempting to load a null chiptune or a chiptune with a null file value");
            return false;
        }

        // Log info into Chiptune
        Tools.clearLog();
        Tools.info("#############");
        Tools.info("# Input Log #");
        Tools.info("#############");

        boolean isChiptuneLoaded = getPreProcessedChiptune(chiptune, arrData, strExt);

        if (isChiptuneLoaded) {
            int intDuration = chiptune.getLength() / chiptune.getPlayRate();
            LOGGER.debug("+ File Informations");
            LOGGER.debug("  - Frames : " + chiptune.getLength());
            LOGGER.debug("  - Time : " + intDuration + " secondes.");
        }

        return isChiptuneLoaded;
    }
}
