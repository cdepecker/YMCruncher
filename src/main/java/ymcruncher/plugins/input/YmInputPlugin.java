package ymcruncher.plugins.input;

import com.google.auto.service.AutoService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ymcruncher.core.Chiptune;
import ymcruncher.core.Frame;
import ymcruncher.core.InputPlugin;
import ymcruncher.core.Sample;
import ymcruncher.core.SampleInstance;
import ymcruncher.core.SpecialFXType;
import ymcruncher.core.Tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class handle the YM chiptune format input to the core cruncher.
 * YM is a format defined originally for atariST chiptunes but has been extended to handle
 * speccy's and cpc's chiptunes as well.
 *
 * @author F-key/RevivaL
 */
@AutoService(InputPlugin.class)
public class YmInputPlugin extends InputPlugin {

    // Logger
    private static final Logger LOGGER = LogManager.getLogger(YmInputPlugin.class.getName());

    // ATARI-ST MFP chip predivisor & Timer settings
    final private static int[] mfpPrediv = {0, 4, 10, 16, 50, 64, 100, 200};
    final private static long MFP_CLOCK = 2457600L;
    final private static String YM2_DIGIDRUM_FILE = "./YM2_digidrums.dat";

    // Attribute values for YM5/6
    final private static byte A_STREAMINTERLEAVED = 1;
    final private static byte A_DRUMSIGNED = 2;
    final private static byte A_DRUM4BITS = 4;
    final private static byte A_TIMECONTROL = 8;
    final private static byte A_LOOPMODE = 16;

    private String strSongName = null;
    private String strAuthorName = null;
    private Long intFrequency = Tools.YM_ATARI_FREQUENCY;
    private Long intLoopVBL = 0L;
    private boolean blnLoop = false;
    private int intPlayRate = Tools.CPC_REPLAY_FREQUENCY;

    // Samples
    private Sample[] arrDigiDrums = null;

    // redirect Samples (trim unused samples)
    private int total_sample = 0;
    private int[] sampleRedirect = null;
    private boolean[] blSampleUsed = null;

    // SpecialFX log
    private boolean blnSidVoice = false;
    private boolean blnSinSid = false;
    private boolean blnSyncBuzzer = false;
    /**
     * Digidrums YM2 Load
     */
    private final byte[][] sampleAdress = {
            new byte[631], new byte[631], new byte[490], new byte[490], new byte[699], new byte[505], new byte[727], new byte[480],
            new byte[2108], new byte[4231], new byte[378], new byte[1527], new byte[258], new byte[258], new byte[451], new byte[1795],
            new byte[271], new byte[636], new byte[1379], new byte[147], new byte[139], new byte[85], new byte[150], new byte[507],
            new byte[230], new byte[120], new byte[271], new byte[293], new byte[391], new byte[391], new byte[391], new byte[407],
            new byte[407], new byte[407], new byte[317], new byte[407], new byte[311], new byte[459], new byte[329], new byte[656]};

    @Override
    protected boolean getPreProcessedChiptune(Chiptune chiptune, ArrayList arrRawChiptune, String strExt) {
        initPlugin();

        // Wrapper for below function
        ArrayList<Frame> arrFrames = getPSGRegistersValues(arrRawChiptune, strExt);

        Sample[] list_samples = getSamples();

        // Special FX info
        boolean blnDigiDrums = (list_samples != null) && (list_samples.length > 0);
        if (blnSidVoice || blnSinSid || blnSyncBuzzer || blnDigiDrums)
            Tools.info("+ SpecialFX");
        if (blnSidVoice) Tools.info("  - Sid Voices");
        if (blnSinSid) Tools.info("  - Sinus Sid");
        if (blnSyncBuzzer) Tools.info("  - Sync Buzzer");
        if (blnDigiDrums) Tools.info("  - Digidrums");


        // return if errors
        if (arrFrames == null) return false;

        chiptune.setStrSongName(strSongName);
        chiptune.setStrAuthorName(strAuthorName);
        chiptune.setArrFrame(arrFrames);
        chiptune.setPlayRate(intPlayRate);
        chiptune.setFrequency(intFrequency);
        chiptune.setBlnLoop(blnLoop);
        chiptune.setLoopVBL(intLoopVBL);
        chiptune.setArrSamples(list_samples);

        // chiptune loaded OK
        return true;
    }

    protected void initPlugin() {
        strSongName = null;
        strAuthorName = null;
        intFrequency = Tools.YM_ATARI_FREQUENCY;
        intLoopVBL = 0L;
        blnLoop = false;
        intPlayRate = Tools.CPC_REPLAY_FREQUENCY;

        // Samples
        arrDigiDrums = null;

        // redirect Samples (trim unused samples)
        total_sample = 0;
        sampleRedirect = null;
        blSampleUsed = null;

        // SpecialFX log
        blnSidVoice = false;
        blnSinSid = false;
        blnSyncBuzzer = false;
    }

    /**
     * This method should basically return an array of PSG registers' values
     * Let's call Rx the Register number x, then this function should return
     * an array of all the R0 values for all frames then all the R1 values for all frames
     * and so on ...
     *
     * @param arrRawChiptune ArrayList representing the uncompressed inputed chiptune
     * @return ArrayList that should contain the PSG registers' values.
     */
    protected ArrayList<Frame> getPSGRegistersValues(ArrayList arrRawChiptune, String strExt) {
        int intNbRegisters = Tools.CPC_REGISTERS;
        ArrayList<Byte> arrRegistersValues = null;

        // Get type of file
        strFileType = Tools.getString(arrRawChiptune, 0, 4);
        switch (strFileType) {
            case "YM2!":
                // Load YM2 digidrums
                FileInputStream file_input;
                try {
                    file_input = new FileInputStream(YM2_DIGIDRUM_FILE);

                    int sample = 0;

                    while (sample < 40) {
                        file_input.read(sampleAdress[sample]);
                        sample++;
                    }

                    // init redirect array
                    sampleRedirect = new int[40];
                    blSampleUsed = new boolean[40];
                    for (int i = 0; i < sampleRedirect.length; i++) blSampleUsed[i] = false;

                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Set Frequency of song to 2000000 (atari)
                intFrequency = Tools.YM_ATARI_FREQUENCY;

                // get rid of the header string
                arrRegistersValues = new ArrayList<Byte>(arrRawChiptune.subList(4, arrRawChiptune.size()));
                break;
            case "YM3!":
                // Set Frequency of song to 2000000 (atari)
                intFrequency = Tools.YM_ATARI_FREQUENCY;

                // get rid of the header string
                arrRegistersValues = new ArrayList<Byte>(arrRawChiptune.subList(4, arrRawChiptune.size()));
                break;
            case "YM3b":
                // Set Frequency of song to 2000000 (atari)
                intFrequency = Tools.YM_ATARI_FREQUENCY;

                // get rid of the header string and the VBL loop
                arrRegistersValues = new ArrayList<Byte>(arrRawChiptune.subList(4, arrRawChiptune.size() - 4));
                break;
            case "YM5!":
            case "YM6!":
                // Set number of registers
                intNbRegisters = 16;

                long intVBL = Tools.getBEInt(arrRawChiptune, 12);
                Long intAttributes = Tools.getBEInt(arrRawChiptune, 16);
                Integer shtDigiDrum = Tools.getBEShort(arrRawChiptune, 20);
                intFrequency = Tools.getBEInt(arrRawChiptune, 22);
                Integer shtPlayerFrequency = Tools.getBEShort(arrRawChiptune, 26);
                intLoopVBL = Tools.getBEInt(arrRawChiptune, 28);
                Tools.info("+ VBL = 0x" + Long.toHexString(intVBL).toUpperCase() + " (~" + intVBL / 50 + " seconds)");
                Tools.info("+ Number of DigiDrum = " + shtDigiDrum);
                Tools.info("+ Frequency = " + intFrequency + " Hz");
                Tools.info("+ Player Frequency = " + shtPlayerFrequency + " Hz");
                Tools.info("+ Loop VBL = " + Long.toHexString(intLoopVBL).toUpperCase());

                /*
                 * Attributes
                 */
                Tools.info("+ Attributes = 0x" + Long.toHexString(intAttributes).toUpperCase());
                boolean isInterleaved = false;
                boolean areSampleSigned = false;
                boolean areSample4Bits = false;

                // Interleaved ?
                if (((intAttributes & A_STREAMINTERLEAVED) == 0))
                    Tools.info("  - YM file is uninterleaved");
                else {
                    Tools.info("  - YM file is interleaved");
                    isInterleaved = true;
                }

                // DIGIDRUM signed ?
                if (((intAttributes & A_DRUMSIGNED) == 0)) Tools.info("  - Digidrums are unsigned");
                else {
                    Tools.info("  - Digidrums are signed");
                    areSampleSigned = true;
                }

                // 4bit DIGIDRUM
                if (((intAttributes & A_DRUM4BITS) == 0))
                    Tools.info("  - Digidrums resolution is 8bits");
                else {
                    Tools.info("  - Digidrums resolution is 4bits");
                    areSample4Bits = true;
                }

                // Loop mode ?
                if (((intAttributes & A_LOOPMODE) == 0)) Tools.info("  - Loop mode OFF");
                else {
                    Tools.info("  - Loop mode ON");
                    blnLoop = true;
                }

                // Time Control ?
                if (((intAttributes & A_TIMECONTROL) == 0)) Tools.info("  - Time Control OFF");
                else Tools.info("  - Time Control ON");

                // Playing rate adjustment (60hz <-> 50hz)
                intPlayRate = shtPlayerFrequency;

                // get digidrums
                int intOffset = 34;
                arrDigiDrums = new Sample[shtDigiDrum];
                for (int i = 0; i < shtDigiDrum; i++) {
                    Long intSizeDD = Tools.getBEInt(arrRawChiptune, intOffset);
                    intOffset += 4;

                    byte[] arrDigi = new byte[intSizeDD.intValue()];
                    for (int j = 0; j < intSizeDD.intValue(); j++) {
                        byte bDD = (Byte) arrRawChiptune.get(intOffset++);
                        arrDigi[j] = bDD;
                    }
                    arrDigiDrums[i] = new Sample(String.valueOf(i),
                            intSizeDD.intValue(),
                            (byte) ((areSample4Bits == true) ? 4 : 8),
                            areSampleSigned,
                            (byte) 0,
                            (byte) 0xF,
                            0,
                            0,
                            arrDigi);

                    // debug
                    Tools.debug("- DigiDrums (" + i + ") size = " + intSizeDD);
                }

                // init redirect array
                sampleRedirect = new int[shtDigiDrum];
                blSampleUsed = new boolean[shtDigiDrum];
                for (int i = 0; i < sampleRedirect.length; i++) blSampleUsed[i] = false;

                // Other Info
                strSongName = Tools.getNTString(arrRawChiptune, intOffset, false);
                strAuthorName = Tools.getNTString(arrRawChiptune, intOffset += strSongName.length() + 1, false);
                String strComments = Tools.getNTString(arrRawChiptune, intOffset += strAuthorName.length() + 1, false);
                Tools.info("+ Song Name = " + strSongName);
                Tools.info("+ Author = " + strAuthorName);
                Tools.info("+ Comments = " + strComments);

                // Offset points to data
                intOffset += strComments.length() + 1;

                // return sub-array (minus 4 bytes for 'End!')
                int intTailer = 0;
                if (Tools.getString(arrRawChiptune, arrRawChiptune.size() - 4, 4).equals("End!")) intTailer = 4;
                arrRegistersValues = new ArrayList<Byte>(arrRawChiptune.subList(intOffset, arrRawChiptune.size() - intTailer));

                // Interleave file if it is not
                if (!isInterleaved) {
                    // Interleave data (set all data for R0 then all for R1 ...)
                    ArrayList<Byte> arrInterleavedRawChiptune = new ArrayList();
                    for (int reg = 0; reg < intNbRegisters; reg++) {
                        for (int i = 0; i < intVBL; i++) {
                            arrInterleavedRawChiptune.add(arrRegistersValues.get(i * intNbRegisters + reg));
                        }
                    }
                    arrRegistersValues = arrInterleavedRawChiptune;
                }

                // Extra log : Offsets in original YM file
                for (int i = 0; i < intNbRegisters; i++)
                    Tools.debug("Offset reg " + i + ": " + (i * (arrRegistersValues.size() / intNbRegisters) + intOffset));
                break;
        }

        // return if not an YM file
        if (arrRegistersValues == null) return null;

        /*
         * Create array Frame
         */
        int intFileSize = arrRegistersValues.size();
        int intNbFrames = intFileSize / intNbRegisters;

        // Digidrums log
        //if (arrSampleLog == null)  arrSampleLog = new ArrayList<SampleInstance[]>();

        //	Get array of bytes during next loop
        byte[] arrPSGRead = new byte[intNbRegisters];
        byte[] arrPSG = new byte[Tools.CPC_REGISTERS];

        // Cut the big array into an Array of Frames
        ArrayList<Frame> arrFrame = new ArrayList<Frame>();
        for (int j = 0; j < intNbFrames; j++) {
            //	Get array of bytes
            for (int i = 0; i < intNbRegisters; i++)
                arrPSGRead[i] = arrRegistersValues.get(i * intNbFrames + j);

            // Copy registers 0-10
            for (byte reg = 0; reg < 11; reg++) arrPSG[reg] = arrPSGRead[reg];

            // Get Special FX
            //arrSampleLog.add(new SampleInstance[3]);
            SampleInstance[] arrSI = blnReadFX(arrPSG, arrPSGRead/*, j*/);

            int dbFreq0 = arrPSG[1] & 0xF;
            dbFreq0 <<= 8;
            dbFreq0 += ((int) arrPSG[0]) & 0xFF;

            int dbFreq1 = arrPSG[3] & 0xF;
            dbFreq1 <<= 8;
            dbFreq1 += ((int) arrPSG[2]) & 0xFF;

            int dbFreq2 = arrPSG[5] & 0xF;
            dbFreq2 <<= 8;
            dbFreq2 += ((int) arrPSG[4]) & 0xFF;

            int dbFreqE = ((int) arrPSG[12]) & 0xFF;
            dbFreqE <<= 8;
            dbFreqE += ((int) arrPSG[11]) & 0xFF;

            //System.out.println(dbFreqE);

            int dbFreqN = ((int) arrPSG[6]) & 0x1F;

            byte bytMixer = arrPSG[7];

            byte bytVol0 = arrPSG[8];
            byte bytVol1 = arrPSG[9];
            byte bytVol2 = arrPSG[10];

            byte bytEnv = arrPSG[13];

            // Hack virual escape
			/*if ((bytVol2 & 0x10) == 0x10){
            			double rate = (double)(YMC_Tools.YM_CPC_FREQUENCY/(256*dbFreqE));
				SampleInstance si = new SampleInstance(0xE, rate);
				si.setOrigFxNb(1);
				si.setType(SpecialFXType.ATARI_SIDVOICE);
				arrSI[2] = si;
				dbFreqE = 1;		// disable env
				bytEnv = (byte)0xFF;	// "
			}*/
            // Hack flow (tao)
			/*if ((bytVol0 & 0x10) == 0x10){
            	double rate = (double)(YMC_Tools.YM_CPC_FREQUENCY/(256*dbFreqE));
				SampleInstance si = new SampleInstance(0xE, rate);
				si.setOrigFxNb(1);
				si.setType(SpecialFXType.ATARI_SIDVOICE);
				arrSI[2] = si;
				dbFreqE = 1;		// disable env
				bytEnv = (byte)0xFF;	// "
			}*/

            arrFrame.add(new Frame(
                    new double[]{dbFreq0, dbFreq1, dbFreq2},    // Freq channels 0,1,2
                    dbFreqN,                                    // Freq Noise
                    dbFreqE,                                    // Freq Env
                    bytMixer,                                    // Mixer (no noise at start)
                    new byte[]{bytVol0, bytVol1, bytVol2},        // Vol channels 0,1,2
                    bytEnv,                                        // Env
                    arrSI                                        // Samples Instance
            ));
        }

        return arrFrame;
    }

    /**
     * Return an array of Samples contained in that chiptune
     * This function should be overriden in subclasses
     */
    protected Sample[] getSamples() {
        // Create samples for YM2 files
        if (strFileType.equals("YM2!") && (total_sample > 0)) {
            arrDigiDrums = new Sample[total_sample];
            for (int i = 0; i < sampleRedirect.length; i++) {
                if (blSampleUsed[i]) {
                    // FIXME assume that all sample YM2 ar at 6083hz ... not sure
                    arrDigiDrums[sampleRedirect[i]] = new Sample(String.valueOf(i),
                            sampleAdress[i].length,
                            (byte) 8,
                            false, // Not signed
                            (byte) 0,
                            (byte) 0xF,
                            0,
                            0,
                            sampleAdress[i]/*,
																	6083l*/);
                }
            }
        } else if ((strFileType.equals("YM5!") || strFileType.equals("YM6!")) && (total_sample > 0)) {
            Sample[] arrDigiDrumsAux = new Sample[total_sample];
            for (int i = 0; i < sampleRedirect.length; i++)
                if (blSampleUsed[i]) arrDigiDrumsAux[sampleRedirect[i]] = arrDigiDrums[i];
            arrDigiDrums = arrDigiDrumsAux;
        }

        return arrDigiDrums;
    }

    private SampleInstance[] blnReadFX(byte[] arrRegs, byte[] arrRegsRead/*, int pos*/) {
        SampleInstance[] arrSI = new SampleInstance[]{null, null, null};
        if (strFileType.equals("YM2!")) {
            readYm2Effect(arrSI, arrRegs, arrRegsRead/*, pos*/);
        } else {
            for (byte reg = 11; reg < Tools.CPC_REGISTERS; reg++) arrRegs[reg] = arrRegsRead[reg];
            if (strFileType.equals("YM6!")) {
                readYm6Effect(arrSI, arrRegsRead/*, pos*/, 1, 6, 14);
                readYm6Effect(arrSI, arrRegsRead/*, pos*/, 3, 8, 15);
            } else if (strFileType.equals("YM5!")) {
                readYm5Effect(arrSI, arrRegsRead/*, pos*/);
            }
        }

        return arrSI;
    }

    /**
     * YM2 Digidrums
     */
    private boolean readYm2Effect(SampleInstance[] arrSI, byte[] arrRegs, byte[] arrRegsRead/*, int pos*/) {
        boolean blnSpecialFX = false;

        if (arrRegsRead[13] != (byte) 0xFF) // MadMax specific (extracted from StSoundLibrary from Leonard)
        {
            arrRegs[11] = arrRegsRead[11];
            arrRegs[12] = 0;
            arrRegs[13] = 10;
        } else arrRegs[13] = (byte) 0xFF;
        if ((arrRegsRead[10] & 0x80) != 0)                    // bit 7 volume canal C pour annoncer une digi-drum madmax.
        {
            int sampleNum;
            arrRegs[7] |= 0x24;                // Coupe TONE + NOISE canal C.
            sampleNum = arrRegsRead[10] & 0x7f;    // Numero du sample

            // remove digidrum info
            arrRegs[10] ^= 0x80;

            if (arrRegsRead[12] != 0) {
                blnSpecialFX = true;
                //sampleFrq = ((MFP_CLOCK>>2) / arrRegs[12]);

                // Sample redirect
                if (sampleNum < blSampleUsed.length && !blSampleUsed[sampleNum]) {
                    sampleRedirect[sampleNum] = total_sample++;
                    blSampleUsed[sampleNum] = true;
                }

                // Log digidrum
                arrSI[2] = new SampleInstance(SpecialFXType.ATARI_DIGIDRUM, sampleRedirect[sampleNum], 1, arrRegsRead[12] & 0xFF, 0);
                //arrSampleLog.get(pos)[2] = new SampleInstance(SampleInstance.FX_TYPE_ATARI_DIGIDRUM, sampleRedirect[sampleNum], 1, arrRegsRead[12] & 0xFF);
                Tools.debug("Sample " + sampleNum + " " + MFP_CLOCK / (4 * (arrRegsRead[12] & 0xFF)) + "Hz");
            }
        }

        return blnSpecialFX;
    }

    /**
     * YM5 SpecialFX
     */
    private boolean readYm5Effect(SampleInstance[] arrSI, byte[] arrRegsRead/*, int pos*/) {
        // YM5 effect decoding
        boolean blnSpecialFX = false;

        //------------------------------------------------------
        // Sid Voice !!
        //------------------------------------------------------
        int code = (arrRegsRead[1] >> 4) & 3;
        if (code != 0) {
            long tmpFreq;
            int voice = code - 1;
            int prediv = (arrRegsRead[6] >> 5) & 7;
            int count = arrRegsRead[14] & 0xFF;
            tmpFreq = mfpPrediv[prediv] * count;
            if (tmpFreq != 0) {
                blnSidVoice = true;
                blnSpecialFX = true;
                tmpFreq = 2457600L / tmpFreq;
                int Vmax = (arrRegsRead[voice + 8] & 15);
                //ymChip.sidStart(voice,tmpFreq,ptr[voice+8]&15);
                arrSI[voice] = new SampleInstance(SpecialFXType.ATARI_SIDVOICE,
                        Vmax,
                        prediv,
                        count, 0);
				/*arrSampleLog.get(pos)[voice] = new SampleInstance(	SampleInstance.FX_TYPE_ATARI_SIDVOICE,
																	Vmax,
																	prediv,
																	count);*/
                //YMC_Tools.debug("+ Sid Sound [" + voice + ", " + (arrRegsRead[voice+8]&15) + ", " + tmpFreq + "]");
				/*System.out.println("+ Sid Sound");
				System.out.println("- Voice : " + voice);
				System.out.println("- VMax : " + (arrRegs[voice+8]&15));
				System.out.println("- Frequency : " + tmpFreq);*/
            }
        }

        //------------------------------------------------------
        // YM5 Digi Drum.
        //------------------------------------------------------
        code = (arrRegsRead[3] >> 4) & 3;
        if (code != 0) {    // Ici un digidrum demarre sur la voie voice.
            int voice = code - 1;
            int ndrum = arrRegsRead[8 + voice] & 31;
            if ((ndrum >= 0) && (ndrum < arrDigiDrums.length)) {
                //long sampleFrq;
                int prediv = mfpPrediv[(arrRegsRead[8] >> 5) & 7];
                prediv *= arrRegsRead[15];
                if (prediv != 0) {
                    blnSpecialFX = true;
                    //sampleFrq = MFP_CLOCK / prediv;
                    //arrSampleLog.get(pos)[voice] = new SampleInstance(ndrum, (arrRegsRead[8]>>5)&7, arrRegsRead[15]);

                    // Sample redirect
                    if (ndrum < blSampleUsed.length && !blSampleUsed[ndrum]) {
                        sampleRedirect[ndrum] = total_sample++;
                        blSampleUsed[ndrum] = true;
                    }

                    // Log digidrum
                    arrSI[voice] = new SampleInstance(SpecialFXType.ATARI_DIGIDRUM, sampleRedirect[ndrum], (arrRegsRead[8] >> 5) & 7, arrRegsRead[15] & 0xFF, 1);
                    //arrSampleLog.get(pos)[voice] = new SampleInstance(SampleInstance.FX_TYPE_ATARI_DIGIDRUM, sampleRedirect[ndrum], (arrRegsRead[8]>>5)&7, arrRegsRead[15] & 0xFF);
                }
            }
        }
        return blnSpecialFX;
    }

    /**
     * YM6 effects
     */
    private boolean readYm6Effect(SampleInstance[] arrSI, byte[] pReg, int code, int prediv, int count) {
        // Get the number of FX (usefull in AYC)
        int nbFx = count - 14;

        code = (byte) (pReg[code] & 0xf0);
        prediv = (pReg[prediv] >> 5) & 7;
        count = pReg[count] & 0xFF;
        boolean blnSpecialFX = false;

        if ((code & 0x30) != 0) {
            long tmpFreq;
            // Ici il y a un effet sur la voie:

            int voice = ((code & 0x30) >> 4) - 1;

            // Detect SpecialFX
            switch (code & 0xc0) {
                case 0x00:        // SID
                    blnSidVoice = true;
                    break;
                case 0x80:        // Sinus-SID
                    blnSinSid = true;
                    break;
                case 0xc0:        // Sync-Buzzer.
                    blnSyncBuzzer = true;
                    break;
            }


            switch (code & 0xc0) {
                case 0x00:        // SID
                case 0x80:        // Sinus-SID
                    tmpFreq = mfpPrediv[prediv] * count;
                    if (tmpFreq != 0) {
                        blnSpecialFX = true;
                        int Vmax = (pReg[voice + 8] & 15);
                        tmpFreq = MFP_CLOCK / tmpFreq;
                        if ((code & 0xc0) == 0x00) {
                            //YMC_Tools.debug("+ Sid Sound [" + voice + ", " + Vmax + ", " + tmpFreq + "]");
                            arrSI[voice] = new SampleInstance(SpecialFXType.ATARI_SIDVOICE,
                                    Vmax,
                                    prediv,
                                    count,
                                    nbFx);
                        } else {
                            Tools.debug("+ SinSid Sound [" + voice + ", " + Vmax + ", " + tmpFreq + "]");
                            arrSI[voice] = new SampleInstance(SpecialFXType.ATARI_SINSID,
                                    Vmax,
                                    prediv,
                                    count,
                                    nbFx);
                        }
                    }
                    break;

                case 0x40:        // DigiDrum
                    int ndrum = pReg[voice + 8] & 31;
                    if ((ndrum >= 0) && (ndrum < arrDigiDrums.length))
                        if (ndrum >= 0) {
                            //prediv = mfpPrediv[prediv];
                            //prediv *= count;
                            if (mfpPrediv[prediv] * count > 0) {
                                blnSpecialFX = true;
                                tmpFreq = MFP_CLOCK / mfpPrediv[prediv] * count;
                                //arrSampleLog.get(pos)[voice] = new SampleInstance(ndrum, prediv, count);

                                // Sample redirect
                                if (ndrum < blSampleUsed.length && !blSampleUsed[ndrum]) {
                                    sampleRedirect[ndrum] = total_sample++;
                                    blSampleUsed[ndrum] = true;
                                }

                                // Log digidrum
                                arrSI[voice] = new SampleInstance(SpecialFXType.ATARI_DIGIDRUM, sampleRedirect[ndrum], prediv, count, nbFx);
                                //arrSampleLog.get(pos)[voice] = new SampleInstance(SampleInstance.FX_TYPE_ATARI_DIGIDRUM, sampleRedirect[ndrum], prediv, count);
                            }
                        }
                    break;

                case 0xc0:        // Sync-Buzzer.
                    tmpFreq = mfpPrediv[prediv] * count;
                    if (tmpFreq != 0) {
                        blnSpecialFX = true;
                        tmpFreq = MFP_CLOCK / tmpFreq;
                        int Env = (pReg[voice + 8] & 15);
                        Tools.debug("+ Sync-Buzzer [" + Env + ", " + tmpFreq + "]");
                        arrSI[voice] = new SampleInstance(SpecialFXType.ATARI_SYNCBUZZER,
                                Env,
                                prediv,
                                count,
                                nbFx);
                    }
                    break;
            }
        }
        return blnSpecialFX;
    }

    @Override
    public String getMenuLabel() {
        return "YM Format";
    }
}
