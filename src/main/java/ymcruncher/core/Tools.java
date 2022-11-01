package ymcruncher.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

public class Tools {
    // YM Possible Frequency
    final public static long YM_ATARI_FREQUENCY = 2000000L;
    final public static long YM_CPC_FREQUENCY = 1000000L;

    // Static Consts
    final public static String strApplicationName = "YaMaha Cruncher - " + "Version 0.6";
    //final public static byte[] header = {'Y','M','C', 0x02};
    final public static byte CPC_REGISTERS = 14;
    final public static byte YM_REGISTERS = 16;
    final public static byte CPC_REPLAY_FREQUENCY = 50;

    // Crunching Formats
    final public static byte FORMAT_YMC = 0;
    final public static byte FORMAT_AYC = 1;
    final public static byte FORMAT_AYL = 2;
    final public static byte FORMAT_YM = 3;

    // Debug file & Flag
    private static boolean blnVerbose = false;
    private static StringBuffer str_debug = new StringBuffer();

    // Read properties file
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

    public static StringBuffer getLog() {
        // return the Buffer
        return str_debug;
    }

    public static void setLog(StringBuffer sb) {
        // Clear the Buffer
        str_debug = new StringBuffer(sb);
    }

    public static void clearLog() {
        // Clear the Buffer
        str_debug.delete(0, str_debug.length());
    }

    public static void setBlnVerbose(boolean blnVerbose) {
        Tools.blnVerbose = blnVerbose;
    }

    /**
     * Helper function to test verbosity
     *
     * @return boolean indicatin whether or not the output will be verbose
     */
    public static boolean isVerbose() {
        return blnVerbose;
    }

    /**
     * Helper function to log information
     *
     * @param strMsg String to log
     */
    public static void info(String strMsg) {
        str_debug.append(strMsg);
        str_debug.append("\r\n");
    }

    /**
     * Helper function to log debugging information
     *
     * @param strMsg String to log
     */
    public static void debug(String strMsg) {
        if (blnVerbose) {
            //System.out.println(strMsg);
            info(strMsg);
        }
    }

    /**
     * Helper function to return an unsigned Integer value into a Short (Big Endian)
     *
     * @param arrVect   ArrayList
     * @param intOffset
     */
    public static Short getLEByte(ArrayList arrVect, int intOffset) {
        short b0 = (short) ((short) ((Byte) arrVect.get(intOffset)).byteValue() & 0xFF);

        // return value
        return new Short(b0);
    }

    /**
     * Helper function to return an unsigned Integer value into a Short (Big Endian)
     *
     * @param arrVect   ArrayList
     * @param intOffset
     */
    public static Integer getLEShort(ArrayList arrVect, int intOffset) {
        int b1 = ((int) ((Byte) arrVect.get(intOffset + 1)).byteValue() & 0xFF) << 8;
        int b0 = ((int) ((Byte) arrVect.get(intOffset)).byteValue() & 0xFF);
        int intValue = b0 + b1;

        // return value
        return new Integer(intValue);
    }

    /**
     * Helper function to return an unsigned Integer value into a Long (Big Endian)
     *
     * @param arrVect   ArrayList
     * @param intOffset
     */
    public static Long getLEInt(ArrayList arrVect, int intOffset) {
        long b3 = ((long) ((Byte) arrVect.get(intOffset + 3)).byteValue() & 0xFF) << 24;
        long b2 = ((long) ((Byte) arrVect.get(intOffset + 2)).byteValue() & 0xFF) << 16;
        long b1 = ((long) ((Byte) arrVect.get(intOffset + 1)).byteValue() & 0xFF) << 8;
        long b0 = ((long) ((Byte) arrVect.get(intOffset)).byteValue() & 0xFF);
        long lngValue = b0 + b1 + b2 + b3;

        // return value
        return new Long(lngValue);
    }

    /**
     * Helper function to return an unsigned Integer value into a Short (Big Endian)
     *
     * @param arrVect   ArrayList
     * @param intOffset
     */
    public static Integer getBEShort(ArrayList arrVect, int intOffset) {
        int b0 = ((int) ((Byte) arrVect.get(intOffset + 1)).byteValue() & 0xFF);
        int b1 = ((int) ((Byte) arrVect.get(intOffset)).byteValue() & 0xFF) << 8;
        int intValue = b0 + b1;

        // return value
        return new Integer(intValue);
    }

    /**
     * Helper function to return an unsigned Integer value into a Long (Big Endian)
     *
     * @param arrVect   ArrayList
     * @param intOffset
     */
    public static Long getBEInt(ArrayList arrVect, int intOffset) {
        long b0 = ((long) ((Byte) arrVect.get(intOffset + 3)).byteValue() & 0xFF);
        long b1 = ((long) ((Byte) arrVect.get(intOffset + 2)).byteValue() & 0xFF) << 8;
        long b2 = ((long) ((Byte) arrVect.get(intOffset + 1)).byteValue() & 0xFF) << 16;
        long b3 = ((long) ((Byte) arrVect.get(intOffset)).byteValue() & 0xFF) << 24;
        long lngValue = b0 + b1 + b2 + b3;

        // return value
        return new Long(lngValue);
    }

    /**
     * Helper function to extract a Null Terminated String from a ArrayList
     *
     * @param arrVect   ArrayList
     * @param intOffset
     */
    public static String getNTString(ArrayList arrVect, int intOffset, boolean blnStopAtNextChar) {
        boolean blnNullPassed = false;
        char c;
        StringBuffer sb = new StringBuffer();
        do {
            c = (char) ((Byte) arrVect.get(intOffset++)).byteValue();
            if (c == 0) {
                blnNullPassed = true;
                if (blnStopAtNextChar)
                    do {
                        char d = (char) ((Byte) arrVect.get(intOffset++)).byteValue();
                        if (d == 0) sb.append(c);
                        c = d;
                    } while (c == 0);
            } else sb.append(c);
        }
        while ((intOffset < arrVect.size()) && !blnNullPassed);

        return new String(sb);
    }

    /**
     * Helper function to extract a String from a ArrayList
     *
     * @param arrVect   ArrayList
     * @param intOffset
     * @param intLength
     */
    public static String getString(ArrayList arrVect, int intOffset, int intLength) {
        byte[] arrByte = new byte[intLength];
        for (int i = 0; i < intLength; i++) {
            arrByte[i] = ((Byte) arrVect.get(intOffset + i)).byteValue();
        }
        return new String(arrByte);
    }
}