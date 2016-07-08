package YMCruncher;

import Plugins.SampleInstance;

/**
 * The aim of this class is to have a sort of record that hold frequencies value in a double format, so that 
 * conversion error are the lowest possible.
 * We don't do any frequency conversion in this class !!! 
 */
public class Frame
{
	// Channels's frequency
	private double pPeriod[] = new double[]{0,0,0};
	
	// Noise's frequency
	private double pPeriodN = 0;
	
	// Evelope's frequency
	private double pPeriodE = 0;
	
	// Byte registers
	private byte bytReg7 = (7<<3)+7;			// Mute all
	private byte bytVol[] = new byte[]{0,0,0};
	private byte bytReg13 = 0;
	
	// Samples
	private SampleInstance arrSI[] = new SampleInstance[]{null, null, null};
		
	//*****************************************************
	// Constructors
	//*****************************************************
	public Frame(){}

	public Frame(double[] intPPeriod, double intPPeriodN, double intPPeriodE, byte bytReg7, byte[] bytVol, byte bytReg13, SampleInstance[] psi) {
		super();
		this.pPeriod = intPPeriod;
		this.pPeriodN = intPPeriodN;
		this.pPeriodE = intPPeriodE;
		this.bytReg7 = bytReg7;
		this.bytVol = bytVol;
		this.bytReg13 = bytReg13;
		this.arrSI = psi;
	}
	
	//*****************************************************
	//* Functions
	//*****************************************************
		
	public Frame copyclone() {
		
		// Clone Arrays
		double[] arrPP = null;
		if (pPeriod != null)
		{
			arrPP = new double[pPeriod.length];
			for(int i=0;i<pPeriod.length;i++) arrPP[i] = pPeriod[i];
		}
		
		byte[] arrV = null;
		if (bytVol != null)
		{
			arrV = new byte[bytVol.length];
			for(int i=0;i<bytVol.length;i++) arrV[i] = bytVol[i];
		}
		
		SampleInstance[] arrS = null;
		if (arrSI != null)
		{
			arrS = new SampleInstance[arrSI.length];
			for(int i=0;i<arrSI.length;i++) arrS[i] = (arrSI[i]==null)?null:arrSI[i].copyclone();
		}
		
		
		return new Frame(
							arrPP,
							this.pPeriodN,
							this.pPeriodE,
							this.bytReg7,
							arrV,
							this.bytReg13,
							arrS);
	}
	
	//*****************************************************
	// Getters and Setters
	//*****************************************************
	public byte getBytReg13() {
		return bytReg13;
	}

	public void setBytReg13(byte bytReg13) {
		this.bytReg13 = bytReg13;
	}

	public boolean blnToneEnable(int c)
	{
		return ((bytReg7 & (1<<c)) == 0);
	}
	
	public boolean blnNoiseEnable(int c)
	{
		return ((bytReg7 & (8<<c)) == 0);
	}	

	public boolean blnEnvEnable(int c)
	{
		return ((bytVol[c] & 0x10)>>4 == 1);
	}		
	
	public byte getBytReg7() {
		return bytReg7;
	}

	public void setBytReg7(byte bytReg7) {
		this.bytReg7 = bytReg7;
	}

	public byte getBytVol(int i) {
		return ((bytVol != null) && (i<bytVol.length) && (i>=0))?
				bytVol[i]:
				0;
	}

	public void setBytVol(int i, byte bytVol) {
		if ((this.bytVol != null) && (i<this.bytVol.length) && (i>=0))
			this.bytVol[i] = bytVol;
	}

	public double getPPeriod(int i) {
		return ((pPeriod != null) && (i<pPeriod.length) && (i>=0))?
				pPeriod[i]:
				0;
	}

	public void setPPeriod(int i, double intPPeriod) {
		if ((this.pPeriod != null) && (i<this.pPeriod.length) && (i>=0))
			this.pPeriod[i] = intPPeriod;
	}

	public double getPPeriodE() {
		return pPeriodE;
	}

	public void setPPeriodE(double intPPeriodE) {
		this.pPeriodE = intPPeriodE;
	}

	public double getPPeriodN() {
		return pPeriodN;
	}

	public void setPPeriodN(double intPPeriodN) {
		this.pPeriodN = intPPeriodN;
	}
	
	public SampleInstance getSI(int i)
	{
		return ((arrSI != null) && (i<arrSI.length) && (i>=0))?
					arrSI[i]:
					null;
	}

	public void setSI(int i, SampleInstance sampi)
	{
		if ((arrSI != null) && (i<arrSI.length) && (i>=0))
			arrSI[i] = sampi;
	}
		
	public byte[] getByteArray() {
		byte[] arrRegs = new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		for (int i=0;i<arrRegs.length;i++) arrRegs[i] = getReg(i);
		return arrRegs;		
	}
	
	public byte getReg(int i)
	{
		switch (i)
		{
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
				long lngFreq = (long)pPeriod[i/2];
				if (lngFreq > 0xFFF) lngFreq = 0xFFF;				
				if ((i & 0x1) == 0)	// 0,2,4
					return (byte)( lngFreq & 0xFF);
				else				// 1,3,5
					return (byte)((lngFreq>>8) & 0xF);
			case 8:
			case 9:
			case 10:
				Byte bytVolChannel = (Byte)bytVol[i-8];
				return (byte)(bytVolChannel & 0x1F);
			case 6:
				lngFreq = (long)pPeriodN;
				if (lngFreq > 0x1F) lngFreq = 0x1F;
				return (byte)( lngFreq & 0x1F);
			case 7:
				return (byte)(bytReg7 & 0x3F);
			case 11:
				lngFreq = (long)pPeriodE;
				if (lngFreq > 0xFFFF) lngFreq = 0xFFFF;
				return (byte)( lngFreq & 0xFF);
			case 12:
				lngFreq = (long)pPeriodE;
				if (lngFreq > 0xFFFF) lngFreq = 0xFFFF;
				return (byte)((lngFreq>>8) & 0xFF);
			case 13:
				return (byte)(bytReg13 & 0xFF);
			default:
				return 0;
		}		
	}
}