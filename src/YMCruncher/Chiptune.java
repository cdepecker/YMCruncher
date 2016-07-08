package YMCruncher;

import java.util.HashMap;
import java.util.Vector;

import Plugins.Sample;
import Plugins.SampleInstance;
import Plugins.SpecialFXType;
/**
 * Chiptune
 * @author eu734
 *
 */
public class Chiptune
{

	//*****************************************************
	// Private Data
	//*****************************************************
	
	// Is loaded
	private boolean isLoaded = false;
	
	// ID3
	private String strSongName = null;		
	private String strAuthorName = null;
	
	// Data
	private int length=0;
	private Vector<Frame> arrFrame = null;
	private int playRate = YMC_Tools.CPC_REPLAY_FREQUENCY;
	private long frequency = YMC_Tools.YM_CPC_FREQUENCY;
	private boolean blnLoop = false;
	private long loopVBL = 0;

	// Sample & Digidrums
	private Sample[] arrSamples = null;
	
	// Log
	private StringBuffer sbLog = null;
			
	
	//*****************************************************
	// Constructors
	//*****************************************************	
	public Chiptune(){}
	
	public Chiptune(	String pSongName,
						String pAuthor,
						Vector<Frame> parrFrame,
						int prate,
						long pfrequency,
						boolean pblnLoop,
						long ploopVBL,
						Sample[] parrSamples)
	{
		// ID3
		strSongName = pSongName;
		strAuthorName = pAuthor;
		
		// Data
		length = parrFrame.size();
		arrFrame = parrFrame;
		playRate = prate;
		frequency = pfrequency;
		blnLoop = pblnLoop;
		loopVBL = ploopVBL;
		
		// Samples & Digidrums
		arrSamples = parrSamples;

		/*
		 * For Every Atari Digidrums
		 * Try to find the static frequency in the chiptune
		 */ 		
		if (arrSamples != null)
		{
			double sample_rate[] = new double[arrSamples.length];
			boolean sample_is_digidrum[] = new boolean[arrSamples.length]; 
			for(int i=0;i<sample_rate.length;i++)
			{
				sample_rate[i] = 0;
				sample_is_digidrum[i] = true;
			} 
			
			for(int v=0;v<arrFrame.size();v++)
			{						 
				for(int si_count=0;si_count<3;si_count++)
				{
					SampleInstance si = arrFrame.get(v).getSI(si_count);
					if ((si == null) || si.getType() != SpecialFXType.ATARI_DIGIDRUM) continue;
					
					int nbSample = si.getSample();
					if (sample_rate[nbSample] == 0)
						sample_rate[nbSample] = si.getRate();
					else if (sample_rate[nbSample] != si.getRate())
						sample_is_digidrum[nbSample] = false;
				}
			}
			
			for(int i=0;i<sample_is_digidrum.length;i++)
				if (sample_is_digidrum[i])
					arrSamples[i].setRate(sample_rate[i]);
		}
		
		isLoaded = true;
	}
	
	
	//*****************************************************
	// Functions
	//*****************************************************	
	public Chiptune copyclone()
	{		
		// Copy arrFrame
		Vector<Frame> arrCloneFrame = new Vector<Frame>();		
		for(int i=0;i<length;i++)
		{
			// clone Frame
			Frame frame = getFrame(i).copyclone();
			arrCloneFrame.add(frame);
		}
		
		// Copy arrSamples	
		Sample[] arrSamplesClone = null;
		if (arrSamples != null)
		{
			arrSamplesClone = new Sample[arrSamples.length];
			for(int i=0;i<arrSamples.length;i++) arrSamplesClone[i] = arrSamples[i].copyclone();
		}
						
		Chiptune chip = new Chiptune(
				// ID3
				(strSongName == null)?null:new String(strSongName),
				(strAuthorName == null)?null:new String(strAuthorName),
				
				// Data
				arrCloneFrame,
				playRate,
				frequency,
				blnLoop,
				loopVBL,
				
				// Sample & Digidrums
				arrSamplesClone
				);
	
		// return cloned chiptune
		return chip;
	}
	
	public void filter(boolean blnConvertFrequency, boolean blnAdjustFrequency, long lngClockFrequency, byte bytReg7Filter, boolean[] arrEnvFilter, HashMap<SpecialFXType, Boolean> arrSpecialFXFilter, boolean blnNullPeriodDisableChannel)
	{
		// get Convertion Ratio (Tone & Noise)
		double dbConvertRatio = (double)lngClockFrequency / frequency;
			
		//Number of values to adjust sequentially (Channels 0, 1, 2) 
		int arrFreqAdjustLength[] = new int[]{0,0,0}; 
		int noiseFreqAdjustLength = 0;
		int envFreqAdjustLength = 0;

		// Deltas
		double arrFreqAdjustDelta[] = new double[]{0,0,0}; 
		double noiseFreqAdjustDelta = 0;
		double envFreqAdjustDelta = 0;
		
		// Deltas
		double arrFreqAdjustValue[] = new double[]{0,0,0}; 
		double noiseFreqAdjustValue = 0;
		double envFreqAdjustValue = 0;		
		
		// Filter Chiptune
		for (int i=0;i<arrFrame.size(); i++)
		{
			// Current frame
			Frame frame = arrFrame.get(i);
			
			// Mixer Filter
			// Disable Channel Mixing when period is null
			byte mixFilter = bytReg7Filter;
			for(int v=0;v<3;v++){
				if (frame.getPPeriod(v) < 1) mixFilter |= (1<<v);
			}
			frame.setBytReg7((byte)(frame.getBytReg7() | mixFilter));
			
			// Filter SpecialFX
			for(byte c=0;c<3;c++)
			{
				SampleInstance si = frame.getSI(c);
				if ((si != null) && (!arrSpecialFXFilter.get(si.getType()))) frame.setSI(c, null);
			}
			
			// Env Filter
			for (int c=0; c<3; c++)
				if (!arrEnvFilter[c]) frame.setBytVol(c, (byte)(frame.getBytVol(c) & 0xF));
			
			// Convert Frequency if needed
			if (blnConvertFrequency && (lngClockFrequency != frequency))
			{
				// Convert Frequencies for Channels 0,1,2
				for(byte c=0;c<3;c++)
					freqAdjust(frame, arrFreqAdjustLength, arrFreqAdjustValue, arrFreqAdjustDelta, c, i, dbConvertRatio);
								
				// Convert Frequencies for Channels N
				double dbConvertedValue = (double)(frame.getPPeriodN() * dbConvertRatio);
				frame.setPPeriodN((int)(dbConvertedValue + 0.5d));				
				

				if (!blnAdjustFrequency)
				{
					
					
					dbConvertedValue = (double)(frame.getPPeriodE() * dbConvertRatio);			
					
					// HACK TAO - Seagulls
					// Add period on the channel A if period is zero (worth a try)
//					if (((int)(dbConvertedValue + 0.5d) != ((int)(dbConvertedValue))) && 
					if ((frame.getPPeriod(0) == 0d) && (frame.getPPeriodE()!= 0d)){
						double dbConvertedValuePeriod = (double)(frame.getPPeriodE() * 16d * dbConvertRatio);
						frame.setPPeriod(0,(int)(dbConvertedValuePeriod + 0.5d));
						System.out.println(	"ENV freq = " + frequency/(256*frame.getPPeriodE()) + 
											" CONV = " + lngClockFrequency/(256*((int)(dbConvertedValue + 0.5d))) +
											" PERIOD = " + lngClockFrequency/(16*((int)(dbConvertedValuePeriod + 0.5d))));
					}
					
					frame.setPPeriodE((int)(dbConvertedValue + 0.5d));
				}
				else if (envFreqAdjustLength == 0)
				{
					int j = i+1;
					while ( (j<arrFrame.size()) && 
							(arrFrame.get(j).getPPeriodE() == frame.getPPeriodE()))
					{
						envFreqAdjustLength++;
						j++;
					}
					
					// We Adjust only if arrFreqAdjustLength[c] > 1
					if (envFreqAdjustLength > 0)
					{
						envFreqAdjustValue = (double)(frame.getPPeriodE() * dbConvertRatio);
						
						// get intValue 
						int intValue = (int)envFreqAdjustValue;
						
						// Get Delta
						envFreqAdjustDelta = envFreqAdjustValue - intValue;
						
						// First is always an int value
						frame.setPPeriodE(intValue); 
					}
					else
					{
						// Normal adjust
						dbConvertedValue = (double)(frame.getPPeriodE() * dbConvertRatio); 
						frame.setPPeriodE((int)(dbConvertedValue  + 0.5d));
					}											
				}
				else
				{					
					// We are in the Adjust Loop
					double dbF = envFreqAdjustValue + envFreqAdjustDelta;
					frame.setPPeriodE((int)dbF);
					
					// If reached next integer values, then go back to previous
					if (((int)dbF) > ((int)envFreqAdjustValue)) envFreqAdjustValue = dbF - 1;
					else  envFreqAdjustValue = dbF;
					
					envFreqAdjustLength--;
				}
			}
		}
		
		// Change frequency
		if (blnConvertFrequency) frequency = lngClockFrequency;
	}
	
	private void freqAdjust(Frame frame,					// current frame
							int arrFreqAdjustLength[],   
							double arrFreqAdjustValue[],
							double arrFreqAdjustDelta[],
							int c,							// channel 0,1,2
							int i,							// current frame's indice
							double dbConvertRatio)
	{
		// Get number of values to adjust - 1
		if (arrFreqAdjustLength[c] == 0)
		{
			int j = i+1;
			while ( (j<arrFrame.size()) && 
					(arrFrame.get(j).getPPeriod(c) == frame.getPPeriod(c)))
			{
				arrFreqAdjustLength[c]++;
				j++;
			}
			
			// We Adjust only if arrFreqAdjustLength[c] > 1
			if (arrFreqAdjustLength[c] > 0)
			{
				arrFreqAdjustValue[c] = (double)(frame.getPPeriod(c) * dbConvertRatio);
				
				// get intValue 
				int intValue = (int)arrFreqAdjustValue[c];
				
				// Get Delta
				arrFreqAdjustDelta[c] = arrFreqAdjustValue[c] - intValue;
				
				// First is always an int value
				frame.setPPeriod(c, intValue); 
			}
			else
			{
				// Normal adjust
				double dbConvertedValue = (double)(frame.getPPeriod(c) * dbConvertRatio); 
				frame.setPPeriod(c, (int)(dbConvertedValue  + 0.5d));
			}						
			
			// next channel
			return;
		}
		
		// We are in the Adjust Loop
		double dbF = arrFreqAdjustValue[c] + arrFreqAdjustDelta[c];
		frame.setPPeriod(c, (int)dbF);
		
		// If reached next integer values, then go back to previous
		if (((int)dbF) > ((int)arrFreqAdjustValue[c])) arrFreqAdjustValue[c] = dbF - 1;
		else  arrFreqAdjustValue[c] = dbF;
		
		arrFreqAdjustLength[c]--;	
	}
	
	// This functions will try to convert to 50hz the input chiptune
	public void force50Hz(){
		double dbConvert = 50d/playRate;
		int nbFrames50Hz = (int)(arrFrame.size()*dbConvert + 0.5d);
		
		Vector<Frame> arrCloneFrame = new Vector<Frame>();
		
		for(int i=0;i<nbFrames50Hz;i++){
			// Get index in arrFrame
			// TODO Interpole with skipped frames ? (channels all but reg 7)
			int index = (int)(i/dbConvert + 0.5d);
			arrCloneFrame.add(getFrame(index).copyclone());
		}
				
		arrFrame = arrCloneFrame;
		length = arrCloneFrame.size();
		playRate = 50;
		
	}
	
	public Vector<Byte> getVectorRegister(int reg)
	{
		Vector<Byte> vect = new Vector<Byte>();
		
		for(int i=0;i<length;i++)
		{
			vect.add(getFrame(i).getReg(reg));
		}
		return vect;
	}	
		
	public Frame getFrame(int i)
	{
		if ((isLoaded) && (arrFrame==null)) return new Frame();
		return arrFrame.get(i);
	}
	
	//*****************************************************
	// Getters and Setters
	//*****************************************************
	
	public int getNbSamples()
	{
		return (arrSamples==null)?0:arrSamples.length;
	}
	
	public Sample[] getArrSamples() {
		return arrSamples;
	}

	public void setArrSamples(Sample[] arrSamples) {
		this.arrSamples = arrSamples;
	}

	public boolean isBlnLoop() {
		return blnLoop;
	}

	public void setBlnLoop(boolean blnLoop) {
		this.blnLoop = blnLoop;
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

	public void setLoaded(boolean isLoaded) {
		this.isLoaded = isLoaded;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public long getLoopVBL() {
		return loopVBL;
	}

	public void setLoopVBL(long loopVBL) {
		this.loopVBL = loopVBL;
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

	public Vector<Frame> getArrFrame() {
		return arrFrame;
	}

	public void setArrFrame(Vector<Frame> arrFrame) {
		this.arrFrame = arrFrame;
	}
}