package ymcruncher.plugins;

import com.google.auto.service.AutoService;
import ymcruncher.core.Chiptune;
import ymcruncher.core.Frame;
import ymcruncher.core.OutputPlugin;
import ymcruncher.core.YMC_Tools;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Convert a chiptune to wav file.
 * + Basic AY-3-891x emulation
 * - counters are not currently reset to zero when the periodes change.
 * - Additional frequencies may appear when intOverSample is not an integer value
 * - !!! Env sign is always 1 ?
 * - Digidrums support : OK
 * - Sid support : OK
 * - Sinus Sid : OK
 * - Sync Buzzer : OK
 * + Notes (StSound compare)
 * - StSound doesn't reset the Tone Oscillator only when the period has been entirely played (Is that good ?)
 * - StSound doesn't seem to set the low state of oscillator to -1 (it does zero instead)
 * - StSound does a Mono Mix by dividing the volumes by 3 for each channels and adding them, we should add them before dividing the result by 3 (more accurate)
 * - StSound resets the noise counter and disable the noise when the noise period is lower than 3 (is that right ?)
 * - StSound disable the tone when the period is lower than 5 (is that right ?)
 * + Process
 * - Get all required values for frame N
 * - Noise : periods + counter
 * - Channels : volumes + periods + counters
 * - Mixer
 * - For intOverSample
 * getOscillatorValue for Channel 0, Channel 1, Channel 2 and Noise
 * Mix them to the wav file
 *
 * @author FKey
 */
@AutoService(OutputPlugin.class)
public class WavOutputPlugin extends OutputPlugin {

    // Parameters
    private static String PARAM_WAVRATE_LIST = "Record Frequency (Hz)";
    private static String PARAM_BITRATE = "BitRate";
    private static String PARAM_STEREO = "Stereo";

    // StSound Volume Table (16bits)
	/*static	int ymVolumeTable[] = new int[]
	{	62,161,265,377,580,774,1155,1575,2260,3088,4570,6233,9330,13187,21220,32767};*/

    public WavOutputPlugin() {
        // Init List parameters
        setListOption(PARAM_WAVRATE_LIST,
                new Integer[]{
                        44100 / 4,
                        44100 / 3,
                        312 * 50,    // CPC+ DMA Standard Freq
                        44100 / 2,
                        44100,}
                , 0);

        setListOption(PARAM_BITRATE,
                new Integer[]{
                        8,
                        16}
                , 0);
        setBooleanOption(PARAM_STEREO, false);
    }

    @Override
    public ArrayList<Byte> doCrunch(String strDestFile, Chiptune chiptune) {
        // Buffer length ArrayList
        if (chiptune != null) {
            // Calculate Chiptune size
            final int intVBL = chiptune.getLength();

            // Set Parameters
            int wavRate = (Integer) getListOptionSelected(PARAM_WAVRATE_LIST);
            int nbBit = (Integer) getListOptionSelected(PARAM_BITRATE);    // 8 or 16
            int nbchannels = getBooleanOption(PARAM_STEREO) ? 2 : 1;        // Stereo or Mono
            int blockAlign = ((nbBit - 1) / 8) + 1;                            // Block align (1 or 2)
            int blockAlignTotal = nbchannels * blockAlign;
            int avgByteSec = wavRate * blockAlignTotal;

            // Log
            YMC_Tools.info("+ Wave");
            YMC_Tools.info("  - PlayMode = " + (getBooleanOption(PARAM_STEREO) ? "Stereo" : "Mono"));
            YMC_Tools.info("  - " + PARAM_WAVRATE_LIST + " = " + wavRate + " Hz");
            YMC_Tools.info("  - " + PARAM_BITRATE + " = " + nbBit + " bits");

            // OverSample (50hz <-> 44100hz)
            int intOverSample = wavRate / chiptune.getPlayRate();

            /**
             * Build volume convertion table.
             * We need that because of the Envelope generator which has to look for volume levels during
             * convertion
             */
            int intVolumeConv[] = new int[16];
            for (int i = 0; i < intVolumeConv.length; i++) {
                int vol = arr4to8bits[i];
                vol &= 0xFF;

                // vol is now a 8bit unsigned sample
                // We should make it 16 if necessary
                if (nbBit <= 8)
                    vol >>= (8 - nbBit);
                else
                    vol <<= (nbBit - 8);

                // vol should be divided by 2 (low and high state)
                // vol/=2;

                // Add it to the volume table
                intVolumeConv[i] = vol;
            }


            /**
             * Here should be the constants for pconst and dbStep
             * They have been converted to long so that we don't get decimal errors.
             * We basically multiplied both values by "chiptune.getFrequency()*wavRate"
             * double pconst = 16d/chiptune.getFrequency();
             * double dbStep = (double)1/(double)wavRate;
             */
            long pconst = wavRate * 16;
            long pconst_env = wavRate * 16;//*256;
            long dbStep = chiptune.getFrequency();

            // Wave array length
            int intWaveLength = intVBL * intOverSample * blockAlignTotal;

            //
            // Wav Header
            //
            ArrayList<Byte> arrWavFile = new ArrayList<Byte>();
            addStringToArray(arrWavFile, "RIFF", false);
            addLEvar(arrWavFile, intWaveLength + 44 - 8, 4);    // whole size
            addStringToArray(arrWavFile, "WAVE", false);

            // Format Chunk
            addStringToArray(arrWavFile, "fmt ", false);
            addLEvar(arrWavFile, 16, 4);                        // chunk size
            addLEvar(arrWavFile, 1, 2);                            // format tag (1==PCM)
            addLEvar(arrWavFile, nbchannels, 2);                // nb channels
            addLEvar(arrWavFile, (long) wavRate, 4);                // sample rate
            addLEvar(arrWavFile, (long) avgByteSec, 4);            // average bytes per second
            addLEvar(arrWavFile, blockAlignTotal, 2);            // nb Bytes / frame
            addLEvar(arrWavFile, nbBit, 2);                        // nb bits

            // Data Chunk
            addStringToArray(arrWavFile, "data", false);
            addLEvar(arrWavFile, intWaveLength, 4);

            // Write header to file
            try {
                FileOutputStream file_output = new FileOutputStream(strDestFile);

                // Copy data in file
                for (int i = 0; i < arrWavFile.size(); i++) {
                    byte bytVal = arrWavFile.get(i).byteValue();
                    file_output.write(bytVal);
                }

                // Calculate the middle value (if nbBits<=8 otherwise it's zero)
                // XXX The AY-3-8912 doesn't seem to simulate a -1/+1 flip/flop output to the mixer
                // It's more like a 0/+1 flip/flop
                //int middle = 0;
                //if (nbBit<=8) middle = 1<<(nbBit-1);
                int middle = 0;
                if (nbBit >= 16) middle = 1 << (nbBit - 1);

                /**
                 * Convert to a Wav file
                 */

                //int intWav = 0;
                int k = 0;        // index in chiptune

                // Initialize variables
                AYState ay0 = new AYState();

                // Array which will contain output volumes for each channels
                // After Mixing (Noise & Tone)
                int output_volumes[] = new int[]{0, 0, 0};

                // buffer
                byte arrWavBuffer[] = new byte[intOverSample * blockAlignTotal];

                // Write Pulses
                while (k < intVBL) {
	            	/*if (k == 766)
	            	{
	            		System.out.print("stop");
	            	}*/

                    // Every VBLs we clear the buffer then we output it to the file
                    Arrays.fill(arrWavBuffer, (byte) 0);

                    Frame frame = chiptune.getFrame(k);

                    //***********************************
                    // Get Periode + Volume for Channels
                    // + Samples
                    //***********************************
                    for (int v = 0; v < 3; v++) {
                        // Voice v
                        long p = (long) frame.getPPeriod(v);

                        // Get Real period
                        ay0.periodes[v] = p * pconst;

                        // get volume
                        ay0.volumes[v] = (int) (frame.getBytVol(v) & 0xF);

                        // Get Samples
                        // counter is incremented by the sample rate and when it reaches the wavrate,
                        // we change the index in the wave table
                        SampleInstance si = frame.getSI(v);
                        if (si != null) // We should play that sample
                        {
                            // Rate of SpecialFX
                            ay0.rate_samples[v] = si.getRate();

                            switch (si.getType()) {
                                case ATARI_SIDVOICE:
                                    ay0.SidMax[v] = si.getSample();
                                    if (ay0.bytType[v] != si.getType()) {
                                        ay0.SidVol[v] = ay0.SidMax[v];
                                        ay0.bytType[v] = si.getType();
                                    }
                                    YMC_Tools.debug("+ SidVoice " + v + " [" + ay0.SidMax[v] + ", " + ay0.rate_samples[v] + "]");
                                    break;
                                case ATARI_SINSID:
                                    ay0.SidMax[v] = si.getSample();
                                    ay0.bytType[v] = si.getType();
                                    YMC_Tools.debug("+ SinSid [" + ay0.SidMax[v] + ", " + ay0.rate_samples[v] + "]");
                                    break;
                                case ATARI_SYNCBUZZER:
                                    ay0.bytType[v] = si.getType();
                                    ay0.Buzzer[v] = (byte) (0xF & si.getSample());
                                    ay0.counters[4] = 0;
                                    ay0.bytEnvVolCnt = 0;
                                    YMC_Tools.debug("+ Sync-Buzzer [" + ay0.Buzzer[v] + ", " + ay0.rate_samples[v] + "]");
                                    break;
                                case ATARI_DIGIDRUM:
                                default:
                                    ay0.bytType[v] = si.getType();
                                    ay0.counters_samples[v] = 0;
                                    ay0.index_samples[v] = 0;
                                    ay0.wav_samples[v] = chiptune.getArrSamples()[si.getSample()].getWave8bits();
                                    YMC_Tools.debug("+ Sample [" + si.getSample() + ", " + ay0.rate_samples[v] + "]");
                            }
                        } else switch (ay0.bytType[v]) {
                            // We terminate the SpecialFX
                            case ATARI_SYNCBUZZER:
                                ay0.counters_samples[v] = 0;    // XXX Not sure ...
                            case ATARI_SIDVOICE:
                            case ATARI_SINSID:
                                ay0.bytType[v] = SpecialFXType.NO_FX;
                        }
                    }

                    //***********************************
                    // Get Periode for Noise
                    //***********************************
                    long p = (long) frame.getPPeriodN();

                    // Get Real period
                    ay0.periodes[3] = p * pconst;


                    //***********************************
                    // Get Periode for Envelope
                    //***********************************
                    p = (long) frame.getPPeriodE();

                    // Get Real period
                    ay0.periodes[4] = p * pconst_env;

                    //***********************************
                    // Get Mixer
                    //***********************************
                    byte bytMixer = frame.getBytReg7();

                    // ***********************************
                    // Get Env Pattern
                    //***********************************
                    byte bytReg13 = frame.getBytReg13();
                    if (bytReg13 != (byte) 0xFF) {
                        // Reset Envelope counter and set new Pattern
                        ay0.counters[4] = 0;
                        ay0.bytEnvVolCnt = 0;
                        ay0.bytEnvPat = bytReg13;
                    }

                    // Loop OverSample
                    int intWavOffset = 0;
                    for (int z = 0; z < intOverSample; z++) {
                        // Get Noise Sign
                        int intNoiseSign = ay0.getOscillatorNoiseSign(dbStep);

                        // Get Envelope Volume
                        int intEnvVolume = ay0.getEnvVolume(dbStep);

                        for (int v = 0; v < 3; v++) {
                            // Mixer (Enable == 1)
                            // T N Result
                            // 0 0 1 (for Sample)
                            // 0 1 intNoiseSign
                            // 1 0 intToneSign
                            // 1 1 (intNoiseSign==0)?0:intToneSign;

                            // -1, 0 or 1
                            int intToneSign = ay0.getOscillatorToneSign(v, dbStep);

                            boolean isToneEnabled = ((bytMixer & (1 << v)) == 0);
                            boolean isNoiseEnabled = ((bytMixer & (8 << v)) == 0);

                            int intSign = 1;    // DigiDrums & Samples

                            if (isToneEnabled) {
                                if (isNoiseEnabled)
                                    intSign = (intNoiseSign == 0) ? 0 : intToneSign;
                                else
                                    intSign = intToneSign;
                            } else if (isNoiseEnabled)
                                intSign = intNoiseSign;

                            // Sync Buzzer specific
                            // We process the Sync Buzzer step if a Sync Buzzer is active on this voice
                            if (ay0.bytType[v] == SpecialFXType.ATARI_SYNCBUZZER)
                                ay0.syncBuzzerStep(v, wavRate);

                            /**
                             * Note about Effect priority, the priority is as follow :
                             * 1 - Sid Voices + Sinus Sid
                             * 2 - Samples (DigiDrums)
                             * 3 - Sync Buzzer + Envelope
                             * 4 - Normal oscillator
                             */


                            // Special FX that's not Sync Buzzer (Sample, SidVoice, SinSid)
                            if (ay0.bytType[v] != SpecialFXType.NO_FX
                                    && ay0.bytType[v] != SpecialFXType.ATARI_SYNCBUZZER) {
                                // Get Output volume for that channel
                                int intSampleVol = ay0.getSampleVolume(v, wavRate);

                                // We need to use the oscillator for Sid emulation only
                                if (ay0.bytType[v] == SpecialFXType.ATARI_SIDVOICE)
                                    intSampleVol *= intSign;

                                output_volumes[v] = (nbBit == 8) ? intSampleVol/*>>1*/ : intSampleVol << 8; // Divide by 2 if 8bits or mult by 2^7 if 16bits
                            }
                            // Is envelope enable or SyncBuzzer ?
                            else if (frame.blnEnvEnable(v)
                                    || ay0.bytType[v] == SpecialFXType.ATARI_SYNCBUZZER) {
                                output_volumes[v] = intSign * intVolumeConv[intEnvVolume];
                            }
                            // NO FX
                            else {
                                // Get Output volume for that channel
                                output_volumes[v] = intVolumeConv[ay0.volumes[v]] * intSign;
                            }
                        }

                        /**
                         * Mixer (Stereo or Mono)
                         */
                        if (getBooleanOption(PARAM_STEREO)) {
                            // Left A+B/2
                            //int intSample = middle + (output_volumes[0]+output_volumes[2])/2;
                            int intSample = middle + (output_volumes[0] + output_volumes[1]) / 2;
                            setLEvar(arrWavBuffer, intWavOffset, intSample, blockAlign);
                            intWavOffset += blockAlign;

                            // Right C+B/2
                            // intSample = middle + (output_volumes[1]+output_volumes[2])/2;
                            intSample = middle + (output_volumes[2] + output_volumes[1]) / 2;
                            setLEvar(arrWavBuffer, intWavOffset, intSample, blockAlign);
                            intWavOffset += blockAlign;

                        } else {
                            // Mix channels as Mono
                            int intSample = 0;
                            for (int vol : output_volumes)
                                intSample += vol;

                            // Mono
                            intSample /= 3;

                            // origin (128 for lower than 8bits and 0 for more than 8bits)
                            intSample += middle;

                            if (intSample < 0) {
                                System.out.println("error : negative value ...");
                            }

                            // Set Sampled value within the flow
                            setLEvar(arrWavBuffer, intWavOffset, intSample, blockAlign);
                            intWavOffset += blockAlign;
                        }
                    }

                    file_output.write(arrWavBuffer);
                    k++;

                    // Notify ratio Completion
                    setCompletionRatio(k * 100 / intVBL);
                }

                // Close file
                file_output.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Override Standdard behaviour
        return null;
    }

    @Override
    public String getExtension() {
        return "wav";
    }

    @Override
    public String getMenuLabel() {
        return "Wave Format";
    }

}
