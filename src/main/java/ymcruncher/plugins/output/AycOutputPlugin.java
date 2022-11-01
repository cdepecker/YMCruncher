package ymcruncher.plugins.output;

import ymcruncher.core.Chiptune;
import ymcruncher.core.Frame;
import ymcruncher.core.OutputPlugin;
import ymcruncher.core.Tools;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.auto.service.AutoService;
import ymcruncher.core.Sample;
import ymcruncher.core.SampleInstance;

/**
 * AYC output module.
 * Notes :
 * - AYC files generated are still compatible with the old AYC players
 * - An extended header has been introduced to help with ID3 info and SpecialFX.
 * - Here's a summary of the old Header :
 * + (2) : Number of VBLs
 * + 14*
 * + (1) : Buffer size (0x1 or 0x4 respectively 0x100 0x400)
 * + (2) : Relative offset to compressed data for register
 * + (6) : reserved
 * <p>
 * -> So the Standard Header length is 2 + 14*(1+2) + 6 = 50 = 0x32
 * <p>
 * EXTENDED HEADER
 * - If relative offset to register0's compressed data is not equal to "0x2E" then there's an extended header.
 * - Extended header relative offset is a word located exactly at offset 0x2E.
 * - The extended header has been placed at the end of the file to enable demomaker to skip it easily in case of need.
 * Here is the format :
 * + (1) : AYC extended header version
 * + (1) : Playback rate (5=50hz; 4=60hz; 3=75hz; 2=100hz; 1=150hz; 0=300hz;)
 * + (1) : Nb Digidrums
 * + NT String : Chiptune Name
 * + NT String : Author Name
 * + NT String : Comments
 * <p>
 * SPECIAL FXs
 * - We use volumes registers three high bits. (Registers 8,9,10)
 * + 000xxxxx means no SpecialFX
 * + 001xxxxx means Sid Voice
 * > Volume register hold the Max Volume value (4 low bits)
 * + 010xxxxx means Sinus Sid
 * > Volume register hold Volume Value (4 low bits)
 * + 011xxxxx means SyncBuzzer
 * > Volume register hold the Envelope shape (may not be necessary - 4 low bits)
 * + 100xxxxx means Digidrums
 * > Volume register hold the Digidrum number (4 low bits)
 * + (>100xxxxx) RESERVED
 * <p>
 * ***
 * *** Thoughts
 * ***
 * <p>
 * The main purpose of the AYC format is to have the smallest size for the chitpune at runtime with minimume overhead in decompression.
 * So we define the followings :
 * - Digidrums are stored in AYList format (this doesn't make sense to have them crunched)
 *
 * @author FKey
 */
@AutoService(OutputPlugin.class)
public class AycOutputPlugin extends OutputPlugin {

    public static final String AYC_FORMAT = "AYC Format";
    public static final String AYC = "ayc";
    //private static final byte AYL_DIGIDRUM_DFLT_VOICE = (byte)0x80;
    private static final byte AYL_SET_REGISTER_COMMAND = (byte) 0x00;
    private static final byte AYL_PAUSE_COMMAND = (byte) 0x10;
    private static final int AYL_STOP_COMMAND = (int) 0x4020;
    // Parameters
    //private static final String PARAM_DIV_VOL = "Divide Volume by 2 for Chiptune that contain DigiDrums";
    private static final String PARAM_EXTENDED_HEADER = "Generate Extended Header";
    private static final String PARAM_BUFFSIZE_REG = "Buffer size Reg";
    private static final String BUFF_SIZE_0x400 = "0x400";
    private static final String BUFF_SIZE_0x100 = "0x100";

    // Atari SpecialFX
	/*private static byte AYC_SIDVOICE =     0x20;
	private static byte AYC_SINSID =       0x40;
	private static byte AYC_SYNCBUZZER =   0x60;
    private static byte AYC_DIGIDRUM =     (byte)0x80;*/
    private static final String BUFF_SIZE_AUTO = "AUTO";
    private static final String CHANNEL_A = "A";
    private static final String CHANNEL_B = "B";
    private static final String CHANNEL_C = "C";
    private static final String PARAM_SAVE_DIGIDRUMS_AYL = "Save Digidrums in AYL format";
    private static final String PARAM_AYL_DEFAULT_CHANNEL = "AYL Default Channel";
    private static final String PARAM_SAVE_ATARI_SFX_REGS = "Save Atari SpecialFX Registers";
    private static final String PARAM_SAVE_REGS_IN_SEPARATE_FILES = "Save Registers in Separate Files";
    private static final String PARAM_CHANNELS_ORDER = "Channels Ordering";
    private static String AYC_COMMENTS = "Converted using main.YMCruncher";
    private static byte AYC_VERSION = 0x00;
    private static byte AYC_EXT_HEADER_PTR = 0x32;
    private static byte AYC_DFLT_INT_PLAYRATE = 5;

    /**
     * Constructor
     */
    public AycOutputPlugin() {

        // swap channels ?
        /*
         * To swap channels we need to change
         * - frequencies (reg 0,1 or 2,3 or 4,5)
         * - volumes (reg 8 or 9 or A)
         * - filter bits (reg 7)
         * - SFX parameters (voice)
         */
        setListOption(PARAM_CHANNELS_ORDER, new String[]{
                CHANNEL_A + CHANNEL_B + CHANNEL_C,
//					CHANNEL_A + CHANNEL_C + CHANNEL_B,
                CHANNEL_B + CHANNEL_A + CHANNEL_C,
//					CHANNEL_B + CHANNEL_C + CHANNEL_A,
//					CHANNEL_C + CHANNEL_A + CHANNEL_B,
//					CHANNEL_C + CHANNEL_B + CHANNEL_A
        }, 0);

        // Init Boolean parameters
        //setBooleanOption(PARAM_DIV_VOL, new Boolean(true));
        //setBooleanOption(PARAM_EXTENDED_HEADER, new Boolean(false));
        setBooleanOption(PARAM_SAVE_DIGIDRUMS_AYL, Boolean.TRUE);
        setListOption(PARAM_AYL_DEFAULT_CHANNEL, new String[]{CHANNEL_A, CHANNEL_B, CHANNEL_C}, 2, true);

        // Trick to space options
//        setListOption(PARAM_AYL_DEFAULT_CHANNEL + "SPACE", null, 0, false);

        setBooleanOption(PARAM_SAVE_ATARI_SFX_REGS, Boolean.FALSE);
        for (int i = 0; i < Tools.YM_REGISTERS; i++)
            setListOption(PARAM_BUFFSIZE_REG + ((i < 10) ? "0" : "") + i, new String[]{BUFF_SIZE_AUTO, BUFF_SIZE_0x100, BUFF_SIZE_0x400}, 0, true);

        // Should we save all registers in separate files ?
        setBooleanOption(PARAM_SAVE_REGS_IN_SEPARATE_FILES, Boolean.FALSE);
    }

    /**
     * Hinerited function that should return the String that will be displayed in the Menu
     *
     * @return String
     */
    public String getMenuLabel() {
        return AYC_FORMAT;
    }

    /**
     * Process AYC crunching :
     *
     * @param intBuffSize int defining the Compression Buffer Size
     * @return Crunched array of Bytes
     */
    ArrayList doAYCCrunch(ArrayList arrRawData, int intBuffSize) {
        // AYC crunching is as follow :
        // - A sequence is
        //		+ A character
        //		+ A lenght + offset
        // - First Byte is a character
        // - Flags are inputed every 8 sequences and bits represents the following sequences, ie : b00000111 means:
        //		+ 5 characters
        //		+ 3 previous sequences

        // stats
        int nbFlag = 0;
        int nbPreviousSeq = 0;
        int nbCharsInSeq = 0;
        int nbChars = 1;    // first char

        ArrayList<Byte> arrCrunchedData = new ArrayList<Byte>();

        /**
         * Minimum interresting size for a previous Pattern
         * For BuffSize = 0x100, it's interresting only if the previous matching pattern size is > 1
         * offset + length + 1 bit = 17 bits > 1 char + 1 bit = 9 bits
         * offset + length + 1 bit = 17 bits < 2 chars + 2 bits = 18 bits
         * For BuffSize = 0x400, it's interresting only if the previous matching pattern size is > 2
         * offset + length + 1 bit = 25 bits > 1 char + 1 bit = 9 bits
         * offset + length + 1 bit = 25 bits > 2 chars + 2 bits = 18 bits
         * offset + length + 1 bit = 25 bits < 3 chars + 3 bits = 27 bits
         */
        //int intMinSeqSize = 3;
        int intMinSeqSize = 1;
        if (intBuffSize > 0x100) intMinSeqSize++;

        // First Byte
        byte bytData = ((Byte) arrRawData.get(0)).byteValue();
        arrCrunchedData.add(new Byte(bytData));

        // Main Crunching Loop
        int curr_index = 1;
        BitSet bsFlag = new BitSet(8);
        bsFlag.clear();
        int intOffsetFlag = 1;
        int intSeqCounter = 8;

        while (curr_index < arrRawData.size()) {
            // Put flag if needed
            if (intSeqCounter <= 0) {
                // Reset number of sequences
                intSeqCounter = 8;

                // Insert Flag in flow
                arrCrunchedData.add(intOffsetFlag, bsToByte(bsFlag));
                intOffsetFlag = arrCrunchedData.size();

                // Clear Flag
                bsFlag.clear();

                // stats
                nbFlag++;
            }

            /********************************************************
             * Fast Crunching method                                *
             * We test until a byte isn't matching the buffered one *
             ********************************************************/

            // get Min index in buffer
            int index_min = curr_index - intBuffSize;
            index_min = (index_min < 0) ? 0 : index_min;
            int index_buffer = curr_index - 1;

            // get Max index in buffer + 1
            int index_max = curr_index + 0x100;
            index_max = (index_max > arrRawData.size()) ? arrRawData.size() : index_max;

            // Set previous matching sequence size and index
            int intSeqSize = 0;
            int intSeqIndex = 0;

            // Loop through all possible matches in the buffer
            while (index_buffer >= index_min) {
                // Current matching Sequence size
                int intCurrSeqSize = 0;

                // Floating index
                int indexFloat = curr_index;
                int indexFloatBuffer = index_buffer;

                // Increase Sequence size
                // When bytes are equal
                while (indexFloat < index_max
                        && ((Byte) arrRawData.get(indexFloatBuffer++)).equals((Byte) arrRawData.get(indexFloat++))) {
                    intCurrSeqSize++;
                }

                // If CurrCalculated Size is bigger than the previous one, swap them
                if (intCurrSeqSize > intSeqSize) {
                    intSeqSize = intCurrSeqSize;
                    intSeqIndex = index_buffer;
                }

                // Decrease index in buffer
                index_buffer--;
            }

            if (intSeqSize > intMinSeqSize) {
                // A previously uncrunched pattern has been found
                // We need to:
                // - neg the length
                // - calculate the offset of previous maching Pattern in the buffer
                // - set bit Flag
                // - decrement Sequence Counter
                // - increment curr_index

                // length
                byte bytNegLength = (byte) ((-intSeqSize) & 0xFF);
                arrCrunchedData.add(new Byte(bytNegLength));

                // Offset
                if (intBuffSize <= 0x100) {
                    // Previous Offset
                    byte bytePrevOffset = (byte) intSeqIndex;

                    // Write 0x100 Offset
                    arrCrunchedData.add(new Byte(bytePrevOffset));
                } else {
                    // Previous Offset
                    short shortPrevOffset = (short) intSeqIndex;

                    // Write 0x400 Offset
                    arrCrunchedData.add(new Byte((byte) (shortPrevOffset & 0xFF)));
                    arrCrunchedData.add(new Byte((byte) (shortPrevOffset >> 8 & 0x3)));
                }

                // Sequence Counter
                intSeqCounter--;

                // set bit Flag
                bsFlag.set(intSeqCounter);

                // Inc index
                curr_index += intSeqSize;

                // stats
                nbPreviousSeq++;
                nbCharsInSeq += intSeqSize;
            } else {
                // No Pattern found (put a character)
                // - put character
                // - unset bit Flag (nothing to do)
                // - decrement Sequence Counter
                // - increment curr_index

                // New character
                bytData = ((Byte) arrRawData.get(curr_index)).byteValue();
                arrCrunchedData.add(new Byte(bytData));

                // Sequence Counter
                intSeqCounter--;

                // Inc index
                curr_index++;

                // stats
                nbChars++;
            }
        }

        // Insert Final Flag in flow
        arrCrunchedData.add(intOffsetFlag, bsToByte(bsFlag));

        // Print Infos:
        //debug("  - Old Size : " + arrRawData.size());
        Tools.debug("  - BufferSize : 0x" + Long.toHexString(intBuffSize).toUpperCase() + " bytes");
        Tools.debug("  - New Size : " + arrCrunchedData.size() + " bytes");
        Tools.debug("  - Compression ratio ~ " + (float) arrCrunchedData.size() * 100 / arrRawData.size() + " %");

        // stats
        Tools.debug("  ! Nb Flags : " + nbFlag);
        Tools.debug("  ! Nb PreviousSeq : " + nbPreviousSeq);
        Tools.debug("  ! Nb Chars : " + nbChars);
        Tools.debug("  ! Nb CharsInPreviousSeq : " + nbCharsInSeq);

        // return Crunched ArrayList
        return arrCrunchedData;
    }

    /**
     * Inherited class that should return the crunched chiptunes
     * This should include the header of the file so that the only action needed
     * by the Core is to persist data in a file
     *
     * @param strDestFile Destination file path
     * @param chiptune    Chiptune being crunched
     * @return ArrayList<Byte> which will be persisted
     */
    @SuppressWarnings("unchecked")
    public ArrayList<Byte> doCrunch(String strDestFile, Chiptune chiptune) {

        // Save additions YM registers ?
        int nbRegs = getBooleanOption(PARAM_SAVE_ATARI_SFX_REGS) ? Tools.YM_REGISTERS : Tools.CPC_REGISTERS;

        // The Following array contains 14 or 16 ArrayList of Bytes (one for each CPC PSG register)
        ArrayList<Byte> arrRegs[] = new ArrayList[nbRegs];
        for (int curr_register = 0; curr_register < arrRegs.length; curr_register++)
            arrRegs[curr_register] = /*(blnConvertFrequency==true)?
					(ArrayList)chiptune.getConvertedArrayListRegister(curr_register, YMC_Tools.YM_CPC_FREQUENCY):*/
                    (ArrayList) chiptune.getArrayListRegister(curr_register);

        // need to swap channels ?
        String channelOrder = (String) getListOptionSelected(PARAM_CHANNELS_ORDER);
        if ((CHANNEL_B + CHANNEL_A + CHANNEL_C).equals(channelOrder)) {
            ArrayList<Byte> aux = arrRegs[0];
            arrRegs[0] = arrRegs[2];
            arrRegs[2] = aux;

            aux = arrRegs[1];
            arrRegs[1] = arrRegs[3];
            arrRegs[3] = aux;

            aux = arrRegs[8];
            arrRegs[8] = arrRegs[9];
            arrRegs[9] = aux;

            for (int i = 0; i < arrRegs[7].size(); i++) {
                Byte bFilter = arrRegs[7].get(i);
                byte bA = (byte) (bFilter.byteValue() & 0x9);
                byte bB = (byte) ((bFilter.byteValue() >> 1) & 0x9);
                byte bC = (byte) ((bFilter.byteValue() >> 2) & 0x9);

                arrRegs[7].set(i, Byte.valueOf((byte) (bB | (bA << 1) | (bC << 2))));

            }

            for (int f = 0; f < chiptune.getLength(); f++) {
                // Get Frame
                Frame frame = chiptune.getFrame(f);
                SampleInstance sA = frame.getSI(0);
                SampleInstance sB = frame.getSI(1);

                frame.setSI(0, sB);
                frame.setSI(1, sA);
            }
        }

        // Samples replay
        // a non null value means desactivated
        //long oscillator[] = {0,0,0};
        //byte sample_number[] = {0,0,0};

        // Debug values
        double minRate[] = {Tools.YM_CPC_FREQUENCY, Tools.YM_CPC_FREQUENCY, Tools.YM_CPC_FREQUENCY};
        double maxRate[] = {0, 0, 0};

        // IceAge Hack
		/*YMC_Model model = new YMC_Model();
		Chiptune iceage = model.getData("d:\\Iceage (digi).ym");
		chiptune.setLength(iceage.getLength()); // truncate
		*/

        // TODO get number of different sid frequencies played (get, put)
        int sidcount = 0;
        HashMap<Integer, Integer> mapSidSemiPeriod = new HashMap<Integer, Integer>();

        // Debug seagulls
		/*double st_pperiod=0;
		int st_volume=0;
		int st_tp=0;
		int st_tc=0;
		int st_Vmax=0;*/

        // Preprocess chiptune to include SpecialFX info
        // TODO be sure that the chiptune isn't stretched
        for (int f = 0; f < chiptune.getLength(); f++) {
            // Get Frame
            Frame frame = chiptune.getFrame(f);
            //Frame frame2 = iceage.getFrame(f);

            // Log voice C (debug seagulls)
			/*SampleInstance sid = frame.getSI(2);
			double pperiod = frame.getPPeriod(2);
			int volume = frame.getBytVol(2);
			int tp=(sid==null)?0:sid.getTp();
			int tc=(sid==null)?0:sid.getTc();
			int vmax = (sid==null)?0:sid.getSample();
			
			// Filter Sid
			if ((pperiod==0d) && (volume==0) && (tp == 0) && (tc ==0) && (vmax == 0) && (st_Vmax!=0)){
				frame.setPPeriod(2, st_pperiod);
				frame.setBytVol(2, (byte)st_volume);
				int on = 0;
				for(int i=0;i<3;i++){
					SampleInstance s = frame.getSI(i);
					if ((s!=null) && (s.getOrigFxNb() == 0))
						on = 1;
				}
				frame.setSI(2, new SampleInstance(SpecialFXType.ATARI_SIDVOICE, st_Vmax, st_tp, st_tc, on));
				//frame.setBytReg7((byte)(frame.getBytReg7() & 0x3B));
				
				sid = frame.getSI(2);
				pperiod = frame.getPPeriod(2);
				volume = frame.getBytVol(2);
				tp=(sid==null)?0:sid.getTp();
				tc=(sid==null)?0:sid.getTc();
				vmax = (sid==null)?0:sid.getSample();
			}
			else{
				// save previous data
				st_pperiod=pperiod;
				st_volume=volume;
				st_tp=tp;
				st_tc=tc;
				st_Vmax=vmax;
			}*/

            // Filter High Low frequencies
			/*if ((pperiod>1800) || (pperiod<5)){
				//frame.setSI(2, null);
				SampleInstance s=frame.getSI(2);
				if (s!=null)s.setSample(0);
				frame.setBytReg7((byte)(frame.getBytReg7() | 0x04));
			}*/
				
			/*int[] mfpPrediv = {0,4,10,16,50,64,100,200};
			YMC_Tools.info(	"Frame " + Integer.toHexString(f) +
							" PPeriod=" + pperiod +
							" Volume=" + volume + 
							" Mixer=" + Integer.toHexString(frame.getBytReg7()) +
							" SidFreq=" + ((tp>0)?2457600L / (tc * mfpPrediv[tp]):0) +
							" Vmax=" + vmax +
							" Notes=" + (((pperiod>1800)||(pperiod<5))?"WARNING":""));*/

            // Detect specialFX on each channels
            for (int voice = 0; voice < 3; voice++) {
                // Hack iceage
				/*if (voice==0){
					SampleInstance si2 = frame2.getSI(voice);
					if ((si2 != null) && ((si2.isAtari()))){
						arrRegs[5].set(f, (byte)(arrRegs[5].get(f) | ((voice+1)<<6) | 0x10));	//Digidrum
						arrRegs[8+voice].set(f, (byte)(si2.getSample()));
					}
					continue;
				}*/

                SampleInstance si = frame.getSI(voice);
                if ((si != null) && ((si.isAtari()))) {
                    /**
                     * We care only about True Digidrums (Samples that don't change rate)
                     */
					/*switch(si.getType())
					{					
						case ATARI_DIGIDRUM:
							byte nbSample = (byte)(si.getSample() & 0xF);
							Sample sample = chiptune.getArrSamples()[nbSample];
							
							if (sample.isDigiDrum())
							{
								// Set Sample number instead of volume (Only 16 digidrums availables)
								arrRegs[8+voice].set(f, (byte)(nbSample | AYC_DIGIDRUM));
								sample_number[voice] = nbSample;
								
								// Desactivate oscillator for voice till the end of sample
								// (Rounded to the closest higher integer)
								oscillator[voice] = (long)Math.ceil((double)YMC_Tools.CPC_REPLAY_FREQUENCY*sample.getLength()/sample.getRate()) - 1;
								arrRegs[7].set(f, (byte)(arrRegs[7].get(f) | 0x9<<voice));
							}
							break;
						default:
							// Unknown Atari FX
							oscillator[voice] = 0;
					}*/

                    // Save Atari additional Registers ?
                    if (getBooleanOption(PARAM_SAVE_ATARI_SFX_REGS)) {
                        //byte tp = (byte)si.getTp();
                        //byte tc = (byte)si.getTc();

                        // get Type code
                        byte fxType = 0x10;                //Digidrum
                        switch (si.getType()) {
                            case ATARI_SIDVOICE:        // SID
                                fxType = 0x00;
                                break;
                            case ATARI_SINSID:            // Sinus-SID
                                fxType = (byte) 0x20;
                                break;
                            case ATARI_SYNCBUZZER:        // Sync-Buzzer.
                                fxType = (byte) 0x30;
                                break;
                        }

                        // TODO get number of different sid frequencies played (get, put)
                        double dbRate = si.getRate();
                        double semiPeriode = 312d * 50d / dbRate;
                        int sp256 = (int) Math.round((semiPeriode * 256)) & 0xFFFF;
                        if ((semiPeriode < 2) || (semiPeriode >= 256))
                            System.out.println("WARNING !!!! Sid rate should be between [61, 7800]hz current is " + dbRate + "hz");
                        if (!mapSidSemiPeriod.containsKey(sp256)) mapSidSemiPeriod.put(sp256, sidcount++);
                        int numSid = mapSidSemiPeriod.get(sp256);

                        // Debug rates
                        if (minRate[voice] > si.getRate()) minRate[voice] = si.getRate();
                        if (maxRate[voice] < si.getRate()) maxRate[voice] = si.getRate();

                        // We use those registers to store the effects :
                        // 1, 8+voice, 14
                        // 3, 8+voice, 15
                        if (si.getOrigFxNb() == 1) {
                            arrRegs[3].set(f, (byte) (arrRegs[3].get(f) | ((voice + 1) << 6) | fxType));        // Voice (2b), Fx Type (2b), Voice Frequency 4 MSB
                            arrRegs[8 + voice].set(f, (byte) (si.getSample() | ((numSid >> 3) & 0xE0)));        // yyy of num Sid (3 bits) + Sample number or Vmax or Env (5 bits)
                            arrRegs[15].set(f, (byte) (numSid & 0xFF));                                    // yyyyyyyy of rate step (8bits)
                            //YMC_Tools.info(f + " " + si.getOrigFxNb() + " " + Integer.toHexString(arrRegs[3].get(f)).toUpperCase() + " " + Integer.toHexString(arrRegs[8+voice].get(f)).toUpperCase() + " " + Integer.toHexString(arrRegs[15].get(f)).toUpperCase() );
                        } else {
                            arrRegs[1].set(f, (byte) (arrRegs[1].get(f) | ((voice + 1) << 6) | fxType));        // Voice (2b), Fx Type (2b), Voice Frequency 4 MSB
                            arrRegs[8 + voice].set(f, (byte) (si.getSample() | ((numSid >> 3) & 0xE0)));        // yyy of num Sid (3 bits) + Sample number or Vmax or Env (5 bits)
                            arrRegs[14].set(f, (byte) (numSid & 0xFF));                                    // yyyyyyyy of rate step (8bits)
                            //YMC_Tools.info(f + " " + si.getOrigFxNb() + " " + Integer.toHexString(arrRegs[1].get(f)).toUpperCase() + " " + Integer.toHexString(arrRegs[8+voice].get(f)).toUpperCase() + " " + Integer.toHexString(arrRegs[14].get(f)).toUpperCase() );
                        }
                        continue;

//						double dbStep = 2048*si.getRate()/(312*50);
//						long incStep = Math.round(dbStep); 
//						double calcRate = (312*50)*incStep>>11;
//						//YMC_Tools.debug("Rate = " + si.getRate() + " =? " + calcRate + " error = " + Math.abs(100 - calcRate*100/si.getRate()) + "%" );
//						
//						/**Rather than saving tc and tp, it's easier for the replayer to save the rate as a step
//						 * Standard Asic rate is 312*50 ~ 15.6khz
//						 * The step is saved as a 11 bits value xyy yyyyyyyy
//						 * x is the integer step value (0 to 1)
//						 * yyyyyyyyyy is the decimal value (0 to 1023)
//						 * Lower rate (0 is illegal): 312*50/1024 ~ 15hz
//						 * Higher rate : 312*50*(2 - 1/1024) = 312*50*2047/1024 ~ 31185hz
//						 */
//						
//						
//						// We use those registers to store the effects :
//						// 1, 8+voice, 14
//						// 3, 8+voice, 15
//						if (si.getOrigFxNb() == 1){
//							arrRegs[3].set(f, (byte)(arrRegs[3].get(f) | ((voice+1)<<6) | fxType));			// Voice (2b), Fx Type (2b), Voice Frequency 4 MSB  
//							arrRegs[8+voice].set(f, (byte)(si.getSample() | ((incStep>>3) & 0xE0)));	// xyy of rate step (3 bits) + Sample number or Vmax or Env (5 bits)
//							arrRegs[15].set(f, (byte)(incStep & 0xFF));									// yyyyyyyy of rate step (8bits)
//							YMC_Tools.info(f + " " + si.getOrigFxNb() + " " + Integer.toHexString(arrRegs[3].get(f)).toUpperCase() + " " + Integer.toHexString(arrRegs[8+voice].get(f)).toUpperCase() + " " + Integer.toHexString(arrRegs[15].get(f)).toUpperCase() );
//						}
//						else{
//							arrRegs[1].set(f, (byte)(arrRegs[1].get(f) | ((voice+1)<<6) | fxType));			// Voice (2b), Fx Type (2b), Voice Frequency 4 MSB  			
//							arrRegs[8+voice].set(f, (byte)(si.getSample() | ((incStep>>3) & 0xE0)));	// xyy of rate step (3 bits) + Sample number or Vmax or Env (5 bits)
//							arrRegs[14].set(f, (byte)(incStep & 0xFF));									// yyyyyyyy of rate step (8bits)
//							YMC_Tools.info(f + " " + si.getOrigFxNb() + " " + Integer.toHexString(arrRegs[1].get(f)).toUpperCase() + " " + Integer.toHexString(arrRegs[8+voice].get(f)).toUpperCase() + " " + Integer.toHexString(arrRegs[14].get(f)).toUpperCase() );
//						}												
                    }
                }
				/*else if (oscillator[voice]>0)
				{
					// During Digidrum/Sample playing :
					// - Desactivate oscillator on voice
					// - Keep same value in the volume register (Sample number) 
					arrRegs[7].set(f, (byte)(arrRegs[7].get(f) | 0x9<<voice));
					arrRegs[8+voice].set(f, sample_number[voice]);
					oscillator[voice]--;
				}*/
            }
        }

        // Save Sids in different file
        try {
            FileOutputStream fileSid = new FileOutputStream(strDestFile + " - SIDs.bin");
            byte arrSidFile[] = new byte[sidcount * 2];
            for (Iterator it = mapSidSemiPeriod.entrySet().iterator(); it.hasNext(); ) {
                Entry entry = (Entry) it.next();
                int numSid = (Integer) entry.getValue();
                int sp256 = (Integer) entry.getKey();
                arrSidFile[2 * numSid] = (byte) (sp256 & 0xFF);
                arrSidFile[2 * numSid + 1] = (byte) ((sp256 >> 8) & 0xFF);
            }
            fileSid.write(arrSidFile);
            fileSid.close();
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Debug effects rate
        for (int voice = 0; voice < 3; voice++) {
            Tools.info("Channel " + voice + " FXs Min Rate : " + minRate[voice]);
            Tools.info("Channel " + voice + " FXs Max Rate : " + maxRate[voice]);
        }

        // Process each Register
        ArrayList<ArrayList<Byte>> arrCrunchedRegister = new ArrayList<ArrayList<Byte>>();
        ArrayList<Byte> arrBuffLen = new ArrayList<Byte>();

        int intRawSize = chiptune.getLength() * nbRegs;
        int sum = 0;
        for (int curr_register = 0; curr_register < arrRegs.length; curr_register++) {
            Tools.debug("+ Process Register " + curr_register);

            byte bytBuffLen = 0x01;
            ArrayList arrCrunchedData = null;

            // Test if the buffer size is forced
            //String strBuffSize = (String) getListOptionSelected(PARAM_BUFFSIZE_REG + String.format("%2d", curr_register));
            String strBuffSize = (String) getListOptionSelected(PARAM_BUFFSIZE_REG + ((curr_register < 10) ? "0" : "") + curr_register);
            if (BUFF_SIZE_0x100.equals(strBuffSize)) {
                arrCrunchedData = doAYCCrunch(arrRegs[curr_register], 0x100);
                //arrBuffLen.add((byte) 0x01);
            } else if (BUFF_SIZE_0x400.equals(strBuffSize)) {
                arrCrunchedData = doAYCCrunch(arrRegs[curr_register], 0x400);
                //arrBuffLen.add((byte) 0x04);
                bytBuffLen = 0x4;
            } else {
                arrCrunchedData = doAYCCrunch(arrRegs[curr_register], 0x100);
                ArrayList arrCrunchedData400 = doAYCCrunch(arrRegs[curr_register], 0x400);

                //byte bytBuffLen = 0x1;
                if ((arrCrunchedData400.size() + 0x300) < arrCrunchedData.size()) {
                    // Use a 0x400 bytes buffer rather than a 0x100 one
                    arrCrunchedData = arrCrunchedData400;
                    bytBuffLen = 0x4;
                }
            }

            // set Buffer Length
            Byte BuffLen = new Byte(bytBuffLen);
            arrBuffLen.add(BuffLen);

            // Add Crunched register to the list
            arrCrunchedRegister.add(arrCrunchedData);
            sum += arrCrunchedData.size();

            // Should we save the register's values in its own file ?
            if (getBooleanOption(PARAM_SAVE_REGS_IN_SEPARATE_FILES)) {
                FileOutputStream file_reg;
                try {
                    file_reg = new FileOutputStream(strDestFile + " - Register " + curr_register + " (buffer size = 0x" + Integer.toHexString(BuffLen).toUpperCase() + "00).bin");
                    for (int i = 0; i < arrCrunchedData.size(); i++) {
                        byte bytVal = ((Byte) arrCrunchedData.get(i)).byteValue();
                        file_reg.write(bytVal);
                    }
                    file_reg.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Notify ratio Completion
            setCompletionRatio(curr_register * 100 / (nbRegs - 1));
        }

        // Printing various Info
        Tools.debug("+ Total Raw Size = " + intRawSize + " bytes");
        Tools.debug("+ Total Compressed Size = " + sum + " bytes");
        Tools.info("+ Compression ratio ~ " + (float) sum * 100 / intRawSize + " %");


        /**
         * Convert the Crunched data to a list of Bytes ready to be stored
         */

        ArrayList<Byte> arrYmBytes = new ArrayList<Byte>();

        // Time
        addLEvar(arrYmBytes, chiptune.getLength(), 2);

        /**
         *  Calculate extended header length
         */
		/*int intExtHeaderLength =	1 + 						// AYC version
									1 +							// Play Frequency
																// 	5=50hz;
																// 	4=60hz;
																// 	3=75hz;
																// 	2=100hz;
																// 	1=150hz;
																// 	0=300hz;)
									chiptune.getStrSongName().length() + 1 +	// SongName
									chiptune.getStrAuthorName().length() + 1 +	// Author Name
									AYC_COMMENTS.length() + 1 +					// Comments
									1;											// Nb Digidrums (0-15)*/


        // get the size needed in the header for each digidrum (original size + compressed size)
		/*for (int i=0;i<intNbDigiDrums;i++) 
		{
			intExtHeaderLength += 2;							// original size
			intExtHeaderLength += arrSamples15khz[i].length;	// sample
			//intExtHeaderLength += (digidrums[i].getLength()+1)/2;
		}*/

        // Buffer size needed for decrunching
        int buffSize = 0;

        // Registers' offset
        int offsetBase = 2 + 2;
        int offsetPhysical = 2 + 16 * 3 /* + intExtHeaderLength*/;
        if (getBooleanOption(PARAM_EXTENDED_HEADER)) offsetPhysical += 2;
        int offset = offsetPhysical - offsetBase;
        addLEvar(arrYmBytes, (Byte) (arrBuffLen.get(0)), 1);
        addLEvar(arrYmBytes, offset, 2);
        buffSize = arrBuffLen.get(0);
        //debug("! Offset Register 0 = " + Long.toHexString(offsetPhysical) + "(" + Long.toHexString(offset) + ")");
        for (int i = 1; i < arrCrunchedRegister.size(); i++) {
            // get Register size
            ArrayList arrData = (ArrayList) (arrCrunchedRegister.get(i - 1));

            // get Relative Offset
            offsetPhysical += arrData.size();
            offsetBase += 3;
            offset = offsetPhysical - offsetBase;

            addLEvar(arrYmBytes, (Byte) (arrBuffLen.get(i)), 1);
            addLEvar(arrYmBytes, offset, 2);
            buffSize += arrBuffLen.get(i);
            //debug("! Offset Register " + i + " = " + Long.toHexString(offsetPhysical) + "(" + Long.toHexString(offset) + ")");
        }

        // Add Dummy values for 14,15 Registers' Offset
        if (nbRegs == Tools.CPC_REGISTERS)
            for (byte b = 0; b < 6; b++) arrYmBytes.add(new Byte((byte) 0xFF));

        // Log buffer size needed for the AYC to be decrunched
        Tools.info("+ Buffer size needed during decrunching : 0x" + Integer.toHexString(buffSize << 8).toUpperCase());

        // We just reserve the place for the Extended header
        if (getBooleanOption(PARAM_EXTENDED_HEADER)) addLEvar(arrYmBytes, 0, 2);

        // Data
        for (int i = 0; i < arrCrunchedRegister.size(); i++) {
            ArrayList arrData = (ArrayList) (arrCrunchedRegister.get(i));
            arrYmBytes.addAll(arrData);
        }

        /**
         * Here is the additional header
         *
         * */
        if (getBooleanOption(PARAM_EXTENDED_HEADER)) {
            // Set the relative pointer to the extended header
            setLEvar(arrYmBytes, arrYmBytes.size() - (AYC_EXT_HEADER_PTR + 1), 2, AYC_EXT_HEADER_PTR);

            // AYC Extended version
            addLEvar(arrYmBytes, AYC_VERSION, 1);

            // Play frequency
            byte intPRate; // = AYC_DFLT_INT_PLAYRATE;
            switch (chiptune.getPlayRate()) {
                case 300:
                    intPRate = 0;
                    break;
                case 150:
                    intPRate = 1;
                    break;
                case 100:
                    intPRate = 2;
                    break;
                case 75:
                    intPRate = 3;
                    break;
                case 60:
                    intPRate = 4;
                    break;
                case 50:
                default:
                    intPRate = AYC_DFLT_INT_PLAYRATE;
            }
            addLEvar(arrYmBytes, intPRate, 1);

            // Number of digidrums (MAX 16)
            int intNbDigidrums = chiptune.getNbSamples() & 0xF;
            addLEvar(arrYmBytes, intNbDigidrums, 1);

            // Digidrums
            // We store them on 4 bits so that we can mix valN and valN+1 on the same byte
            for (int i = 0; i < intNbDigidrums; i++) {
                // Get Sample in 15khz 4bits
                byte sample15khz[] = getSampleAt15khz(chiptune.getArrSamples()[i]);

                // TODO This is a pure test
				/*ArrayList<Byte> vect15khz = new ArrayList<Byte>();
				for (int k=0;k<sample15khz.length;k++) vect15khz.add(new Byte(sample15khz[k])); 
				ArrayList arrCrunchedDrum100 = doAYCCrunch(vect15khz, 0x100);
				ArrayList arrCrunchedDrum400 = doAYCCrunch(vect15khz, 0x400);*/

                // Digidrum length (word)
                addLEvar(arrYmBytes, sample15khz.length, 2);

                // Sample data
                for (int j = 0; j < sample15khz.length; j++) {
                    int count = 0;
                    int bDD = 0;
                    int bDDMerged = 0;

                    while (++count < 2) {
                        if (j < sample15khz.length) bDD = sample15khz[j];

                        bDDMerged <<= 4;
                        bDDMerged |= bDD;
                        j++;
                    }

                    addBEvar(arrYmBytes, bDDMerged, 1);
                }
            }

            // Song Name + Author Name + Comments (Let's say ID3)
            addStringToArray(arrYmBytes, chiptune.getStrSongName(), true);
            addStringToArray(arrYmBytes, chiptune.getStrAuthorName(), true);
            addStringToArray(arrYmBytes, AYC_COMMENTS, true);
        }

        // Save Digidrums in AYL format ?
        if (getBooleanOption(PARAM_SAVE_DIGIDRUMS_AYL)) {
            // Get default channel for digidrums
            byte dfltChannel = (byte) 0xA;    // Channel C
            switch (getListOptionIndex(PARAM_AYL_DEFAULT_CHANNEL)) {
                case 0:
                    dfltChannel = (byte) 0x8;
                    break;
                case 1:
                    dfltChannel = (byte) 0x9;
                    break;
            }

            // Number of digidrums (MAX 32)
            int intNbDigidrums = chiptune.getNbSamples() & 0x1F;

            for (int i = 0; i < intNbDigidrums; i++) {
                // Get Sample in 15khz 4bits
                byte sample15khz[] = getSampleAt15khz(chiptune.getArrSamples()[i]);

                try {
                    FileOutputStream file_output_crunched = new FileOutputStream(strDestFile + " - Digidrum" + i + "(crunched).bin");
                    FileOutputStream file_output_vanilla = new FileOutputStream(strDestFile + " - Digidrum" + i + ".bin");

                    int intDrumLength = 0;

                    // First sample value
                    if (sample15khz.length > 0) {
                        short shtPause = 0;
                        byte bytPrevValue = (byte) sample15khz[0];
                        file_output_crunched.write(bytPrevValue);                                        // sample value
                        file_output_crunched.write(AYL_SET_REGISTER_COMMAND | dfltChannel);    // Set Default Digidrum Register
                        file_output_vanilla.write(bytPrevValue);                                        // sample value
                        file_output_vanilla.write(AYL_SET_REGISTER_COMMAND | dfltChannel);    // Set Default Digidrum Register
                        intDrumLength += 2;

                        for (int k = 1; k < sample15khz.length; k++) {
                            byte bytCurrValue = (byte) sample15khz[k];

                            file_output_vanilla.write(bytCurrValue);                                        // sample value
                            file_output_vanilla.write(AYL_SET_REGISTER_COMMAND | dfltChannel);    // Set Default Digidrum Register

                            if (bytCurrValue == bytPrevValue) shtPause++;
                            else {
                                // Set Pause
                                while (shtPause > 0) {
                                    byte bytPauseLow = 0;
                                    byte bytPauseHigh = 0;

                                    if (shtPause > 4096) {
                                        bytPauseLow = (byte) (0xFF);    // Set Pause to 4095 * HBL
                                        bytPauseHigh = (byte) (0xF);
                                        Tools.info("!!! Pause > 4096");
                                    } else {
                                        bytPauseLow = (byte) ((shtPause) & 0xFF);
                                        bytPauseHigh = (byte) (((shtPause) >> 8) & 0xF);
                                    }

                                    file_output_crunched.write(bytPauseLow);                        // Pause value
                                    file_output_crunched.write(bytPauseHigh | AYL_PAUSE_COMMAND);    // Pause Commande
                                    intDrumLength += 2;
                                    shtPause -= 4096;
                                }

                                file_output_crunched.write(bytCurrValue);                                        // sample value
                                file_output_crunched.write(AYL_SET_REGISTER_COMMAND | dfltChannel);    // Set Default Digidrum Register
                                intDrumLength += 2;
                                bytPrevValue = bytCurrValue;
                                shtPause = 0;
                            }
                        }

                        // May happen when there are pauses at the end of the sample
                        // Set Pause
                        while (shtPause > 0) {
                            byte bytPauseLow = 0;
                            byte bytPauseHigh = 0;

                            if (shtPause > 4096) {
                                bytPauseLow = (byte) (0xFF);
                                bytPauseHigh = (byte) (0xF);
                            } else {
                                bytPauseLow = (byte) ((shtPause) & 0xFF);
                                bytPauseHigh = (byte) (((shtPause) >> 8) & 0xF);
                            }

                            file_output_crunched.write(bytPauseLow);                        // Pause value
                            file_output_crunched.write(bytPauseHigh | AYL_PAUSE_COMMAND);    // Pause Commande
                            intDrumLength += 2;
                            shtPause -= 4096;
                        }
                    }

                    // Stop DMA List
                    file_output_crunched.write(AYL_STOP_COMMAND & 0xFF);        // Stop
                    file_output_crunched.write((AYL_STOP_COMMAND >> 8) & 0xFF);    // Command
                    intDrumLength += 2;

                    file_output_crunched.close();

                    file_output_vanilla.write(AYL_STOP_COMMAND & 0xFF);        // Stop
                    file_output_vanilla.write((AYL_STOP_COMMAND >> 8) & 0xFF);    // Command

                    file_output_vanilla.close();

                    // Log drum length
                    Tools.info("+ Digidrum " + i + " size = 0x" + Integer.toHexString(intDrumLength).toUpperCase());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return arrYmBytes;
    }

    /**
     * Should return the Plugin extension (i.e. ".ayc")
     *
     * @return String fileextension
     */
    public String getExtension() {
        return AYC;
    }

    /**
     * Return a 15khz sample suitable for CPC+ replay
     * TODO Wrong for Mod Samples !!!
     *
     * @param sample
     * @return
     */
    private byte[] getSampleAt15khz(Sample sample) {
        if (!sample.isDigiDrum()) return sample.getWave();

        // Calculate Length of Sample in 15khz mode
        double dbStep = (double) (312 * 50) / sample.getRate();
        int cpcSampleLength = (int) (Math.round((double) sample.getLength() * dbStep));
        byte arrByte[] = new byte[cpcSampleLength];

        int newIndex = 0;

        // index in sample
        int k = 0;
        for (int j = 0; j < cpcSampleLength; j++) {
            // index in sample
            k = (int) (Math.round((double) j / dbStep));
            //System.out.println(sample.getLength() + " - " + sample.getWave().length + " - " + k);

            // Convert 8bits samples to 4 bits samples
            int sval = sample.getWave()[(k < sample.getLength()) ? k : (sample.getLength() - 1)];
            byte dval = 0;
            if (sample.getResolution() == 8) {
                // Convert signed sample to unsigned
                if (sample.isSigned()) sval += 128;

                sval &= 0xFF;
                dval = arr8to4bits[sval];
            } else {
                // Convert signed sample to unsigned
                if (sample.isSigned()) sval += 8;

                dval = (byte) (sval & 0xF);

                // Convert 4bits samples to 8 bits samples
                // bDD = arr4to8bits[bDD];

            }

            arrByte[newIndex++] = (byte) dval;    // sample value on C
        }

        return arrByte;
    }
}
