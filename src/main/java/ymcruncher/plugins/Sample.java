package ymcruncher.plugins;


/**
 * Sample
 */
public class Sample
{
	// Convert 8bits to 4bits sample (Given by gwem from the Atari scene)
	final private static byte[] arr8to4bits = { 0x0, 0x1, 0x2, 0x3, 0x3, 0x4, 0x5, 0x5, 0x6, 0x6, 0x6, 0x7, 0x7, 0x7, 0x7, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF};
	
	// Convert 4bits to 8bits sample (Extracted from STSound by Leonard)
	final protected byte[] arr4to8bits = { 0x0, 0x1, 0x2, 0x2, 0x4, 0x6, 0x9, 0xC, 0x11, 0x18, 0x23, 0x30, 0x48, 0x67, (byte)0xA5, (byte)0xFF };
	
	/* Volume level (Gwem, Falcon) 
	$0 = .000                $8 = .069
	$1 = .005                $9 = .095
	$2 = .008                $A = .139
	$3 = .012                $B = .191
	$4 = .018                $C = .287
	$5 = .024                $D = .407
	$6 = .036                $E = .648
	$7 = .048                $F = 1.00
	*/
	
	/**
	 * Data members
	 */
	private String name = null;
	private int length;
	private byte resolution;
	private boolean signed;
	private byte finetune;
	private byte volume;
	private int repeat_offset;
	private int repeat_length;
	private byte[] arrWave = null;
	
	// If a Sample has a frequency that's zero, then it's a Drum which has the same frequency
	// wherever in the chiptune 
	private double rate = 0;
	private boolean isDigiDrum = false;
	
	/**
	 * Constructor
	 */
	Sample(String pname, int plength, byte presolution, boolean pSigned, byte pfinetune, byte pvolume, int prepeat_offset, int prepeat_length, byte[] parrWave)
	{
		name = pname;
		length = plength;
		resolution = presolution;
		signed = pSigned;
		finetune = pfinetune;
		volume = pvolume;
		repeat_offset = prepeat_offset;
		repeat_length = prepeat_length;
		arrWave = parrWave;
	}
	
	private Sample(String pname, int plength, byte presolution, boolean pSigned, byte pfinetune, byte pvolume, int prepeat_offset, int prepeat_length, byte[] parrWave, double freq)
	{
		name = pname;
		length = plength;
		resolution = presolution;
		signed = pSigned;
		finetune = pfinetune;
		volume = pvolume;
		repeat_offset = prepeat_offset;
		repeat_length = prepeat_length;
		arrWave = parrWave;
		rate = freq;
	}
	
	
	/**
	 * Clone
	 */
	public Sample copyclone()
	{
		// arrWave.clone()
		byte arrWaveClone[] = null;
		if (arrWave != null)
		{
			arrWaveClone = new byte[arrWave.length];
			System.arraycopy(arrWave, 0, arrWaveClone, 0, arrWave.length);
		}
		
		return new Sample(
				(name == null)?null:new String(name),
				length,
				resolution,
				signed,
				finetune,
				volume,
				repeat_offset,
				repeat_length,
				arrWaveClone, //arrWave.clone(),
				rate);
	}
	
	/**
	 * Set Data
	 */
	public void setWave(byte[] parrWave)
	{
		arrWave = parrWave;
	}
	
	/**
	 * Acess data
	 */
		
	public String getName()
	{return name;}
	
	public int getLength()
	{return length;}
	
	public byte getFinetune()
	{return finetune;}
	
	public byte getVolume()
	{return volume;}
	
	public int getRepeatOffset()
	{return repeat_offset;}
	
	public int getRepeatlength()
	{return repeat_length;}
	
	public byte[] getWave()
	{return arrWave;}

	// Return a Wave that is either 8bits unsigned or 16bits signed
	public int[] getWave8bits()
	{
		int arrWaveAux[] = new int[arrWave.length];
		for(int i=0;i<arrWaveAux.length;i++)
		{
			int bDD = arrWave[i];
			
			// Convert 8bits samples to 4 bits samples
			if (getResolution() == 8)
			{
				// Convert signed sample to unsigned
				if (isSigned())
					bDD +=128;
				
				bDD &= 0xFF;				
			}
			else
			{
				// Convert signed sample to unsigned
				if (isSigned())
					bDD +=8;
				
				bDD &= 0xF;
				
				// Convert 4bits samples to 8 bits samples
				bDD = arr4to8bits[bDD] & 0xFF;				
				
			}
			
			// here we've got a 8bits unsigned wave
			arrWaveAux[i] = bDD;
		}
		return arrWaveAux;
	}
	
	/**
	 * @return Returns the resolution.
	 */
	public byte getResolution() {
		return resolution;
	}

	/**
	 * @param resolution The resolution to set.
	 */
	public void setResolution(byte resolution) {
		this.resolution = resolution;
	}

	/**
	 * @return Returns the signed.
	 */
	public boolean isSigned() {
		return signed;
	}

	/**
	 * @param signed The signed to set.
	 */
	public void setSigned(boolean signed) {
		this.signed = signed;
	}
	
	public double getRate() {
		return rate;
	}

	public void setRate(double rate) {
		this.rate = rate;
	}

	public boolean isDigiDrum() {
		return (rate!=0);
	}
}