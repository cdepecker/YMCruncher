package ymcruncher.core;

import lha.LhaEntry;
import lha.LhaFile;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Chiptune
 *
 * @author FKey
 */
public class Chiptune {

    // Logger
    private static final Logger LOGGER = LogManager.getLogger(Chiptune.class.getName());

    //*****************************************************
    // Private Data
    //*****************************************************

    // Is loaded
    private boolean isLoaded = false;

    // Underlying file
    private File file = null;

    // ID3
    private String strSongName = null;
    private String strAuthorName = null;

    // Data
//    private int length = 0;
    private ArrayList<Frame> arrFrame = null;
    private int playRate = Tools.CPC_REPLAY_FREQUENCY;
    private long frequency = Tools.YM_CPC_FREQUENCY;
    private boolean blnLoop = false;
    private long loopVBL = 0;

    // Sample & Digidrums
    private Sample[] arrSamples = null;

    // Log
    private StringBuffer sbLog = null;


    //*****************************************************
    // Constructors
    //*****************************************************
    public Chiptune() {}

    public Chiptune(File file) {
        this.file = file;
    }

    public Chiptune(String pSongName,
                    String pAuthor,
                    ArrayList<Frame> parrFrame,
                    int prate,
                    long pfrequency,
                    boolean pblnLoop,
                    long ploopVBL,
                    Sample[] parrSamples,
                    File file) {
        // ID3
        strSongName = pSongName;
        strAuthorName = pAuthor;

        // Data
        arrFrame = parrFrame;
        playRate = prate;
        frequency = pfrequency;
        blnLoop = pblnLoop;
        loopVBL = ploopVBL;

        // Samples & Digidrums
        arrSamples = parrSamples;

        // File
        this.file = file;

        /*
         * For Every Atari Digidrums
         * Try to find the static frequency in the chiptune
         */
        if (arrSamples != null) {
            double[] sample_rate = new double[arrSamples.length];
            boolean[] sample_is_digidrum = new boolean[arrSamples.length];
            for (int i = 0; i < sample_rate.length; i++) {
                sample_rate[i] = 0;
                sample_is_digidrum[i] = true;
            }

            for (Frame frame : arrFrame) {
                for (int si_count = 0; si_count < 3; si_count++) {
                    SampleInstance si = frame.getSI(si_count);
                    if ((si == null) || si.getType() != SpecialFXType.ATARI_DIGIDRUM) continue;

                    int nbSample = si.getSample();
                    if (sample_rate[nbSample] == 0)
                        sample_rate[nbSample] = si.getRate();
                    else if (sample_rate[nbSample] != si.getRate())
                        sample_is_digidrum[nbSample] = false;
                }
            }

            for (int i = 0; i < sample_is_digidrum.length; i++)
                if (sample_is_digidrum[i])
                    arrSamples[i].setRate(sample_rate[i]);
        }

        isLoaded = true;
    }


    //*****************************************************
    // Functions
    //*****************************************************

    /**
     * Return a String identifying the type of the file:
     *
     * @param strSourceFile String indicating the chiptune source filename
     * @param intOffset     String offset (where to get the type of this file)
     * @param intNbBytes    Number of characters to return
     */
    private String getFileType(String strSourceFile, int intOffset, int intNbBytes) {
        byte[] arrType = new byte[intNbBytes];

        try {
            RandomAccessFile file_input = new RandomAccessFile(strSourceFile, "r");

            try {
                // read intNbBytes bytes
                file_input.seek(intOffset);
                file_input.read(arrType);
            } catch (EOFException eof) {
                LOGGER.error(eof);
                return null;
            }
            file_input.close();
        } catch (IOException e) {
            LOGGER.error(e);
            return null;
        }

        return new String(arrType);
    }

    /**
     * Get the uncompressed file from the InputStream
     *
     * @param in InputStream of the compressed file
     * @return Uncompressed Byte ArrayList
     */
    private ArrayList<Byte> getArrayListFromInputStream(InputStream in) {
        ArrayList<Byte> arrData = new ArrayList<>();
        try {
            while (in.available() > 0) {
                Byte byte_read = (byte) in.read();
                arrData.add(byte_read);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return arrData;
    }

    public Chiptune load(List<InputPlugin> inputPlugins) {

        // Reject
        if (file == null) {
            LOGGER.warn("Attempting to load a chiptune with a null file value");
            return this;
        }

        // Load chiptune only if it's not already loaded
        if (!isLoaded()) {

            String strSourceFile = file.getAbsolutePath();
            LOGGER.debug("Loading file " + strSourceFile);
            ArrayList<Byte> arrData;

            if ("-lh5-".equals(getFileType(strSourceFile, 2, 5))) {
                // LHA Compressed
                try {
                    LhaFile inpLhaFile = new LhaFile(strSourceFile);
                    Enumeration<LhaEntry> e = inpLhaFile.entries();
                    LhaEntry entry = e.nextElement();
                    InputStream in = inpLhaFile.getInputStream(entry);
                    arrData = getArrayListFromInputStream(in);
                    in.close();
                    inpLhaFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    LOGGER.error("Error during LHA deflating operation of file "+ strSourceFile);
                    return this;
                }
            } else {   // Might be Zip, Gzip or not compressed
                try {
                    ZipFile zipFile = new ZipFile(strSourceFile);
                    Enumeration<? extends ZipEntry> e = zipFile.entries();
                    ZipEntry entry = e.nextElement();
                    InputStream in = zipFile.getInputStream(entry);
                    arrData = getArrayListFromInputStream(in);
                    in.close();
                    zipFile.close();
                } catch (IOException e1) {
                    LOGGER.debug("Not possible to unZip " + strSourceFile);
                    try {
                        InputStream in = new GZIPInputStream(new FileInputStream(strSourceFile));
                        arrData = getArrayListFromInputStream(in);
                        in.close();
                    } catch (IOException e3) {
                        LOGGER.debug("Not possible to unGzip " + strSourceFile);
                        // Source file isn't compressed
                        try {
                            // Wrap the FileInputStream with a DataInputStream
                            FileInputStream file_input = new FileInputStream(strSourceFile);
                            arrData = getArrayListFromInputStream(new DataInputStream(file_input));
                            ((InputStream) new DataInputStream(file_input)).close();
                            file_input.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                            LOGGER.error("Error during uncompressed file read operation of "+ strSourceFile);
                            return this;
                        }
                    }
                }
            }

            // return if something has gone wrong
            if (arrData == null) return this;

            // Get File extension in order to guess the chiptune format
            String strFileNameOnly = file.getName();
            String strExt = strFileNameOnly.replaceFirst("^.*\\.", "");

            /*
             * Parse the list of available InputPlugins and try to find
             * one which can handle the format of the input chiptune
             */
            for (InputPlugin inputPlugin: inputPlugins) {
                LOGGER.debug("Testing plugin " + inputPlugin.getClass() +" to load file " + strSourceFile);
                // Get array of Registers' values
                // Basically a ArrayList of 14 Registers for which the frequency has been converted
                if (inputPlugin.loadChiptune(this, arrData, strExt)) {
                    isLoaded = true;
                    Tools.info("+ File Type = " + inputPlugin.strFileType);
                    setSbLog(Tools.getLog());
                    LOGGER.debug(Tools.getLog().toString());
                    return this;
                }
            }
        }
        return this;
    }

    public Chiptune copyclone() {
        // Copy arrFrame
        ArrayList<Frame> arrCloneFrame = new ArrayList<>();
        for (int i = 0; i < arrFrame.size(); i++) {
            // clone Frame
            Frame frame = getFrame(i).copyclone();
            arrCloneFrame.add(frame);
        }

        // Copy arrSamples
        Sample[] arrSamplesClone = null;
        if (arrSamples != null) {
            arrSamplesClone = new Sample[arrSamples.length];
            for (int i = 0; i < arrSamples.length; i++) arrSamplesClone[i] = arrSamples[i].copyclone();
        }

        // return cloned chiptune
        return new Chiptune(
                // ID3
                (strSongName == null) ? null : strSongName,
                (strAuthorName == null) ? null : strAuthorName,

                // Data
                arrCloneFrame,
                playRate,
                frequency,
                blnLoop,
                loopVBL,

                // Sample & Digidrums
                arrSamplesClone,

                // File
                file
        );
    }

    public void filter(boolean blnConvertFrequency, boolean blnAdjustFrequency, long lngClockFrequency, byte bytReg7Filter, boolean[] arrEnvFilter, HashMap<SpecialFXType, Boolean> arrSpecialFXFilter) {
        // get Convertion Ratio (Tone & Noise)
        double dbConvertRatio = (double) lngClockFrequency / frequency;

        //Number of values to adjust sequentially (Channels 0, 1, 2)
        int[] arrFreqAdjustLength = new int[]{0, 0, 0};
        int envFreqAdjustLength = 0;

        // Deltas
        double[] arrFreqAdjustDelta = new double[]{0, 0, 0};
        double envFreqAdjustDelta = 0;

        // Deltas
        double[] arrFreqAdjustValue = new double[]{0, 0, 0};
        double envFreqAdjustValue = 0;

        // Filter Chiptune
        for (int i = 0; i < arrFrame.size(); i++) {
            // Current frame
            Frame frame = arrFrame.get(i);

            // Mixer Filter
            // Disable Channel Mixing when period is null
            byte mixFilter = bytReg7Filter;
            for (int v = 0; v < 3; v++) {
                if (frame.getPPeriod(v) < 1) mixFilter |= (1 << v);
            }
            frame.setBytReg7((byte) (frame.getBytReg7() | mixFilter));

            // Filter SpecialFX
            for (byte c = 0; c < 3; c++) {
                SampleInstance si = frame.getSI(c);
                if ((si != null) && (!arrSpecialFXFilter.get(si.getType()))) frame.setSI(c, null);
            }

            // Env Filter
            for (int c = 0; c < 3; c++)
                if (!arrEnvFilter[c]) frame.setBytVol(c, (byte) (frame.getBytVol(c) & 0xF));

            // Convert Frequency if needed
            if (blnConvertFrequency && (lngClockFrequency != frequency)) {
                // Convert Frequencies for Channels 0,1,2
                for (byte c = 0; c < 3; c++)
                    freqAdjust(frame, arrFreqAdjustLength, arrFreqAdjustValue, arrFreqAdjustDelta, c, i, dbConvertRatio);

                // Convert Frequencies for Channels N
                double dbConvertedValue = frame.getPPeriodN() * dbConvertRatio;
                frame.setPPeriodN((int) (dbConvertedValue + 0.5d));


                if (!blnAdjustFrequency) {


                    dbConvertedValue = frame.getPPeriodE() * dbConvertRatio;

                    // HACK TAO - Seagulls
                    // Add period on the channel A if period is zero (worth a try)
//					if (((int)(dbConvertedValue + 0.5d) != ((int)(dbConvertedValue))) && 
                    if ((frame.getPPeriod(0) == 0d) && (frame.getPPeriodE() != 0d)) {
                        double dbConvertedValuePeriod = frame.getPPeriodE() * 16d * dbConvertRatio;
                        frame.setPPeriod(0, (int) (dbConvertedValuePeriod + 0.5d));
                        System.out.println("ENV freq = " + frequency / (256 * frame.getPPeriodE()) +
                                " CONV = " + lngClockFrequency / (256L * ((int) (dbConvertedValue + 0.5d))) +
                                " PERIOD = " + lngClockFrequency / (16L * ((int) (dbConvertedValuePeriod + 0.5d))));
                    }

                    frame.setPPeriodE((int) (dbConvertedValue + 0.5d));
                } else if (envFreqAdjustLength == 0) {
                    int j = i + 1;
                    while ((j < arrFrame.size()) &&
                            (arrFrame.get(j).getPPeriodE() == frame.getPPeriodE())) {
                        envFreqAdjustLength++;
                        j++;
                    }

                    // We Adjust only if arrFreqAdjustLength[c] > 1
                    if (envFreqAdjustLength > 0) {
                        envFreqAdjustValue = frame.getPPeriodE() * dbConvertRatio;

                        // get intValue
                        int intValue = (int) envFreqAdjustValue;

                        // Get Delta
                        envFreqAdjustDelta = envFreqAdjustValue - intValue;

                        // First is always an int value
                        frame.setPPeriodE(intValue);
                    } else {
                        // Normal adjust
                        dbConvertedValue = frame.getPPeriodE() * dbConvertRatio;
                        frame.setPPeriodE((int) (dbConvertedValue + 0.5d));
                    }
                } else {
                    // We are in the adjustment Loop
                    double dbF = envFreqAdjustValue + envFreqAdjustDelta;
                    frame.setPPeriodE((int) dbF);

                    // If reached next integer values, then go back to previous
                    if (((int) dbF) > ((int) envFreqAdjustValue)) envFreqAdjustValue = dbF - 1;
                    else envFreqAdjustValue = dbF;

                    envFreqAdjustLength--;
                }
            }
        }

        // Change frequency
        if (blnConvertFrequency) frequency = lngClockFrequency;
    }

    private void freqAdjust(Frame frame,                    // current frame
                            int[] arrFreqAdjustLength,
                            double[] arrFreqAdjustValue,
                            double[] arrFreqAdjustDelta,
                            int c,                            // channel 0,1,2
                            int i,                            // current frame's indice
                            double dbConvertRatio) {
        // Get number of values to adjust - 1
        if (arrFreqAdjustLength[c] == 0) {
            int j = i + 1;
            while ((j < arrFrame.size()) &&
                    (arrFrame.get(j).getPPeriod(c) == frame.getPPeriod(c))) {
                arrFreqAdjustLength[c]++;
                j++;
            }

            // We Adjust only if arrFreqAdjustLength[c] > 1
            if (arrFreqAdjustLength[c] > 0) {
                arrFreqAdjustValue[c] = frame.getPPeriod(c) * dbConvertRatio;

                // get intValue
                int intValue = (int) arrFreqAdjustValue[c];

                // Get Delta
                arrFreqAdjustDelta[c] = arrFreqAdjustValue[c] - intValue;

                // First is always an int value
                frame.setPPeriod(c, intValue);
            } else {
                // Normal adjust
                double dbConvertedValue = frame.getPPeriod(c) * dbConvertRatio;
                frame.setPPeriod(c, (int) (dbConvertedValue + 0.5d));
            }

            // next channel
            return;
        }

        // We are in the adjustment loop
        double dbF = arrFreqAdjustValue[c] + arrFreqAdjustDelta[c];
        frame.setPPeriod(c, (int) dbF);

        // If reached next integer values, then go back to previous
        if (((int) dbF) > ((int) arrFreqAdjustValue[c])) arrFreqAdjustValue[c] = dbF - 1;
        else arrFreqAdjustValue[c] = dbF;

        arrFreqAdjustLength[c]--;
    }

    // This functions will try to convert to 50hz the input chiptune
    public void force50Hz() {
        double dbConvert = 50d / playRate;
        int nbFrames50Hz = (int) (arrFrame.size() * dbConvert + 0.5d);

        ArrayList<Frame> arrCloneFrame = new ArrayList<>();

        for (int i = 0; i < nbFrames50Hz; i++) {
            // Get index in arrFrame
            // TODO Interpole with skipped frames ? (channels all but reg 7)
            int index = (int) (i / dbConvert + 0.5d);
            arrCloneFrame.add(getFrame(index).copyclone());
        }

        arrFrame = arrCloneFrame;
//        length = arrCloneFrame.size();
        playRate = 50;

    }

    public ArrayList<Byte> getArrayListRegister(int reg) {
        ArrayList<Byte> vect = new ArrayList<>();

        for (int i = 0; i < arrFrame.size(); i++) {
            vect.add(getFrame(i).getReg(reg));
        }
        return vect;
    }

    public Frame getFrame(int i) {
        if ((isLoaded) && (arrFrame == null)) return new Frame();
        return arrFrame.get(i);
    }

    public String getInfo() {
        return this.getStrSongName() +
                "\nAuthor = " + this.getStrAuthorName() +
                "\nPlay Rate = " + this.getPlayRate() +
                "\nLength = " + this.getLength() +
                "\nFrequency = " + this.getFrequency() +
                "\nNb samples = " + this.getNbSamples();
    }

    //*****************************************************
    // Getters and Setters
    //*****************************************************

    public int getNbSamples() {
        return (arrSamples == null) ? 0 : arrSamples.length;
    }

    public Sample[] getArrSamples() {
        return arrSamples;
    }

    public void setBlnLoop(boolean blnLoop) {
        this.blnLoop = blnLoop;
    }

    public void setLoopVBL(long loopVBL) {
        this.loopVBL = loopVBL;
    }

    public void setArrSamples(Sample[] arrSamples) {
        this.arrSamples = arrSamples;
    }

    public boolean isBlnLoop() {
        return blnLoop;
    }

    public long getFrequency() {
        return frequency;
    }

    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public int getLength() {
        return arrFrame.size();
    }

    public long getLoopVBL() {
        return loopVBL;
    }

    public int getPlayRate() {
        return playRate;
    }

    public void setPlayRate(int playRate) {
        this.playRate = playRate;
    }

    public StringBuffer getSbLog() {
        return sbLog;
    }

    public void setSbLog(StringBuffer sbLog) {
        this.sbLog = new StringBuffer(sbLog);
    }

    public String getStrAuthorName() {
        return strAuthorName;
    }

    public void setStrAuthorName(String strAuthorName) {
        this.strAuthorName = strAuthorName;
    }

    public String getStrSongName() {
        return strSongName;
    }

    public void setStrSongName(String strSongName) {
        this.strSongName = strSongName;
    }

    public ArrayList<Frame> getArrFrame() {
        return arrFrame;
    }

    public void setArrFrame(ArrayList<Frame> arrFrame) {
        this.arrFrame = arrFrame;
    }

    public File getFile() {return file;}

    public void unload() {
        arrFrame = null;
        isLoaded = false;
    }

    public String toString(){
        return getFileName();
    }

    public String getFileName() {
        return file==null?"UNKNOWN":file.getName();
    }
}