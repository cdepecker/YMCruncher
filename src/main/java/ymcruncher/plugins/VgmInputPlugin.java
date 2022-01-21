package ymcruncher.plugins;

import com.google.auto.service.AutoService;
import ymcruncher.core.Chiptune;
import ymcruncher.core.Frame;
import ymcruncher.core.InputPlugin;
import ymcruncher.core.YMC_Tools;

import java.util.ArrayList;

/**
 * This class handle the VGM chiptune format input to the core cruncher.
 * VGM is a format defined originally for Sega Master System compatible PSG chiptunes but has been extended to handle
 * other formats (genesis ,etc ...).
 * Anyway, we can only try to convert SMS PSG chitunes (tones should be convertible, but the noise register isn't managed the same way on cpc)
 * Notes :
 * - Volume outputs are attenuation of 2db where 0 means full volume and 0xF means silence.
 * - AY-3-8910 uses 3db scales for volumes, then we may need to adapt volume level
 *
 * @author F-key/RevivaL
 */
@AutoService(InputPlugin.class)
public class VgmInputPlugin extends InputPlugin {

    /**
     * Static Vgm commands
     */
    private static final byte GG_STEREO = 0x4F;
    private static final byte SN76489_WRITE = 0x50;
    private static final byte YM2413_WRITE = 0x51;
    private static final byte YM2612_0_WRITE = 0x52;
    private static final byte YM2612_1_WRITE = 0x53;
    private static final byte YM2151_WRITE = 0x54;

    /* 0x7X = Wait X+1 samples*/
    private static final byte WAITN0 = 0x70;
    private static final byte WAITN1 = 0x71;
    private static final byte WAITN2 = 0x72;
    private static final byte WAITN3 = 0x73;
    private static final byte WAITN4 = 0x74;
    private static final byte WAITN5 = 0x75;
    private static final byte WAITN6 = 0x76;
    private static final byte WAITN7 = 0x77;
    private static final byte WAITN8 = 0x78;
    private static final byte WAITN9 = 0x79;
    private static final byte WAITNA = 0x7A;
    private static final byte WAITNB = 0x7B;
    private static final byte WAITNC = 0x7C;
    private static final byte WAITND = 0x7D;
    private static final byte WAITNE = 0x7E;
    private static final byte WAITNF = 0x7F;
    /* 0x7X = Wait X+1 samples*/

    private static final byte WAITN = 0x61;
    private static final byte WAIT_60 = 0x62;
    private static final byte WAIT_50 = 0x63;
    private static final byte END_SOUND = 0x66;

    /**
     * Static Vgm data values
     */
    private static final byte LATCH_DATA = (byte) 0x80;

    // Parameters
    //private static String PARAM_ADJUST_VOLUME = "Adjust the Volume to CPC chip (AY-3-8910)";

    private static byte bytVolumeConvertion[] =
            {15, 14, 14, 13, 12, 12, 11, 10, 10, 9, 8, 8, 7, 6, 6, 0};
    // old vol table {10, 9, 9, 8, 7, 7, 6, 5, 5, 4, 3, 3, 2, 1, 1, 0};

    /**
     * This method should basically return an array of PSG registers' values
     * Let's call Rx the Register number x, then this function should return
     * an array of all the R0 values for all frames then all the R1 values for all frames
     * and so on ...
     *
     * @param arrRawChiptune ArrayList representing the uncompressed inputed chiptune
     * @return ArrayList that should contain the PSG registers' values.
     */
    protected ArrayList getPSGRegistersValues(ArrayList arrRawChiptune, String strExt) {
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Chiptune getPreProcessedChiptune(ArrayList arrRawChiptune, String strExt) {
        // Get type of file
        strFileType = YMC_Tools.getString(arrRawChiptune, 0, 4);
        if (strFileType.subSequence(0, 3).equals("Vgm")) {
            //ArrayList<Byte> arrRegistersValues = null;
            //arrRegistersValues = new ArrayList<Byte>();
            ArrayList<Frame> arrFrame = new ArrayList();

            // Let's get the header
            Long intVersion = YMC_Tools.getLEInt(arrRawChiptune, 0x8);
            Long intFrequency = YMC_Tools.getLEInt(arrRawChiptune, 0xC);
            Long intRate = YMC_Tools.getLEInt(arrRawChiptune, 0x24);
            Long intVGMdataOffset = YMC_Tools.getLEInt(arrRawChiptune, 0x34);
            YMC_Tools.info("+ VGM version = 0x" + Long.toHexString(intVersion.longValue()).toUpperCase());
            YMC_Tools.info("+ SN76489 clock = " + intFrequency.longValue() + "Hz");
            YMC_Tools.info("+ Player Frequency = " + intRate.longValue() + "Hz");
            YMC_Tools.info("+ VGM data offset = " + Long.toHexString(intVGMdataOffset.longValue()));

            // set Frequency for conversion to cpc
            intRate = (intRate == 0) ? 60 : intRate;
            intFrequency = new Long(intFrequency.longValue() / 2);
            int intPlayRate = intRate.intValue();

            // Nb samples in a frame
            int nbSampleFrame = (intRate == 60) ? 735 : 882;

            // Useless values in the header
            YMC_Tools.debug("+ EoF offset = 0x" + Long.toHexString(YMC_Tools.getLEInt(arrRawChiptune, 0x4).longValue()).toUpperCase());
            YMC_Tools.debug("+ YM2413 clock = " + YMC_Tools.getLEInt(arrRawChiptune, 0x10).longValue() + "Hz");
            YMC_Tools.debug("+ GD3 Offset = 0x" + Long.toHexString(YMC_Tools.getLEInt(arrRawChiptune, 0x14).longValue()).toUpperCase());
            YMC_Tools.debug("+ Total # samples = " + YMC_Tools.getLEInt(arrRawChiptune, 0x18).longValue());
            YMC_Tools.debug("+ Loop offset = 0x" + Long.toHexString(YMC_Tools.getLEInt(arrRawChiptune, 0x1C).longValue()).toUpperCase());
            YMC_Tools.debug("+ Loop # samples = " + YMC_Tools.getLEInt(arrRawChiptune, 0x20).longValue());
            YMC_Tools.debug("+ SN76489 feedback = 0x" + Long.toHexString(YMC_Tools.getLEShort(arrRawChiptune, 0x28).longValue()).toUpperCase());
            YMC_Tools.debug("+ SN76489 shift register width = 0x" + Long.toHexString(YMC_Tools.getLEByte(arrRawChiptune, 0x2A).longValue()).toUpperCase());
            YMC_Tools.debug("+ YM2612 clock = " + YMC_Tools.getLEInt(arrRawChiptune, 0x2C).longValue() + "Hz");
            YMC_Tools.debug("+ YM2151 clock = " + YMC_Tools.getLEInt(arrRawChiptune, 0x30).longValue() + "Hz");

            // Check version
            if ((intVersion <= 0x110) || intVGMdataOffset == 0) intVGMdataOffset = new Long(0x40L);

            Frame frame = new Frame(
                    new double[]{0, 0, 0},    // Freq channels 0,1,2
                    0,                        // Freq Noise
                    0,                        // Freq Env
                    (byte) 0x38,                // Mixer (no noise at start)
                    new byte[]{0, 0, 0},        // Vol channels 0,1,2
                    (byte) 0xFF,                // Env
                    new SampleInstance[]{null, null, null} // SampleInstances
            );
            byte NoiseVol = 0;
            byte NoiseControl = 0;

            // Usefull
            int intChannel = 0;
            int intType = 0;

            // debug
            int intWait = 0;

            // Wait nbsamples
            Integer shtWait = 0;

            // Parse all commands
            boolean blnEnd = false;
            int intOffset = intVGMdataOffset.intValue();
            if (intVersion <= 0x101) intOffset = (intOffset == 0) ? 0x40 : intOffset;
            else intOffset += 0x34;
            while (intOffset < arrRawChiptune.size() && !blnEnd) {
                // Waiting ?
                if (shtWait > nbSampleFrame) {
                    // nbsample to wait > nbsamples per frame
                    shtWait -= nbSampleFrame;

                    // Modify local frame to include noise
                    frame = getFrameWithNoise(frame, NoiseVol, NoiseControl);

                    // Add a clone version of the local frame to the array
                    arrFrame.add(frame.copyclone());
                    intWait++;
                } else {
                    byte cmd = (Byte) arrRawChiptune.get(intOffset++);
                    switch (cmd) {
                        case GG_STEREO:
                            intOffset++;
                            break;
                        case WAITN0:
                        case WAITN1:
                        case WAITN2:
                        case WAITN3:
                        case WAITN4:
                        case WAITN5:
                        case WAITN6:
                        case WAITN7:
                        case WAITN8:
                        case WAITN9:
                        case WAITNA:
                        case WAITNB:
                        case WAITNC:
                        case WAITND:
                        case WAITNE:
                        case WAITNF:
                            // Get NbSample
                            byte nWait = (byte) ((cmd & 0xF) + 1);
                            shtWait += nWait;
                            break;
                        case WAITN:
                            shtWait += YMC_Tools.getLEShort(arrRawChiptune, intOffset);
                            intOffset += 2;
                            break;
                        case WAIT_50:
                        case WAIT_60:
                            // Modify local frame to include noise
                            frame = getFrameWithNoise(frame, NoiseVol, NoiseControl);

                            // Add a clone version of the local frame to the array
                            arrFrame.add(frame.copyclone());
                            intWait++;
                            break;
                        case SN76489_WRITE:
                            byte bytPSGvalue = (Byte) arrRawChiptune.get(intOffset++);
                            int intValue = 0;
                            switch (bytPSGvalue & LATCH_DATA) {
                                case LATCH_DATA:
                                    intValue = bytPSGvalue & 0x0F;
                                    intChannel = (bytPSGvalue & 0x60) >> 5;
                                    intType = (bytPSGvalue & 0x10) >> 4; // 0 = tone, 1 = volume

                                    switch (intType) {
                                        case 1:    // volume
                                            switch (intChannel) {
                                                case 3:    // Noise channel
                                                    NoiseVol = (byte) (~intValue & 0x0F);
                                                    break;
                                                default: // Channels 0,1,2
                                                    frame.setBytVol(intChannel, bytVolumeConvertion[intValue]);
                                                    //frame.setBytVol(intChannel, (byte)(~intValue & 0x0F));
                                            }
                                            break;
                                        default:    // tone
                                            switch (intChannel) {
                                                case 3:    // Noise channel
                                                    NoiseControl = (byte) (intValue & 0x7);
                                                    break;
                                                default: // Channels 0,1,2
                                                    int dbFreq = (int) frame.getPPeriod(intChannel);
                                                    dbFreq &= 0xF0;
                                                    dbFreq |= intValue;
                                                    frame.setPPeriod(intChannel, dbFreq);
                                            }
                                    }
                                    break;
                                default:    // Extra Data
                                    intValue = bytPSGvalue & 0x3F;

                                    switch (intType) {
                                        case 1:    // volume
                                            switch (intChannel) {
                                                case 3:    // Noise channel
                                                    NoiseVol = (byte) (~intValue & 0x0F);
                                                    break;
                                                default: // Channels 0,1,2
                                                    frame.setBytVol(intChannel, bytVolumeConvertion[intValue]);
                                                    //frame.setBytVol(intChannel, (byte)(~intValue & 0x0F));
                                            }
                                            break;
                                        default:    // tone
                                            switch (intChannel) {
                                                case 3:    // Noise channel
                                                    NoiseControl = (byte) (intValue & 0x7);
                                                    break;
                                                default: // Channels 0,1,2
                                                    int dbFreq = (int) frame.getPPeriod(intChannel);
                                                    dbFreq &= 0x0F;
                                                    dbFreq |= (intValue << 4);
                                                    frame.setPPeriod(intChannel, dbFreq);
                                            }

                                    }
                            }
                            break;
                        case END_SOUND:
                            blnEnd = true;
                            break;
                        case YM2413_WRITE: // Those commands are ignored
                        case YM2612_0_WRITE:
                        case YM2612_1_WRITE:
                        case YM2151_WRITE:
                            int intCmd2 = cmd & 0xFF;
                            YMC_Tools.debug("! Ignored VGM command : 0x" + Integer.toHexString(intCmd2) + " [Offset : 0x" + Integer.toHexString(intOffset) + "]");
                            intOffset += 2;
                            break;
                        default:
                            int intCmd = cmd & 0xFF;
                            if ((intCmd >= 0x30) && (intCmd <= 0x4e)) intOffset++;            // Reserved
                            else if ((intCmd >= 0x55) && (intCmd <= 0x5f)) intOffset += 2;    // Reserved
                            else if ((intCmd >= 0xa0) && (intCmd <= 0xbf)) intOffset += 2;    // Reserved
                            else if ((intCmd >= 0xc0) && (intCmd <= 0xdf)) intOffset += 3;    // Reserved
                            else if ((intCmd >= 0xe1) && (intCmd <= 0xff)) intOffset += 4;    // Reserved
                            else
                                YMC_Tools.info("! Unsupported VGM command : 0x" + Integer.toHexString(intCmd) + " [Offset : 0x" + Integer.toHexString(intOffset) + "]");
                    }
                }
            }

            // Return if nothing
            if (arrFrame == null) return null;

            return new Chiptune(null,
                    null,
                    arrFrame,
                    intPlayRate,
                    intFrequency.longValue(),
                    false,
                    0,
                    null);

        }

        // Not a VGM file
        return null;
    }

    /**
     * Private function to pouplate Noise info for VGM converted tunes
     */
    private Frame getFrameWithNoise(Frame frame, byte NoiseVol, byte NoiseControl) {
        /**
         * Low 2 bits    Value counter
         of register    is reset to
         00            0x10
         01            0x20
         10            0x40
         11            Tone2
         */
        if (NoiseVol > 0) {
            // Ratio Noise
            double dbNoiseFrequency;
            switch ((int) NoiseControl & 0x03) {
                case 0:
                    dbNoiseFrequency = 0x10;
                    break;
                case 1:
                    dbNoiseFrequency = 0x20;
                    break;
                case 2:
                    dbNoiseFrequency = 0x30;
                    break;
                default:
                    dbNoiseFrequency = frame.getPPeriod(2);
            }

            frame.setPPeriodN(dbNoiseFrequency);

            // Enable Noise
            byte bytNoiseFilter = getNoiseMixer(NoiseVol, frame.getBytVol(0), frame.getBytVol(1), frame.getBytVol(2));
            frame.setBytReg7((byte) (frame.getBytReg7() & ~(bytNoiseFilter << 3)));
        } else {
            //arrRegistersValues.set(intOffBase+6,(byte)0);
            frame.setPPeriodN(0);

            // Disable Noise
            frame.setBytReg7((byte) (frame.getBytReg7() | (0x7 << 3)));
        }
        return frame;
    }

    /**
     * This function is intended to return the noise mixer control Flag that fits best
     * the Noise volume provided in the VGM file.
     * AY doesn't have a noise volume register so we are simply mapping noise to 0, 1, 2 or 3 channels
     * in order to emulate a volume control
     */
    private byte getNoiseMixer(byte bytNoiseVol, byte A, byte B, byte C) {
        int intOffset = 0;
        int intDiff = bytNoiseVol;
        int arrNoiseFilter[] = {0, C, B, B + C, A, A + C, A + B, A + B + C};
        for (int i = 1; i < arrNoiseFilter.length; i++) {
            int intDiffAux = Math.abs(arrNoiseFilter[i] - bytVolumeConvertion[bytNoiseVol]);
            if (intDiffAux < intDiff) {
                // Better match
                intDiff = intDiffAux;
                intOffset = i;
            }
        }
        return (byte) intOffset;
    }
}
