package ymcruncher.core;


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

public abstract class InputPlugin {

    /**
     * Private data needed for further convertion
     */
    public String strFileType = "unknown";

    static private List<InputPlugin> plugins = initPlugins();

    /**
     * Return available input plugins
     */
    @SuppressWarnings("unchecked")
    public static List<InputPlugin> getInputPlugins() {
        return plugins;
    }

    private static List<InputPlugin> initPlugins() {
        ServiceLoader<InputPlugin> serviceLoader = ServiceLoader.load(InputPlugin.class);
        List<InputPlugin> plugins = serviceLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        return plugins;
    }

    /**
     * Return a Chiptune from the inputed <Byte>ArrayList
     *
     * @param arrRawChiptune <Byte>ArrayList of uncompressed chiptune data
     * @param strExt         String Extension of the file (i.e. "mod")
     * @return Chiptune
     */
    abstract protected Chiptune getPreProcessedChiptune(ArrayList arrRawChiptune, String strExt);

    /**
     * Wrapper for the previous function
     *
     * @param arrRawChiptune ArrayList of uncompressed input chiptune data
     * @return Chiptune which hold the input chiptune
     */
    public Chiptune getChiptune(ArrayList arrRawChiptune, boolean blnConvertFrequency, String strExt) {
        // get Chiptune
        Chiptune chiptune = this.getPreProcessedChiptune(arrRawChiptune, strExt);

        // Printing various Informations
        if (chiptune != null) {
            int intDuration = chiptune.getLength() / chiptune.getPlayRate();
            YMC_Tools.debug("+ File Informations");
            YMC_Tools.debug("  - Frames : " + chiptune.getLength());
            YMC_Tools.debug("  - Time : " + intDuration + " secondes.");
        }

        return chiptune;
    }
}
