import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import Plugins.Sample;
import Plugins.SampleInstance;
import YMCruncher.Frame;
import YMCruncher.YMC_Tools;

public class ExtractSamples {

	final private static String YM2_DIGIDRUM_FILE = "./YM2_digidrums.dat";	
	
	/**
	 * Digidrums YM2 Load
	 */

	private static byte[] sampleAdress[] = {
		new byte[631], new byte[631], new byte[490], new byte[490], new byte[699], new byte[505], new byte[727], new byte[480],
		new byte[2108], new byte[4231], new byte[378], new byte[1527], new byte[258], new byte[258], new byte[451], new byte[1795],
		new byte[271], new byte[636], new byte[1379], new byte[147], new byte[139], new byte[85], new byte[150], new byte[507],
		new byte[230], new byte[120], new byte[271], new byte[293], new byte[391], new byte[391], new byte[391], new byte[407],
		new byte[407], new byte[407], new byte[317], new byte[407], new byte[311], new byte[459], new byte[329], new byte[656]};
	
	// Convert 8bits to 4bits sample (Given by gwem from the Atari scene)
	final private static byte[] arr8to4bits = { 0x0, 0x1, 0x2, 0x3, 0x3, 0x4, 0x5, 0x5, 0x6, 0x6, 0x6, 0x7, 0x7, 0x7, 0x7, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF};
	
	// 15khz CPC+ sample frequency
	final private static double CPC_SAMPLE_FREQUENCY = 312*50; 
	
	// Frequency in hertz (2,4,6,8)
	private static double sampleFreq[] = {
		6083, 0, 6083, 0, 6083, 6083, 6083, 6083, 6083, 0, 
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		
	// Wav
	public static final void addLEvar(ArrayList<Byte> arrBytes, long lngVar, int bytNbbits)
	{
		if (bytNbbits>4) return;
		for(int i=0;i<=8*(bytNbbits-1);i+=8)
		{
			byte bytVal = (byte)((lngVar >> i) & 0xFF);
			arrBytes.add(bytVal);
		}
	}
	
	public static final void addStringToArray(ArrayList<Byte> arrBytes, String strToAdd, boolean isNullTerminated)
	{
		byte[] arrChars = strToAdd.getBytes();
		for(int i=0;i<arrChars.length;i++)
			arrBytes.add(new Byte(arrChars[i]));
		
		if (isNullTerminated) arrBytes.add(new Byte((byte)0));
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// Create Samples 
		CreateSamples();
		
		// Dummy YM file
		//CreateDummyYM();
	}
	
	public static void CreateSamples()
	{
		// Load YM2 digidrums
		System.out.println("+ Extracting Samples from YM2 samples");		
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
			//for (int i=0;i<sampleRedirect.length;i++) sampleRedirect[i] = -1;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Create digidrums files
		System.out.println("+ Create digidrums files");
		for(int i=0;i<sampleAdress.length;i++)
		{	
			// log sign change in sample
			int sign = 1;
			int indsign = 0;
			
			// Convert 8bits samples to 4bits samples
			byte[] arrSample = sampleAdress[i];
			//if (sampleFreq[i] <= 0) continue;
			
			// Save to new file
			try {
				FileOutputStream file_output = new FileOutputStream("Sample"+i+".bin");
				FileOutputStream file_output_wav = new FileOutputStream("Sample"+i+".wav");
				FileWriter file_output_log = new FileWriter("Sample"+i+".log");

				// Calculate Length of Sample in 15khz mode 
				double dbStep = CPC_SAMPLE_FREQUENCY / 6083;/*sampleFreq[i];*/
				int cpcSampleLength = (int)(((double)arrSample.length) * dbStep + 0.5d);
				
				// Disable output on all channels
				file_output.write((byte)(0x7<<3) + 0x7); 	// Disable oscillator on Channel
				file_output.write((byte)0x07);				// Set Register 7 (Mixer)
				
				// Wav Header				
				ArrayList<Byte> arrWavHeader = new ArrayList<Byte>();
				
				// Build header 
				addStringToArray(arrWavHeader, "RIFF", false);
				addLEvar(arrWavHeader, arrSample.length + 44 - 8, 4);
				addStringToArray(arrWavHeader, "WAVE", false);
				
				// Format Chunk
				addStringToArray(arrWavHeader, "fmt ", false);
				addLEvar(arrWavHeader, 16, 4);
				addLEvar(arrWavHeader, 1, 2);
				addLEvar(arrWavHeader, 1, 2);
				addLEvar(arrWavHeader, (long)6083/*sampleFreq[i]*/, 4);
				addLEvar(arrWavHeader, (long)6083/*sampleFreq[i]*/, 4);
				addLEvar(arrWavHeader, 1, 2);
				addLEvar(arrWavHeader, 8, 2);
				
				// Data Chunk 
				addStringToArray(arrWavHeader, "data", false);
				addLEvar(arrWavHeader, arrSample.length, 4);
				
				for (Iterator iter = arrWavHeader.iterator();iter.hasNext();)
					file_output_wav.write(((Byte)iter.next()).byteValue());
				
				file_output_wav.write(arrSample);
				file_output_wav.close();
				
				// index in sample
				int k=0;
				for(int j=0;j<cpcSampleLength;j++)
				{
					// index in sample
					k = (int)(((double)j)/dbStep);
					
					int sval = arrSample[k] & 0xFF;	// get unsigned value (0 - 255)
					byte dval = arr8to4bits[sval];
					file_output.write((byte)dval); 	// sample value on C
					file_output.write((byte)0x0A);	// Set Register 10 (channel C)
					
					//file_output.write((byte)0); 	// NOP (~ 8013hz)
					//file_output.write((byte)0x40); 	// Command			
					
					// log sign
					if (sign*(sval-127)<0)
					{
						sign = -sign;
						file_output_log.write("change : " + (k-indsign) + "\n");
						indsign = k;
					}
				}

				// Stop DMA List
				file_output.write((byte)0x20); 	// Stop
				file_output.write((byte)0x40); 	// Command					

				
				// Data 
				//file_output.write(arrSample);		
				file_output.close();
				file_output_log.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("+ Digidrums files created successfully");
	}
	
	public static void CreateDummyYM()
	{
		System.out.println("+ Create Dummy YM file");

		// periods
		long p0 = (YMC_Tools.YM_CPC_FREQUENCY/16)/50;
		//long p0 = 0;
		//long p1 = (YMC_Tools.YM_CPC_FREQUENCY/16)/35;
		long p1 = 0;
		long p2 = (YMC_Tools.YM_CPC_FREQUENCY/256)/(440*2);	// 200hz (8 periods, 4 visible)
		int intVBL = 50*10; // 10 secondes		
	
		// First Tick (Period changes)
		byte arrBytes1[] = new byte[]{
				/*(byte)(p0 & 0xFF)*/0,
				/*(byte)((p0 & 0xF00)>>8)*/(byte)0xF0, // SyncBuzzer on channel 3 (0xF0)
				0,
				0,
				0,
				0,
				(byte)(2<<5),			// TP
				0x3F,		// All off
				0,		
				0,
				0x10,		// Env = 14
				(byte)(p2 & 0xFF),			// Envelope
				(byte)((p2 & 0xFF00)>>8),	// Periode
				0xE, // E Pattern
				(byte)0xC0,		// TC
				0};

		
		// Other Tick
		byte arrBytes[] = new byte[]{
				/*(byte)(p0 & 0xFF)*/0,
				/*(byte)((p0 & 0xF00)>>8)*/(byte)0xF0, // SyncBuzzer on channel 3
				0,
				0,
				0,
				0,
				(byte)(2<<5),			// TP
				0x3F,		// All off
				0,		
				0,
				0x10,		// Env = 14
				(byte)(p2 & 0xFF),			// Envelope
				(byte)((p2 & 0xFF00)>>8),	// Periode
				(byte)0xFF, // E Pattern
				(byte)0xC0,		// TC
				0};
		
		ArrayList<Byte> arrYmBytes = new ArrayList<Byte>();
			
		// Header
		addStringToArray(arrYmBytes, "YM6!", false);
		addStringToArray(arrYmBytes, "LeOnArD!", false);
		addBEvar(arrYmBytes, intVBL, 4); // NbFrames
		addBEvar(arrYmBytes, 0, 4); // Attributes
		addBEvar(arrYmBytes, 0, 2); // Nb of Samples
		addBEvar(arrYmBytes, YMC_Tools.YM_CPC_FREQUENCY, 4); // 1000000hz
		addBEvar(arrYmBytes, 50, 2); // 50hz
		addBEvar(arrYmBytes, 0, 4); // Loop frame
		addBEvar(arrYmBytes, 0, 2); // Additional data
					
		// Song Name + Author Name + Comments
		addStringToArray(arrYmBytes, "Dummy", true);
		addStringToArray(arrYmBytes, "Dummy", true);
		addStringToArray(arrYmBytes, "Generated by YMCruncher (debug)", true);

		// First Tick
		for (int r=0;r<16;r++)
			arrYmBytes.add(arrBytes1[r]);
		
		for (int i=1;i<intVBL;i++)
			for (int r=0;r<16;r++)
				arrYmBytes.add(arrBytes[r]);
			
		// Footer
		addStringToArray(arrYmBytes, "End!", false);
		
		try {
			FileOutputStream file_output = new FileOutputStream("Dummy.ym");
		
			// Data 
			for(int i=0;i<arrYmBytes.size();i++)
			{
				byte bytVal = arrYmBytes.get(i).byteValue();
				file_output.write(bytVal);			
			}
			
			// Close file
			file_output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("+ Dummy YM file created successfully");
	}
	
	/**
	 * Set a variable in an Array of Bytes using Big Endian style
	 * @param arrBytes ArrayList<Byte> where the var should be written to
	 * @param lngVar long variable to insert into the array
	 * @param bytNbbits int Number of bytes to insert (cannot be>4) 
	 */
	protected static void addBEvar(ArrayList<Byte> arrBytes, long lngVar, int bytNbbytes)
	{
		if (bytNbbytes>4) return;
		for(int i=8*(bytNbbytes-1);i>=0;i-=8)
		{
			byte bytVal = (byte)((lngVar >> i) & 0xFF);
			arrBytes.add(bytVal);
		}
	}	
	
//	 Attribute values for YM5/6
	final private static byte A_STREAMINTERLEAVED = 1;
	final private static byte A_DRUMSIGNED = 2;
	final private static byte A_DRUM4BITS = 4;
	final private static byte A_TIMECONTROL = 8;
	final private static byte A_LOOPMODE = 16;	
}
