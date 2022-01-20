/**
 * 
 */
package ymcruncher.plugins;

import java.util.Vector;

import ymcruncher.core.Chiptune;
import ymcruncher.core.Frame;
import ymcruncher.core.InputPlugin;
import ymcruncher.core.YMC_Tools;

/**
 * @author eu734
 *
 */
public class MymInputPlugin extends InputPlugin {

	final private static byte REGS=14;
	final private static short FRAG=128;    /*  Number of rows to compress at a time   */
	final private static byte OFFNUM=14;   /*  Bits needed to store off+num of FRAG  */
	final private static byte regbits[]={8,4,8,4, 8,4,5,8, 5,5,5,8, 8,8}; /* Bits per PSG reg */
	
	// For function getBits
	private static int lngCurrBitOffset = 16;
	private static byte bytRead = 0;
	
	/* (non-Javadoc)
	 * @see main.YMCruncher.InputPlugin#getPreProcessedChiptune(java.util.Vector, java.lang.String)
	 */
	@Override
	protected Chiptune getPreProcessedChiptune(Vector arrRawChiptune, String strExt) {
		
		if (strExt.toUpperCase().equals("MYM"))
		{
			// Set file style
			strFileType = "MYM";
			
			int intNbFrag = YMC_Tools.getLEShort(arrRawChiptune, 0);
						
			// Stats
			int intUnchangedFrag = 0;
			int intUnchangedReg = 0;
			int intRawData = 0;
			int intPreviousData = 0;
			
			// Init aux table
			byte data[][] = new byte[REGS][intNbFrag+FRAG];
			byte current[] = new byte[REGS];
			
			// Reset static variables
			lngCurrBitOffset = 16;
			bytRead = 0;
			
			// Total number of VBL cycles
			int nbVBL = 0;
			
			for(int n=0;n<intNbFrag;n+=FRAG)
				for(byte i=0;i<REGS;i++)
				{
					// If bit read == 0
					if(getBits((byte)1,arrRawChiptune) == 0)  /*  Totally unchanged fragment */
					{
						// Stat
						intUnchangedFrag++;
						
						nbVBL += FRAG;
						
						for(int row=0;row<FRAG;row++)
							data[i][n+row]=current[i];
		                continue;
					}
					
					// If bit read == 1
					int index = 0;
					while(index<FRAG)   /*  Packed fragment */
			        {
						if(getBits((byte)1,arrRawChiptune) == 0)  /*  Unchanged register  */
		                {
							// Stat
							intUnchangedReg++;

							nbVBL++;
							
		                    data[i][n+index]=current[i];
		                    index++;
		                }
		                else
		                {
		                    if(getBits((byte)1,arrRawChiptune) != 0)   /*  Raw data    */
		                    {
		                    	// Stat
		                    	intRawData++;
		                    	
		                    	nbVBL++;
		                    	
		                        byte c=getBits(regbits[i],arrRawChiptune);
		                        current[i]=data[i][n+index]=c;
		                        index++;
		                    }
		                    else    /*  Reference to previous data */
		                    {
		                        int compoff= 0xFF & getBits(OFFNUM/2,arrRawChiptune)+index;
		                        int compnum= 0xFF & getBits(OFFNUM/2,arrRawChiptune)+1;

		                    	// Stat
		                        intPreviousData+=compnum;
		                        
		                        nbVBL += compnum;
		                        
		                        for(int row=0;row<compnum;row++)
		                        {
		                            byte c=data[i][n-FRAG+compoff+row];
		                            data[i][n+index]=current[i]=c;
		                            index++;
		                        }
		                    }
		                }
			        }
				}
			
				// Set nbVBL
				nbVBL /= REGS;
			
				YMC_Tools.info("+ NbVBL = 0x" + Long.toHexString(nbVBL).toUpperCase() + " (" + intNbFrag/50 + " seconds)");			
				YMC_Tools.debug("+ Number of unchanged fragment = " + intUnchangedFrag);
				YMC_Tools.debug("+ Number of unchanged register = " + intUnchangedReg);
				YMC_Tools.debug("+ Number of row data = " + intRawData);
				YMC_Tools.debug("+ Number of previous data = " + intPreviousData);
			
				// Cut the big array into an Array of Frames
				Vector<Frame> arrFrame = new Vector<Frame>();
				for (int j=0; j<nbVBL; j++)
				{				
					int dbPPeriod0 = data[1][j] & 0xF;
					dbPPeriod0 <<=	8; 
					dbPPeriod0 += 	((int)data[0][j]) & 0xFF;
	
					int dbPPeriod1 = data[3][j] & 0xF;
					dbPPeriod1 <<=	8; 
					dbPPeriod1 += 	((int)data[2][j]) & 0xFF;
	
					int dbPPeriod2 = data[5][j] & 0xF;
					dbPPeriod2 <<=	8; 
					dbPPeriod2 += 	((int)data[4][j]) & 0xFF;
					
					int dbPPeriodE = ((int)data[12][j]) & 0xFF;
					dbPPeriodE <<=	8; 
					dbPPeriodE += 	((int)data[11][j]) & 0xFF;
	
					int dbPPeriodN = ((int)data[6][j]) & 0xFF;
					
					byte bytMixer = data[7][j];
					
					byte bytVol0 = data[8][j];
					byte bytVol1 = data[9][j];
					byte bytVol2 = data[10][j];			
					
					byte bytEnv = data[13][j];
					
					arrFrame.add(new Frame(
							new double[]{dbPPeriod0, dbPPeriod1, dbPPeriod2},	// Freq channels 0,1,2
							dbPPeriodN,									// Freq Noise
							dbPPeriodE,									// Freq Env
							bytMixer,									// Mixer (no noise at start)
							new byte[]{bytVol0, bytVol1, bytVol2},		// Vol channels 0,1,2
							bytEnv,										// Env
							new SampleInstance[]{null, null, null}		// Samples Instance
						));
				}
				
				return new Chiptune(null,
						null,
						arrFrame,
						YMC_Tools.CPC_REPLAY_FREQUENCY,
						YMC_Tools.YM_ATARI_FREQUENCY,
						false,
						0,
						null);
		}
		
		// Nothing to do
		return null;
	}

	private byte getBits(int nbBits, Vector<Byte> arrRawChiptune)
	{
		byte bytRet = 0;
		
		for (int i=0;i<nbBits;i++)
		{
			byte bytIndex = (byte)(lngCurrBitOffset & 0x7);
			
			if ( bytIndex == 0) // New byte
				bytRead = arrRawChiptune.get(lngCurrBitOffset/8);
			
			// Get bits
			bytRet<<=1;
			if ((bytRead & (0x80>>bytIndex)) != 0) bytRet |= 1;
			
			// Inc Bits read
			lngCurrBitOffset++;
		}
		
		return bytRet;
	}
}
