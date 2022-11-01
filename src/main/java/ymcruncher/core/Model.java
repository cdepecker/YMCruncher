package ymcruncher.core;

// Files

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Logger;

/**
 * Model class of the MVC-framework design
 * The purpose of this package is to process chiptune files (such as YM or VTX) to get small and easy
 * to read chiptunes usable for low memory old computers using AY-3-891x
 * family sound processor.
 * <p>
 * Most of existing chiptune files are compressed using a "lha" or "zip/gzip" compliant
 * compression method. This is not easy to use on low memory computers
 * because the compressed file needs to be loaded entirely in memory
 * in order to be decompressed and played.
 * <p>
 * The aim of this Class is to provide a way to decompress and play
 * such a file "on the fly", thus one will be able to play hours of
 * irritating music ;)
 *
 * @author F-key/RevivaL
 */
public class Model {

    // Logger
    private static final Logger LOGGER = LogManager.getLogger(Model.class.getName());

    // Application Name
    final public static String strApplicationName = "YaMaha Cruncher - " + Tools.properties.getProperty("version");

    // Flags & Filters
    private byte bytReg7Filter = 0x0; // All on
    private final boolean[] arrEnvFilter = new boolean[]{true, true, true};
    private boolean blnConvertTo50Hz = false;
    private boolean blnConvertFrequency = true;
    private boolean blnAdjustFrequency = false;
    private boolean blnNullPeriodDisableChannel = false;

    // 0: SID voice, 1:Digidrum, 2:Sinus SID (TAO), 3: Sync Buzzer (TAO)
    private final HashMap<SpecialFXType, Boolean> arrSpecialFXFilter = new HashMap<>();

    // InputPlugins definition
    private final List<InputPlugin> inputPlugins = InputPlugin.getInputPlugins();

    // OutputPlugins definition
    private List<OutputPlugin> outputPlugins = new ArrayList<OutputPlugin>();
    private OutputPlugin current_OutputPlugin = null;

    // Loaded Chiptunes (indexed by path)
//    private Hashtable<String, Chiptune> hmChiptune = new Hashtable<String, Chiptune>();
    private final ObservableList<Chiptune> chiptuneList = FXCollections.observableArrayList();

    // Processing fields
    private final SimpleDoubleProperty totalProgress = new SimpleDoubleProperty(0);
    private String strFileNameOnlyWithoutExt = "none";

    // Destination Path
    private String destinationPath = new File(".").getAbsolutePath();
    private File sourceFolder = Paths.get(".").toAbsolutePath().normalize().toFile();

    /**
     * Standard Constructor.
     */
    public Model() {
        // Welcome message
        Tools.debug(strApplicationName);

        // Initialize Input Plugin Class
        if ((inputPlugins == null) || (inputPlugins.size() <= 0)) {
            System.out.println("Error : No available Input main.Plugins detected");
            System.exit(-1);
        }

        // Initialize OutputPlugins
        outputPlugins = OutputPlugin.getOutputPlugins();

        // Initialize Current OutputPlugin
        current_OutputPlugin = (OutputPlugin) outputPlugins.get(0);

        // Initialize SpecialFX filters
        arrSpecialFXFilter.put(SpecialFXType.ATARI_SIDVOICE, true);
        arrSpecialFXFilter.put(SpecialFXType.ATARI_DIGIDRUM, true);
        arrSpecialFXFilter.put(SpecialFXType.ATARI_SINSID, true);
        arrSpecialFXFilter.put(SpecialFXType.ATARI_SYNCBUZZER, true);
        arrSpecialFXFilter.put(SpecialFXType.OTHER, true);
    }

    /**
     * Return list of available plugins for the GUI.
     *
     * @return arrOutputPlugins ArrayList of available Output main.Plugins
     */
    public List<OutputPlugin> getOutuptPlugins() {
        return outputPlugins;
    }

    public ObservableList<OutputPlugin> getOutputPluginsObservableList() {
        return FXCollections.observableList(outputPlugins);
    }

    public OutputPlugin getCurrent_OutputPlugin() {
        return current_OutputPlugin;
    }

    public void setCurrent_OutputPlugin(OutputPlugin current_OutputPlugin) {
        this.current_OutputPlugin = current_OutputPlugin;
    }

    public void toggleFilterEnv(int c) {
        arrEnvFilter[c] = !arrEnvFilter[c];
    }

    public void toggleSpecialFXFilter(SpecialFXType c) {
        //arrSpecialFXFilter[c] = !arrSpecialFXFilter[c];
        arrSpecialFXFilter.put(c, !arrSpecialFXFilter.get(c));
    }

    void toggleAdjustFrequeny() {
        blnAdjustFrequency = !blnAdjustFrequency;
    }

    /**
     * Toggle the Register 7 filter bit of the inputed voice.
     *
     * @param mix  int between 0 and 5 inclusive
     *              (0 = voiceA, 1= voiceB, 2=voiceC, 3=noiseA, 4=noiseB, 5=noiseC)
     */
    public void toggleFilterVoice(int mix) {
        if ((mix >= 0) && (mix <= 5))
            bytReg7Filter ^= (1 << mix);
    }

    /**
     * Crunch the hasmap of chiptunes using outputPlugin and store results at destPath.
     */
    public void crunchList() {
        // Used only for display purposes
        int hmChiptuneindex = 0;

        for (Chiptune chiptune: chiptuneList) {

            // Get Full Path
            String strPathChiptune = chiptune.getFile().getAbsolutePath();

            // Debug
            LOGGER.info("Crunching file: " +strPathChiptune);

            chiptune.load(inputPlugins);

            //	Get Destination path
            String strFileNameOnly = chiptune.getFileName();
            strFileNameOnlyWithoutExt = strFileNameOnly.replaceFirst("\\.[^\\.]*$", "");
            String destination = destinationPath + File.separatorChar + strFileNameOnlyWithoutExt;

            // Progression
            totalProgress.set(hmChiptuneindex==0?0:chiptuneList.size()/hmChiptuneindex);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // process Crunching ...
            if (chiptune.isLoaded()) {
                // Add the input Log
                Tools.setLog(chiptune.getSbLog());
                Tools.info("\n##############");
                Tools.info("# Output Log #");
                Tools.info("##############");

                // Clone chiptune and process it
                // + Filter Mixer
                // + Clock Frequency change
                Chiptune cloned_chiptune = chiptune.copyclone();
                cloned_chiptune.filter(blnConvertFrequency, blnAdjustFrequency, Tools.YM_CPC_FREQUENCY, bytReg7Filter, arrEnvFilter, arrSpecialFXFilter);
                LOGGER.debug("chiptune size = " + chiptune.getArrFrame().size());
                LOGGER.debug("Copy chiptune size = " + cloned_chiptune.getArrFrame().size());

                // Convert to 50hz ?
                if (blnConvertTo50Hz) cloned_chiptune.force50Hz();

                // Set SongName & Author Info if not available
                String strSongName = (chiptune.getStrSongName() == null) ? strFileNameOnlyWithoutExt : chiptune.getStrSongName();
                String strAuthorName = (chiptune.getStrAuthorName() == null) ? "" : chiptune.getStrAuthorName();
                cloned_chiptune.setStrSongName(strSongName);
                cloned_chiptune.setStrAuthorName(strAuthorName);

                // Crunch Registers ArrayList
                System.out.print("(+) ");
                current_OutputPlugin.crunchAndStore(destination, cloned_chiptune);

                // Unload the Chiptune from the HashMap (This should free the java heap)
                chiptune.unload();
            } else {
                Tools.info("Cannot decode chiptune.");
                System.out.println("(E) Error processing file : " + strPathChiptune);
            }

            // Log File creation
            FileWriter log_output;
            try {
                log_output = new FileWriter(destination + ".txt", false);
                log_output.write(Tools.getLog().toString());
                log_output.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // increase index
            hmChiptuneindex++;
        }

        // reset current counter
        hmChiptuneindex = 0;
        // FIXME setChanged();
        // FIXME notifyObservers();
    }

    /**
     * Convert to CPC Psg frequency
     * Usefull for ATARI to CPC Conversion
     */
    public void ToggleFrequencyConvert() {
        blnConvertFrequency = !blnConvertFrequency;
    }

    // Force replay frequency to 50hz
    public void ToggleReplayFrequencyConvert() {
        blnConvertTo50Hz = !blnConvertTo50Hz;
    }

    public void ToggleNullPeriodDisableChannel() {
        blnNullPeriodDisableChannel = !blnNullPeriodDisableChannel;
    }

    public ObservableList<Chiptune> getChiptuneList() {
        return chiptuneList;
    }

    public void removeAllChiptunes(ObservableList<Chiptune> selectedItems) {
        chiptuneList.removeAll(selectedItems);
    }

    public void addChiptune(Chiptune chiptune) {
        chiptuneList.add(chiptune);
    }

    public List<InputPlugin> getInputPlugins() {
        return inputPlugins;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    /*************************************************************************************
     * The following part is used only to signal the Position of the Cruncher in the list
     ************************************************************************************/
    public ObservableValue<? extends Number> getTotalProgress() {
        return totalProgress;
    }

    public File getSourceFolder() {
        return sourceFolder;
    }

    public void setSourceFolder(File sourceFolder) {
        this.sourceFolder = sourceFolder;
    }
}
