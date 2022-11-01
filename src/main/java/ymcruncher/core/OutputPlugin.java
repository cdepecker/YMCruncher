package ymcruncher.core;

import javafx.beans.property.SimpleDoubleProperty;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;


/**
 * Abstract class to be extended by further main.Plugins such as the one used
 * to crunch chiptunes in AYC format
 * <p>
 * This class is an attempt to reach an architecture a bit more
 * plugin-friendly than it was ;)
 *
 * @author F-key/RevivaL
 */

public abstract class OutputPlugin extends Plugin {

    /* Note on volume output
     *
     * It seems that the formula is something like volume = ((2^0.5)/2)^(15-a) V
     * Where 'a' is from 0-15 (volume level).
     * In fact the volume level is decreased by the half square root 2 each step.
     * i.e.
     * 	(a=15)  volume = ((2^0.5)/2)^(15-15) = ((2^0.5)/2)^0 = 1V
     *  (a=14)  volume = ((2^0.5)/2)^(15-14) = ((2^0.5)/2)^1 = 0.707V
     * */

    // Convert 4bits to 8bits sample (Extracted from STSound by Leonard)
    final protected byte[] arr4to8bits = {0x0, 0x1, 0x2, 0x2, 0x4, 0x6, 0x9, 0xC, 0x11, 0x18, 0x23, 0x30, 0x48, 0x67, (byte) 0xA5, (byte) 0xFF};
    // Convert 8bits to 4bits sample (Given by gwem from the Atari scene)
    final protected byte[] arr8to4bits = {0x0, 0x1, 0x2, 0x3, 0x3, 0x4, 0x5, 0x5, 0x6, 0x6, 0x6, 0x7, 0x7, 0x7, 0x7, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF};

    static private final List<OutputPlugin> plugins = initPlugins();

    // Progression information
    private static final SimpleDoubleProperty completionRatio = new SimpleDoubleProperty(0);

    /**
     * Return available input plugins
     */
    public static List<OutputPlugin> getOutputPlugins() {
        return plugins;
    }

    private static List<OutputPlugin> initPlugins() {
        ServiceLoader<OutputPlugin> serviceLoader = ServiceLoader.load(OutputPlugin.class);
        return serviceLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
    }

    /**
     * Get the completion percentage of the overall compression process
     * Should only be used once the compression process has started.
     *
     * @return Completion percentage
     */
    public static SimpleDoubleProperty getCompletionRatio() {
        return completionRatio;
    }

    /**
     * Set the completion percentage of the overall compression process
     * Should only be used once the compression process has started.
     */
    protected static void setCompletionRatio(int intComp) {
        if ((intComp >= 0) && (intComp <= 100)) {
            completionRatio.set(intComp/100);
        }
    }

    /**
     * Helper function used to convert a BitSet to a Byte
     *
     * @param bs BitSet to be converted
     * @return Byte from BitSet
     */
    protected Byte bsToByte(BitSet bs) {
        byte bytRet = 0;

        // Loop through bits
        for (byte b = 7; b > 0; b--) {
            bytRet |= bs.get(b) ? 1 : 0;
            bytRet <<= 1;
        }
        bytRet |= bs.get(0) ? 1 : 0;

        return bytRet;
    }

    /**
     * Set a variable in an Array of Bytes using Big Endian style
     *
     * @param arrBytes  ArrayList<Byte> where the var should be written to
     * @param lngVar    long variable to insert into the array
     * @param bytNbbytes int Number of bytes to insert (cannot be>4)
     */
    protected void addBEvar(ArrayList<Byte> arrBytes, long lngVar, int bytNbbytes) {
        if (bytNbbytes > 4) return;
        for (int i = 8 * (bytNbbytes - 1); i >= 0; i -= 8) {
            byte bytVal = (byte) ((lngVar >> i) & 0xFF);
            arrBytes.add(bytVal);
        }
    }

    /**
     * Set a variable in an Array of Bytes using little Endian style
     *
     * @param arrBytes  ArrayList<Byte> where the var should be written to
     * @param lngVar    long variable to insert into the array
     * @param bytNbbits int Number of bytes to insert (cannot be>4)
     */
    protected void addLEvar(ArrayList<Byte> arrBytes, long lngVar, int bytNbbits) {
        if (bytNbbits > 4) return;
        for (int i = 0; i <= 8 * (bytNbbits - 1); i += 8) {
            byte bytVal = (byte) ((lngVar >> i) & 0xFF);
            arrBytes.add(bytVal);
        }
    }

    /**
     * Set a variable in an Array of Bytes using little Endian style
     *
     * @param arrBytes  ArrayList<Byte> where the var should be written to
     * @param lngVar    long variable to insert into the array
     * @param bytNbbits int Number of bytes to insert (cannot be>4)
     * @param offset    int that indicate where to set the variable
     */
    protected void setLEvar(ArrayList<Byte> arrBytes, long lngVar, int bytNbbits, int offset) {
        if (bytNbbits > 4) return;
        for (int i = 0; i <= 8 * (bytNbbits - 1); i += 8) {
            byte bytVal = (byte) ((lngVar >> i) & 0xFF);
            arrBytes.set(offset++, bytVal);
        }
    }

    /**
     * Set a variable in an Array of Bytes using little Endian style
     *
     * @param arrBytes  Bytes array that will contain the value
     * @param lngVar    long variable to insert into the array
     * @param bytNbbits int Number of bytes to insert (cannot be>4)
     */
    protected void setLEvar(byte[] arrBytes, int index, long lngVar, int bytNbbits) {
        if (bytNbbits > 4) return;
        for (int i = 0; i <= 8 * (bytNbbits - 1); i += 8) {
            byte bytVal = (byte) ((lngVar >> i) & 0xFF);
            arrBytes[index++] = bytVal;
        }
    }

    /**
     * Set a variable in an Array of Bytes using Big Endian style
     *
     * @param arrBytes         ArrayList<Byte> where the var should be written to
     * @param strToAdd         String Message to add in the array
     * @param isNullTerminated boolean indicating wether or not the String must be NT
     */
    protected void addStringToArray(ArrayList<Byte> arrBytes, String strToAdd, boolean isNullTerminated) {
        byte[] arrChars = strToAdd.getBytes();
        for (byte arrChar : arrChars) arrBytes.add(arrChar);

        if (isNullTerminated) arrBytes.add((byte) 0);

    }

    /**
     * Abstract class that should return the crunched chiptunes
     * This should include the header of the file so that the only action needed
     * by the Core is to persist data in a file
     *
     * @param strDestFile  Path to destination file
     * @param chiptune  input chiptune being crunched
     * @return ArrayList<Byte> which will be persisted
     */
    public abstract ArrayList<Byte> doCrunch(String strDestFile, Chiptune chiptune);

    /**
     * Should return the Plugin extension (i.e. "ayc")
     *
     * @return String fileextension
     */
    public abstract String getExtension();

    /**
     * Wrapper that will call the doCrunch function and store the crunched dqtq
     * destinqtion file
     *
     * @param destination  Path to destination file
     * @param chiptune  input chiptune being crunched
     */
    public void crunchAndStore(String destination, Chiptune chiptune) {
        String strDestFile = destination + "." + getExtension();

        // Data
        ArrayList<Byte> arrCrunchedData = doCrunch(strDestFile, chiptune);

        if (arrCrunchedData != null) {
            try {
                FileOutputStream file_output = new FileOutputStream(strDestFile);

                // Copy data in file
                for (Byte arrCrunchedDatum : arrCrunchedData) {
                    byte bytVal = arrCrunchedDatum;
                    file_output.write(bytVal);
                }

                // Close file
                file_output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Print filename in the console
        System.out.println(" " + strDestFile);

        // reset Completion at 0
        setCompletionRatio(0);

    }
}