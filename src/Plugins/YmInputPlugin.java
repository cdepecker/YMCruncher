package Plugins;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import YMCruncher.Chiptune;
import YMCruncher.Frame;
import YMCruncher.InputPlugin;
import YMCruncher.YMC_Tools;

/**
 * This class handle the YM chiptune format input to the core cruncher.
 * YM is a format defined originally for atariST chiptunes but has been extended to handle 
 * speccy's and cpc's chiptunes as well.
 * @author F-key/RevivaL
 */
public class YmInputPlugin extends InputPlugin {
	
	// ATARI-ST MFP chip predivisor & Timer settings
	final private static int[] mfpPrediv = {0,4,10,16,50,64,100,200};
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
	private Long intFrequency = new Long(YMC_Tools.YM_ATARI_FREQUENCY);
	private Long intLoopVBL = new Long(0);
	private boolean blnLoop = false;
	private int intPlayRate = YMC_Tools.CPC_REPLAY_FREQUENCY;

	// Samples
	private Sample[] arrDigiDrums = null;	
	
	// redirect Samples (trim unused samples)
	private int total_sample = 0;
	private int sampleRedirect[] = null;
	private boolean blSampleUsed[] = null;
	
	// SpecialFX log
	private boolean blnSidVoice = false;
	private boolean blnSinSid = false;
	private boolean blnSyncBuzzer = false;

	
	@Override
	protected Chiptune getPreProcessedChiptune(Vector arrRawChiptune, String strExt)
	{
		// Wrapper for below function
		Vector<Frame> arrFrames = getPSGRegistersValues(arrRawChiptune, strExt);
		
		Sample [] list_samples = getSamples();
		
		// Special FX info
		boolean blnDigiDrums = (list_samples!=null) && (list_samples.length>0);
		if (blnSidVoice || blnSinSid || blnSyncBuzzer || blnDigiDrums)
			YMC_Tools.info("+ SpecialFX");
		if (blnSidVoice) YMC_Tools.info("  - Sid Voices");
		if (blnSinSid) YMC_Tools.info("  - Sinus Sid");
		if (blnSyncBuzzer) YMC_Tools.info("  - Sync Buzzer");
		if (blnDigiDrums) YMC_Tools.info("  - Digidrums");
		
		
		// return if errors
		if (arrFrames == null) return null;
			
		return new Chiptune(strSongName,
							strAuthorName,
							arrFrames,
							intPlayRate,
							intFrequency.longValue(),
							blnLoop,
							intLoopVBL,
							list_samples);
	}

	
	/**
	 * This method should basically return an array of PSG registers' values
	 * Let's call Rx the Register number x, then this function should return
	 * an array of all the R0 values for all frames then all the R1 values for all frames 
	 * and so on ...
	 * @param arrRawChiptune Vector representing the uncompressed inputed chiptune
	 * @return Vector that should contain the PSG registers' values.
	 */
	protected Vector<Frame> getPSGRegistersValues(Vector arrRawChiptune, String strExt)
	{
		int intNbRegisters = YMC_Tools.CPC_REGISTERS;
		Vector <Byte>arrRegistersValues = null;
		
		// Get type of file
		strFileType = YMC_Tools.getString(arrRawChiptune, 0, 4);
		if (strFileType.equals("YM2!"))
			{
				// Load YM2 digidrums
				FileInputStream file_input;
				try {
					file_input = new FileInputStream (YM2_DIGIDRUM_FILE);
					
					int sample = 0;
					
					while (sample<40)
					{
						file_input.read(sampleAdress[sample]);
						sample++;
					}
					
					// init redirect array
					sampleRedirect = new int[40]; 
					blSampleUsed = new boolean[40];
					for (int i=0;i<sampleRedirect.length;i++) blSampleUsed[i] = false;
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			
				// Set Frequency of song to 2000000 (atari)
				intFrequency = new Long(YMC_Tools.YM_ATARI_FREQUENCY);
				
				// get rid of the header string
				arrRegistersValues = new Vector<Byte>(arrRawChiptune.subList(4, arrRawChiptune.size()));
			}
			else if (strFileType.equals("YM3!"))
			{
				// Set Frequency of song to 2000000 (atari)
				intFrequency = new Long(YMC_Tools.YM_ATARI_FREQUENCY);
				
				// get rid of the header string
				arrRegistersValues = new Vector<Byte>(arrRawChiptune.subList(4, arrRawChiptune.size()));				
			}
			else if (strFileType.equals("YM3b"))
			{
				// Set Frequency of song to 2000000 (atari)
				intFrequency = new Long(YMC_Tools.YM_ATARI_FREQUENCY);
				
				// get rid of the header string and the VBL loop
				arrRegistersValues = new Vector<Byte>(arrRawChiptune.subList(4, arrRawChiptune.size()-4));
			}
			else if (strFileType.equals("YM5!")
					|| strFileType.equals("YM6!"))
			{
				// Set number of registers
				intNbRegisters = 16;			
				
				Long intVBL = YMC_Tools.getBEInt(arrRawChiptune, 12);
				Long intAttributes = YMC_Tools.getBEInt(arrRawChiptune, 16);
				Integer shtDigiDrum = YMC_Tools.getBEShort(arrRawChiptune, 20);
				intFrequency = YMC_Tools.getBEInt(arrRawChiptune, 22);
				Integer shtPlayerFrequency = YMC_Tools.getBEShort(arrRawChiptune, 26);
				intLoopVBL = YMC_Tools.getBEInt(arrRawChiptune, 28);
				YMC_Tools.info("+ VBL = 0x" + Long.toHexString(intVBL.longValue()).toUpperCase() + " (~" + intVBL.longValue()/50 + " seconds)");
				YMC_Tools.info("+ Number of DigiDrum = " + shtDigiDrum);
				YMC_Tools.info("+ Frequency = " + intFrequency + " Hz");
				YMC_Tools.info("+ Player Frequency = " + shtPlayerFrequency + " Hz");
				YMC_Tools.info("+ Loop VBL = " + Long.toHexString(intLoopVBL.longValue()).toUpperCase());
				
				/**
				 * Attributes
				 * */
				YMC_Tools.info("+ Attributes = 0x" + Long.toHexString(intAttributes.longValue()).toUpperCase());
				boolean isInterleaved = false;
				boolean areSampleSigned = false;
				boolean areSample4Bits = false;
				
				// Interleaved ?
				if (((intAttributes.longValue() & A_STREAMINTERLEAVED) == 0)) YMC_Tools.info("  - YM file is uninterleaved");
				else
				{
					YMC_Tools.info("  - YM file is interleaved");
					isInterleaved = true;
				}
				
				// DIGIDRUM signed ?
				if (((intAttributes.longValue() & A_DRUMSIGNED) == 0)) YMC_Tools.info("  - Digidrums are unsigned");
				else
				{
					YMC_Tools.info("  - Digidrums are signed");
					areSampleSigned = true;
				}
				
				// 4bit DIGIDRUM
				if (((intAttributes.longValue() & A_DRUM4BITS) == 0)) YMC_Tools.info("  - Digidrums resolution is 8bits");
				else
				{
					YMC_Tools.info("  - Digidrums resolution is 4bits");
					areSample4Bits = true;
				}
				
				// Loop mode ?
				if (((intAttributes.longValue() & A_LOOPMODE) == 0)) YMC_Tools.info("  - Loop mode OFF");
				else
				{
					YMC_Tools.info("  - Loop mode ON");
					blnLoop = true;
				}
				
				// Time Control ?
				if (((intAttributes.longValue() & A_TIMECONTROL) == 0)) YMC_Tools.info("  - Time Control OFF");
				else YMC_Tools.info("  - Time Control ON");										
				
				// Playing rate adjustment (60hz <-> 50hz)
				intPlayRate = shtPlayerFrequency;
				
				// get digidrums
				int intOffset = 34;
				arrDigiDrums = new Sample[shtDigiDrum.intValue()];
				for (int i=0; i<shtDigiDrum.intValue(); i++)
				{
					Long intSizeDD = YMC_Tools.getBEInt(arrRawChiptune, intOffset);
					intOffset += 4;
					
					byte[] arrDigi = new byte[intSizeDD.intValue()];
					for (int j=0; j< intSizeDD.intValue(); j++)
					{
						byte bDD = ((Byte)arrRawChiptune.elementAt(intOffset++)).byteValue();
						arrDigi[j] = bDD;
					}				
					arrDigiDrums[i] = new Sample(	String.valueOf(i),
													intSizeDD.intValue(),
													(byte)((areSample4Bits == true)?4:8),
													areSampleSigned,
													(byte)0,
													(byte)0xF,
													0,
													0,
													arrDigi);
					
					// debug
					YMC_Tools.debug("- DigiDrums (" + i + ") size = " + intSizeDD);
				}
				
				// init redirect array
				sampleRedirect = new int[shtDigiDrum.intValue()]; 
				blSampleUsed = new boolean[shtDigiDrum.intValue()];
				for (int i=0;i<sampleRedirect.length;i++) blSampleUsed[i] = false;
				
				// Other Info
				strSongName = YMC_Tools.getNTString(arrRawChiptune, intOffset, false);
				strAuthorName = YMC_Tools.getNTString(arrRawChiptune, intOffset += strSongName.length()+1, false);
				String strComments = YMC_Tools.getNTString(arrRawChiptune, intOffset += strAuthorName.length()+1, false);
				YMC_Tools.info("+ Song Name = " + strSongName);
				YMC_Tools.info("+ Author = " + strAuthorName);
				YMC_Tools.info("+ Comments = " + strComments);
				
				// Offset points to data 
				intOffset += strComments.length()+1;
			
				// return sub-array (minus 4 bytes for 'End!')
				int intTailer = 0;
				if (YMC_Tools.getString(arrRawChiptune, arrRawChiptune.size()-4, 4).equals("End!")) intTailer = 4;
				arrRegistersValues = new Vector<Byte>(arrRawChiptune.subList(intOffset, arrRawChiptune.size() - intTailer));
				
				// Interleave file if it is not
				if (!isInterleaved)
				{
					// Interleave data (set all data for R0 then all for R1 ...)
					Vector<Byte> arrInterleavedRawChiptune = new Vector();
					for(int reg=0;reg<intNbRegisters;reg++)
					{
						for (int i=0;i<intVBL.longValue();i++)
						{
							arrInterleavedRawChiptune.add(arrRegistersValues.elementAt(i*intNbRegisters+reg));
						}
					}
					arrRegistersValues = arrInterleavedRawChiptune;
				}				
				
				// Extra log : Offsets in original YM file
				for (int i=0;i<intNbRegisters;i++) YMC_Tools.debug("Offset reg " + i + ": " + (i*(arrRegistersValues.size()/intNbRegisters) + intOffset)); 
			}			
			
		// return if not an YM file
		if (arrRegistersValues == null) return null;
		
		/**
		 * Create array Frame
		 */
		int intFileSize = arrRegistersValues.size();
		int intNbFrames = intFileSize/intNbRegisters;
		
		// Digidrums log
		//if (arrSampleLog == null)  arrSampleLog = new Vector<SampleInstance[]>();
		
		//	Get array of bytes during next loop
		byte arrPSGRead[] = new byte[intNbRegisters];
		byte arrPSG[] = new byte[YMC_Tools.CPC_REGISTERS];
		
		// Cut the big array into an Array of Frames
		Vector<Frame> arrFrame = new Vector<Frame>();
		for (int j=0; j<intNbFrames; j++)
		{
			//	Get array of bytes
			for (int i=0;i<intNbRegisters;i++)arrPSGRead[i] = arrRegistersValues.get(i*intNbFrames + j).byteValue();
			
			// Copy registers 0-10
			for (byte reg=0; reg<11;reg++) arrPSG[reg] = arrPSGRead[reg];
			
			// Get Special FX
			//arrSampleLog.add(new SampleInstance[3]);
			SampleInstance arrSI[] = blnReadFX(arrPSG, arrPSGRead/*, j*/);
			
			int dbFreq0 = arrPSG[1] & 0xF;
			dbFreq0 <<=	8; 
			dbFreq0 += 	((int)arrPSG[0]) & 0xFF;

			int dbFreq1 = arrPSG[3] & 0xF;
			dbFreq1 <<=	8; 
			dbFreq1 += 	((int)arrPSG[2]) & 0xFF;

			int dbFreq2 = arrPSG[5] & 0xF;
			dbFreq2 <<=	8; 
			dbFreq2 += 	((int)arrPSG[4]) & 0xFF;
			
			int dbFreqE = ((int)arrPSG[12]) & 0xFF;
			dbFreqE <<=	8; 
			dbFreqE += 	((int)arrPSG[11]) & 0xFF;
			
			//System.out.println(dbFreqE);

			int dbFreqN = ((int)arrPSG[6]) & 0x1F;
			
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
					new double[]{dbFreq0, dbFreq1, dbFreq2},	// Freq channels 0,1,2
					dbFreqN,									// Freq Noise
					dbFreqE,									// Freq Env
					bytMixer,									// Mixer (no noise at start)
					new byte[]{bytVol0, bytVol1, bytVol2},		// Vol channels 0,1,2
					bytEnv,										// Env
					arrSI										// Samples Instance
				));
		}
					
		return arrFrame;
	}
	
	/**
	 * Return an array of Samples contained in that chiptune
	 * This function should be overriden in subclasses 
	 */
	protected Sample[] getSamples()
	{
		// Create samples for YM2 files 
		if (strFileType.equals("YM2!") && (total_sample>0))
		{
			arrDigiDrums = new Sample[total_sample];
			for (int i=0;i<sampleRedirect.length;i++)
			{
				if (blSampleUsed[i])
				{
					// FIXME assume that all sample YM2 ar at 6083hz ... not sure
					arrDigiDrums[sampleRedirect[i]] = new Sample(	String.valueOf(i), 
																	sampleAdress[i].length, 
																	(byte)8, 
																	false, // Not signed
																	(byte)0, 
																	(byte)0xF, 
																	0, 
																	0, 
																	sampleAdress[i]/*,
																	6083l*/);
				}
			}
		}
		else if ((strFileType.equals("YM5!") || strFileType.equals("YM6!")) && (total_sample>0))
		{
			Sample[] arrDigiDrumsAux = new Sample[total_sample];
			for (int i=0;i<sampleRedirect.length;i++)
				if (blSampleUsed[i]) arrDigiDrumsAux[sampleRedirect[i]] = arrDigiDrums[i];
			arrDigiDrums = arrDigiDrumsAux;
		}
		
		return arrDigiDrums;
	}
	
	private SampleInstance[] blnReadFX(byte[] arrRegs, byte[] arrRegsRead/*, int pos*/) {
		SampleInstance arrSI[] = new SampleInstance[]{null, null, null};
		if (strFileType.equals("YM2!")) 
		{
			readYm2Effect(arrSI, arrRegs, arrRegsRead/*, pos*/);
		}
		else
		{
			for(byte reg = 11;reg<YMC_Tools.CPC_REGISTERS;reg++) arrRegs[reg] = arrRegsRead[reg];
			if (strFileType.equals("YM6!"))
			{
				readYm6Effect(arrSI, arrRegsRead/*, pos*/, 1,6,14);
				readYm6Effect(arrSI, arrRegsRead/*, pos*/, 3,8,15);
			}
			else if (strFileType.equals("YM5!"))
			{
				readYm5Effect(arrSI, arrRegsRead/*, pos*/);
			}				
		}
		
		return arrSI;
	}
	
	/**
	 *  YM2 Digidrums
	 *  
	 */
	private boolean readYm2Effect(SampleInstance[] arrSI, byte[] arrRegs, byte[] arrRegsRead/*, int pos*/)
	{
		boolean blnSpecialFX = false;
		
		if (arrRegsRead[13] != (byte)0xFF) // MadMax specific (extracted from StSoundLibrary from Leonard)
		{
			arrRegs[11] = arrRegsRead[11];
			arrRegs[12] = 0;
			arrRegs[13] = 10;
		}
		else arrRegs[13] = (byte)0xFF;
		if ((arrRegsRead[10]&0x80) != 0)					// bit 7 volume canal C pour annoncer une digi-drum madmax.
		{
			int	sampleNum;
			arrRegs[7] |= 0x24;				// Coupe TONE + NOISE canal C.
			sampleNum = arrRegsRead[10]&0x7f;	// Numero du sample

			// remove digidrum info
			arrRegs[10] ^= 0x80; 
			
			if (arrRegsRead[12] != 0)
			{
				blnSpecialFX = true;
				//sampleFrq = ((MFP_CLOCK>>2) / arrRegs[12]);
				
				// Sample redirect
				if ( sampleNum<blSampleUsed.length && !blSampleUsed[sampleNum])
				{
					sampleRedirect[sampleNum] = total_sample++;
					blSampleUsed[sampleNum] = true;
				}
				
				// Log digidrum
				arrSI[2] = new SampleInstance(SpecialFXType.ATARI_DIGIDRUM, sampleRedirect[sampleNum], 1, arrRegsRead[12] & 0xFF, 0);
				//arrSampleLog.get(pos)[2] = new SampleInstance(SampleInstance.FX_TYPE_ATARI_DIGIDRUM, sampleRedirect[sampleNum], 1, arrRegsRead[12] & 0xFF);
				YMC_Tools.debug("Sample " + sampleNum + " " + MFP_CLOCK/(4*(arrRegsRead[12] & 0xFF)) + "Hz");
			}			
		}	
		
		return blnSpecialFX;
	}
	
	/**
	 *  YM5 SpecialFX
	 *  
	 */
	private boolean readYm5Effect(SampleInstance[] arrSI, byte[] arrRegsRead/*, int pos*/)
	{
		// YM5 effect decoding
		boolean blnSpecialFX = false;

		//------------------------------------------------------
		// Sid Voice !!
		//------------------------------------------------------
		int code = (arrRegsRead[1]>>4)&3;
		if (code!=0)
		{
			long tmpFreq;
			int voice = code-1;
			int prediv = (arrRegsRead[6]>>5)&7;
			int count = arrRegsRead[14] & 0xFF;
			tmpFreq = mfpPrediv[prediv]*count;
			if (tmpFreq != 0)
			{
				blnSidVoice = true;
				blnSpecialFX = true;
				tmpFreq = 2457600L / tmpFreq;
				int Vmax = (arrRegsRead[voice+8]&15);
				//ymChip.sidStart(voice,tmpFreq,ptr[voice+8]&15);
				arrSI[voice] = new SampleInstance(	SpecialFXType.ATARI_SIDVOICE,
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
		code = (arrRegsRead[3]>>4)&3;
		if (code!=0)
		{	// Ici un digidrum demarre sur la voie voice.
			int voice = code-1;
			int ndrum = arrRegsRead[8+voice]&31;
			if ((ndrum>=0) && (ndrum<arrDigiDrums.length))
			{
				//long sampleFrq;
				int prediv = mfpPrediv[(arrRegsRead[8]>>5)&7];
				prediv *= arrRegsRead[15];
				if (prediv != 0)
				{
					blnSpecialFX = true;
					//sampleFrq = MFP_CLOCK / prediv;
					//arrSampleLog.get(pos)[voice] = new SampleInstance(ndrum, (arrRegsRead[8]>>5)&7, arrRegsRead[15]);
					
					// Sample redirect
					if ( ndrum<blSampleUsed.length && !blSampleUsed[ndrum])
					{
						sampleRedirect[ndrum] = total_sample++;
						blSampleUsed[ndrum] = true;
					}
					
					// Log digidrum
					arrSI[voice] = new SampleInstance(SpecialFXType.ATARI_DIGIDRUM, sampleRedirect[ndrum], (arrRegsRead[8]>>5)&7, arrRegsRead[15] & 0xFF, 1);
					//arrSampleLog.get(pos)[voice] = new SampleInstance(SampleInstance.FX_TYPE_ATARI_DIGIDRUM, sampleRedirect[ndrum], (arrRegsRead[8]>>5)&7, arrRegsRead[15] & 0xFF);
				}
			}
		}		
		return blnSpecialFX;
	}
	
	/**
	 * YM6 effects
	 *  
	 */
	private boolean readYm6Effect(SampleInstance[] arrSI, byte[] pReg, int code, int prediv, int count)
	{
		// Get the number of FX (usefull in AYC)
		int nbFx = count-14; 
		
		code = (byte)(pReg[code]&0xf0);
		prediv = (pReg[prediv]>>5)&7;
		count = pReg[count]  & 0xFF;
		boolean blnSpecialFX = false;
		
		if ((code&0x30) != 0)
		{
			long tmpFreq;
			// Ici il y a un effet sur la voie:

			int voice = ((code&0x30)>>4)-1;
			
			// Detect SpecialFX
			switch (code&0xc0)
			{
				case 0x00:		// SID
					blnSidVoice = true;
					break;
				case 0x80:		// Sinus-SID
					blnSinSid = true;
					break;
				case 0xc0:		// Sync-Buzzer.
					blnSyncBuzzer = true;
					break;
			}
			
			
			switch (code&0xc0)
			{
				case 0x00:		// SID
				case 0x80:		// Sinus-SID
					tmpFreq = mfpPrediv[prediv]*count;
					if (tmpFreq != 0)
					{
						blnSpecialFX = true;
						int Vmax = (pReg[voice+8]&15);
						tmpFreq = MFP_CLOCK / tmpFreq;
						if ((code&0xc0)==0x00)
						{
							//YMC_Tools.debug("+ Sid Sound [" + voice + ", " + Vmax + ", " + tmpFreq + "]");
							arrSI[voice] = new SampleInstance(	SpecialFXType.ATARI_SIDVOICE,
									Vmax,
									prediv,
									count,
									nbFx);
						}
						else
						{
							YMC_Tools.debug("+ SinSid Sound [" + voice + ", " + Vmax + ", " + tmpFreq + "]");
							arrSI[voice] = new SampleInstance(	SpecialFXType.ATARI_SINSID,
									Vmax,
									prediv,
									count,
									nbFx);
						}
					}
					break;

				case 0x40:		// DigiDrum
					int ndrum = pReg[voice+8]&31;
					if ((ndrum>=0) && (ndrum<arrDigiDrums.length))
					if (ndrum>=0)
					{
						//prediv = mfpPrediv[prediv];
						//prediv *= count;
						if (mfpPrediv[prediv]*count>0)
						{
							blnSpecialFX = true;
							tmpFreq = MFP_CLOCK / mfpPrediv[prediv]*count;
							//arrSampleLog.get(pos)[voice] = new SampleInstance(ndrum, prediv, count);
							
							// Sample redirect
							if ( ndrum<blSampleUsed.length && !blSampleUsed[ndrum])
							{
								sampleRedirect[ndrum] = total_sample++;
								blSampleUsed[ndrum] = true;
							}
							
							// Log digidrum
							arrSI[voice] = new SampleInstance(SpecialFXType.ATARI_DIGIDRUM, sampleRedirect[ndrum], prediv, count, nbFx);
							//arrSampleLog.get(pos)[voice] = new SampleInstance(SampleInstance.FX_TYPE_ATARI_DIGIDRUM, sampleRedirect[ndrum], prediv, count);
						}
					}
					break;

				case 0xc0:		// Sync-Buzzer.
					tmpFreq = mfpPrediv[prediv]*count;
					if (tmpFreq != 0)
					{
						blnSpecialFX = true;
						tmpFreq = MFP_CLOCK / tmpFreq;
						int Env = (pReg[voice+8]&15);
						YMC_Tools.debug("+ Sync-Buzzer [" + Env + ", " + tmpFreq + "]");
						arrSI[voice] = new SampleInstance(	SpecialFXType.ATARI_SYNCBUZZER,
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
	
	/**
	 * Digidrums YM2 Load
	 */
	private byte[] sampleAdress[] = {
		new byte[631], new byte[631], new byte[490], new byte[490], new byte[699], new byte[505], new byte[727], new byte[480],
		new byte[2108], new byte[4231], new byte[378], new byte[1527], new byte[258], new byte[258], new byte[451], new byte[1795],
		new byte[271], new byte[636], new byte[1379], new byte[147], new byte[139], new byte[85], new byte[150], new byte[507],
		new byte[230], new byte[120], new byte[271], new byte[293], new byte[391], new byte[391], new byte[391], new byte[407],
		new byte[407], new byte[407], new byte[317], new byte[407], new byte[311], new byte[459], new byte[329], new byte[656]};	
}
