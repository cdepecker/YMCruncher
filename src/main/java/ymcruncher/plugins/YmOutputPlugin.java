package ymcruncher.plugins;

import com.google.auto.service.AutoService;
import ymcruncher.core.Chiptune;
import ymcruncher.core.Frame;
import ymcruncher.core.OutputPlugin;
import ymcruncher.core.YMC_Tools;

import java.util.ArrayList;

@AutoService(OutputPlugin.class)
public class YmOutputPlugin extends OutputPlugin {

    final private static int[] mfpPrediv = {0, 4, 10, 16, 50, 64, 100, 200};
    final private static long MFP_CLOCK = 2457600L;
    // Attribute values for YM5/6
    final private static byte A_STREAMINTERLEAVED = 1;
    final private static byte A_DRUMSIGNED = 2;
    final private static byte A_DRUM4BITS = 4;
    final private static byte A_TIMECONTROL = 8;
    final private static byte A_LOOPMODE = 16;
    // Parameters
    private static String PARAM_4BITS_DIGI = "Convert 8bits samples to 4bits";
    private static String PARAM_DRUMSIGNED = "Use Signed Samples";
    private static String PARAM_STREAMINTERLEAVED = "Interleave Data";
    // Digidrums store
    // Note : It could be a good idea to use those registers rather than the original (to be able to play 3 special effects at once)
    // - 1,8,14
    // - 3,9,15
    // - 5,10,16
    final private byte[][] regDigiDrums =
            {
                    {1, 6, 14},
                    {3, 8, 15}
            };

    /**
     * Constructor
     */
    public YmOutputPlugin() {
        // Init Boolean parameters
        setBooleanOption(PARAM_4BITS_DIGI, new Boolean(true));
        setBooleanOption(PARAM_DRUMSIGNED, new Boolean(false));
        setBooleanOption(PARAM_STREAMINTERLEAVED, new Boolean(true));
    }

    /**
     * Hinerited function that should return the String that will be displayed in the Menu
     *
     * @return String
     */
    public String getMenuLabel() {
        return "YM Format";
    }

    /**
     * Inherited class that should return the crunched chiptunes
     * This should include the header of the file so that the only action needed
     * by the Core is to persist data in a file
     *
     * @param arrPSGValues PSG values that will be crunched
     * @return ArrayList<Byte> which will be persisted
     */
    @SuppressWarnings("unchecked")
    public ArrayList<Byte> doCrunch(String strDestFile, Chiptune chiptune) {
        ArrayList<Byte> arrYmBytes = new ArrayList<Byte>();

        // PSG values
        ArrayList<Frame> arrFrame = chiptune.getArrFrame();

        // Digidrums
        Sample digidrums[] = chiptune.getArrSamples();
        //ArrayList<SampleInstance[]> sampleLog = chiptune.getSamplesLog();
        int intNbDigiDrums = (digidrums == null) ? 0 : digidrums.length;

        // Set Parameters
        byte bytParam = 0;
        if (getBooleanOption(PARAM_STREAMINTERLEAVED)) bytParam |= A_STREAMINTERLEAVED;
        if (getBooleanOption(PARAM_4BITS_DIGI)) bytParam |= A_DRUM4BITS;
        if (getBooleanOption(PARAM_DRUMSIGNED)) bytParam |= A_DRUMSIGNED;
        bytParam |= (chiptune.isBlnLoop() ? A_LOOPMODE : 0);

        // Header
        addStringToArray(arrYmBytes, "YM6!", false);
        addStringToArray(arrYmBytes, "LeOnArD!", false);
        addBEvar(arrYmBytes, chiptune.getLength(), 4); // NbFrames
        addBEvar(arrYmBytes, bytParam, 4); // Attributes
        addBEvar(arrYmBytes, intNbDigiDrums, 2); // Nb of Samples
        addBEvar(arrYmBytes, chiptune.getFrequency(), 4); // 1000000hz
        addBEvar(arrYmBytes, chiptune.getPlayRate(), 2); // 50hz
        addBEvar(arrYmBytes, chiptune.getLoopVBL(), 4); // Loop frame
        addBEvar(arrYmBytes, 0, 2); // Additional data

        // Digidrums
        for (int i = 0; i < intNbDigiDrums; i++) {
            // Digidrum length
            addBEvar(arrYmBytes, digidrums[i].getLength(), 4);

            // Sample data
            for (int j = 0; j < digidrums[i].getLength(); j++) {
                int bDD = digidrums[i].getWave()[j];

                // Convert 8bits samples to 4 bits samples
                if (digidrums[i].getResolution() == 8) {
                    // Convert signed sample to unsigned
                    if (!getBooleanOption(PARAM_DRUMSIGNED) && digidrums[i].isSigned())
                        bDD += 128;

                    bDD &= 0xFF;

                    if (getBooleanOption(PARAM_4BITS_DIGI))
                        bDD = arr8to4bits[bDD];
                } else {
                    // Convert signed sample to unsigned
                    if (!getBooleanOption(PARAM_DRUMSIGNED) && digidrums[i].isSigned())
                        bDD += 8;

                    bDD &= 0xF;

                    // Convert 4bits samples to 8 bits samples
                    if (!getBooleanOption(PARAM_4BITS_DIGI))
                        bDD = arr4to8bits[bDD];

                }

                addBEvar(arrYmBytes, bDD, 1);
            }
        }

        // Song Name + Author Name + Comments
        addStringToArray(arrYmBytes, chiptune.getStrSongName(), true);
        addStringToArray(arrYmBytes, chiptune.getStrAuthorName(), true);
        addStringToArray(arrYmBytes, "Generated by main.YMCruncher (debug)", true);

        // Create a 16 Register Array of values
        byte arrPSG16[][] = new byte[chiptune.getLength()][16];
        for (int i = 0; i < chiptune.getLength(); i++) {
            // copy 13 first classic registers
            Frame frame = arrFrame.get(i);
            arrPSG16[i] = frame.getByteArray();

            // Detects Digidrums
            int nbRegOnThisRow = 0;
            for (byte c = 0; (c < 3) && nbRegOnThisRow < 2; c++) {
                SampleInstance curr_sample = frame.getSI(c);
                if (curr_sample != null) {
                    /**
                     * We have a sample here
                     */

                    /**
                     * Set TP & TC
                     */
                    int prediv_indice;
                    int timer_count;
                    if (curr_sample.isAtari()) {
                        prediv_indice = curr_sample.getTp();
                        timer_count = curr_sample.getTc();
                    } else {
                        // Try to find the best couple prediv/count for this sample rate
                        prediv_indice = 1;
                        timer_count = 1;
                        double dbDiff = Math.abs(curr_sample.getRate() - (MFP_CLOCK / mfpPrediv[1]));
                        for (int p = 1; p < mfpPrediv.length; p++) {
                            for (int y = 1; y < 256; y++) {
                                double rate_aux = MFP_CLOCK / (y * mfpPrediv[p]);
                                double dbDiffAux = Math.abs(curr_sample.getRate() - rate_aux);

                                if (dbDiffAux < dbDiff) {
                                    // Better match
                                    dbDiff = dbDiffAux;
                                    prediv_indice = p;
                                    timer_count = y;
                                }
                            }
                        }

                        // TODO a rate conversion might be required if the calculated rate is not equal
                        // to the requested rate
                    }

                    // Set TP/TC
                    int intRegTp = regDigiDrums[nbRegOnThisRow][1];
                    int intRegTc = regDigiDrums[nbRegOnThisRow][2];
                    arrPSG16[i][intRegTp] |= prediv_indice << 5; // TP
                    arrPSG16[i][intRegTc] = (byte) timer_count; // TC

                    // Enable Fx and set channel
                    int intRegChannel = regDigiDrums[nbRegOnThisRow][0];
                    arrPSG16[i][intRegChannel] |= (c + 1) << 4;

                    // Sample or Vmax or Env
                    arrPSG16[i][8 + c] = (byte) curr_sample.getSample();

                    switch (curr_sample.getType()) {
                        case ATARI_SIDVOICE:
                            arrPSG16[i][intRegChannel] |= 0x00;
                            break;
                        case ATARI_SINSID:
                            arrPSG16[i][intRegChannel] |= 0x80;
                            break;
                        case ATARI_SYNCBUZZER:
                            arrPSG16[i][intRegChannel] |= 0xC0;
                            arrPSG16[i][8 + c] |= 0x10;
                            break;
                        default://SampleInstance.FX_TYPE_ATARI_DIGIDRUM:
                            arrPSG16[i][intRegChannel] |= 0x40;
                    }

                    // DEBUG
                    if (nbRegOnThisRow == 0) YMC_Tools.debug("+ Tick : " + i);
                    YMC_Tools.debug("  - voice : " + c + ", sample" + curr_sample.getSample() + " " + MFP_CLOCK / (timer_count * mfpPrediv[prediv_indice]) + "hz (original=" + curr_sample.getRate() + ")");

                    // increase the number of Digidrums on this row
                    nbRegOnThisRow++;
                }
            }
        }

        // Data (interleaved format)
        if (getBooleanOption(PARAM_STREAMINTERLEAVED))
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < chiptune.getLength(); j++)
                    arrYmBytes.add(arrPSG16[j][i]);

                // Notify ratio Completion
                setCompletionRatio(i * 100 / (YMC_Tools.CPC_REGISTERS + 1));
            }
        else // Data (Uninterleaved format)
            for (int j = 0; j < chiptune.getLength(); j++) {
                for (int i = 0; i < 16; i++)
                    arrYmBytes.add(arrPSG16[j][i]);

                // Notify ratio Completion
                setCompletionRatio(j * 100 / (chiptune.getLength()));
            }

        // Footer
        addStringToArray(arrYmBytes, "End!", false);

        // Return array of Bytes to be written
        return arrYmBytes;
    }

    /**
     * Should return the Plugin extension (i.e. ".ayc")
     *
     * @return String fileextension
     */
    public String getExtension() {
        return "ym";
    }
}
