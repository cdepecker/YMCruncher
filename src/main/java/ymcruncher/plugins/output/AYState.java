package ymcruncher.plugins.output;

import ymcruncher.core.SpecialFXType;

import java.util.Arrays;

public class AYState {
    // Sinus SID (8 bytes sinus per volume)
    public static final int[][] SinSidVol = new int[16][8];
    // Envelope Masks
    private static byte ENV_CONT = 0x8;
    private static byte ENV_ATT = 0x4;
    private static byte ENV_ALT = 0x2;
    private static byte ENV_HOLD = 0x8;

    // Envelope Patterns
    // 0x10 or 0x1F means Hold value
    //
    // Patterns :
    // 00xx \_
    // 01xx /_
    // 1000 \\
    // 1001 \_
    // 1010 \/
    // 1011 \-
    // 1100 //
    // 1101 /-
    // 1110 /\
    // 1111 /_
    private static byte Env[][] = {
            {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0x10},    // 00xx
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0x10},    // 01xx
            {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0},        // 1000
            {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0x10},    // 1001
            {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},        // 1010
            {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0x1F},    // 1011
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},        // 1100
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0x1F},    // 1101
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0},        // 1110
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0x10},    // 1111
    };

    static {
        /**
         * Build Sinus Sid volume convertion table.
         * We need that because of the Envelope generator which has to look for volume levels during
         * convertion
         */

        for (int j = 0; j < 8; j++) {
            int a = (int) (j * 2 * Math.PI / 8);
            int vol = (int) (0x7F * (Math.sin(a) + 1));
            for (int i = 0; i < 16; i++)
                //SinSidVol[i][j] = i*vol/15;
                SinSidVol[i][j] = i * vol / 16;
        }
    }

    // Convert 4bits to 8bits sample (Extracted from STSound by Leonard)
    final protected byte[] arr4to8bits = {0x0, 0x1, 0x2, 0x2, 0x4, 0x6, 0x9, 0xC, 0x11, 0x18, 0x23, 0x30, 0x48, 0x67, (byte) 0xA5, (byte) 0xFF};
    // Noise (Pseudo Random)
    public int srNoise = 1;        // Shift Register
    public int noiseSign = 1;        // Sign (Can be 0 or 1)
    // Envelope Pattern Schema(Register 13)
    public byte bytEnvPat = 0;
    public byte bytEnvVolCnt = 0;
    // Periodes (Tones + Noise + Envelope)
    // Counters (Tones + Noise + Envelope)
    // Volumes (Tones)
    public long periodes[] = new long[]{0, 0, 0, 0, 0};
    public long counters[] = new long[]{0, 0, 0, 0, 0};
    public int volumes[] = new int[]{0, 0, 0};
    // Samples (Digidrums)
    public int index_samples[] = new int[]{0, 0, 0};
    public long counters_samples[] = new long[]{0, 0, 0};
    public double rate_samples[] = new double[]{0, 0, 0};
    public int wav_samples[][] = new int[][]{null, null, null};
    // SID Voices
    public int SidVol[] = new int[]{0, 0, 0};
    public int SidMax[] = new int[]{0, 0, 0};
    public byte SinSidIndex[] = new byte[]{0, 0, 0};
    // Sync Buzzer
    public byte Buzzer[] = new byte[]{0, 0, 0};
    // Special FX
    public SpecialFXType bytType[] = new SpecialFXType[]{SpecialFXType.NO_FX, SpecialFXType.NO_FX, SpecialFXType.NO_FX};

    /* Constructor */
    public void AYSate() {
        Arrays.fill(counters, (long) 0);
        Arrays.fill(periodes, (long) 0);
        Arrays.fill(volumes, (int) 0);
        srNoise = 1;
        noiseSign = 1;
    }

    public int getEnvVolume(long dbStep) {

        // If period == 0
        // if (periodes[4] == 0) return 0;

        // get index in byte Envelope volumes
        int intEnvPat = ((bytEnvPat & ENV_CONT) == 0) ?
                (bytEnvPat >> 2) & 0x1 :
                (bytEnvPat & 0x7) + 2;

        byte bytReturn = Env[intEnvPat][bytEnvVolCnt];

        // calculate next indexp
        counters[4] += dbStep;
        if ((periodes[4] != 0) && (counters[4] >= periodes[4]))    // Start another period
        {
            counters[4] %= periodes[4];

            // inc index if not a "Hold Value"
            if ((bytReturn & 0x10) == 0) {
                bytEnvVolCnt++;
                bytEnvVolCnt &= 0x1F;
            }
        }

        return (bytReturn & 0xF);
    }

    public int getOscillatorToneSign(int channel, long dbStep) {
        // Assume output always 1 if 0 period (for Digi-sample !)
        if (periodes[channel] == 0) return 1;

        int intReturn = 0;

        if (counters[channel] < (periodes[channel] / 2))    // High state
            intReturn = 1;
        else // Low state
            //intReturn = -1;
            intReturn = 0;

        // calculate next indexp
        counters[channel] += dbStep;
        if (counters[channel] >= periodes[channel])    // Start another period
        {
            counters[channel] %= periodes[channel];
        }

        return intReturn;
    }


    public int getOscillatorNoiseSign(long dbStep) {
        // If period = 0
        if (periodes[3] == 0) return 0;

        int currentNoiseSign = noiseSign;

        // calculate next indexp
        counters[3] += dbStep;
        if (counters[3] >= periodes[3])    // Start another period
        {
            counters[3] %= periodes[3];

            // Magic function for the Noise register
            int rBit = (srNoise & 1) ^ ((srNoise >> 2) & 1);
            srNoise = (srNoise >> 1) | (rBit << 16);
            noiseSign ^= (1 - rBit);
        }

        return currentNoiseSign;
    }

    // Compute SyncBuzzer if needed
    public void syncBuzzerStep(int channel, long lngSamplePeriode) {
        // calculate next indexp
        counters_samples[channel] += rate_samples[channel];
        if (counters_samples[channel] >= lngSamplePeriode)    // Start another period
        {
            counters_samples[channel] %= lngSamplePeriode;

            // Reset Env counter
            bytEnvVolCnt = 0;
            counters[4] = 0;
        }
    }

    // Return either an unsigned 8bits
    public int getSampleVolume(int channel, long lngSamplePeriode) {
        int currentSampVol = 0;

        switch (bytType[channel]) {
            case ATARI_SIDVOICE:
                currentSampVol = 0xFF & arr4to8bits[SidVol[channel]];
                break;
            case ATARI_SINSID:
                currentSampVol = SinSidVol[SidMax[channel] & 0xF][SinSidIndex[channel]];
                break;
            case ATARI_DIGIDRUM:
            default:
                currentSampVol = wav_samples[channel][index_samples[channel]];
        }


        // calculate next indexp
        counters_samples[channel] += rate_samples[channel];
        if (counters_samples[channel] >= lngSamplePeriode)    // Start another period
        {
            counters_samples[channel] %= lngSamplePeriode;

            switch (bytType[channel]) {
                case ATARI_SIDVOICE:
                    SidVol[channel] = (SidVol[channel] == 0) ? SidMax[channel] : 0;
                    break;
                case ATARI_SINSID:
                    SinSidIndex[channel]++;
                    SinSidIndex[channel] &= 0x7;
                    break;
                case ATARI_DIGIDRUM:
                default:
                    // Increase index in sample
                    // And disable sample if finished
                    if (++index_samples[channel] >= wav_samples[channel].length)
                        //wav_samples[channel] = null;
                        bytType[channel] = SpecialFXType.NO_FX;
            }
        }

        return currentSampVol;
    }
}