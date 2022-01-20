package ymcruncher.core;

// Files
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lha.LhaEntry;
import lha.LhaException;
import lha.LhaFile;
import ymcruncher.plugins.SpecialFXType;

/**
 * 
 * Process a chiptune file (such as YM or VTX) to get a small and easy  
 * to read chiptune usable for low memory old computers using AY-3-891x 
 * family sound processor.
 * <p>
 * Most of existing chiptune files are compressed using a "lha" or "zip/gzip" compliant 
 * compression method. This is not easy to use on low memory computers
 * because the compressed file needs to be loaded entirely in memory
 * in order to be decompressed and played.
 * <p>
 * The aim of this Class is to provide a way to decompress and play
 * such a file "on the fly", thus one will be able to play hours of
 * irritating music ;)
 * <p>
 * Please note that this version will produce a 50hz playable chiptune only,
 * @author F-key/RevivaL
 * @version  0.86
 */
public class YMC_Model extends Observable
{		
	// Static Consts
	final public static String strApplicationName = "YaMaha Cruncher - " + "Version 0.87";
	
	// Flags & Filters
	private byte bytReg7Filter = 0x0; // All on
	private boolean arrEnvFilter[] = new boolean[]{true, true, true};
	private boolean blnConvertTo50Hz = false;
	private boolean blnConvertFrequency = true;
	private boolean blnAdjustFrequency = false;
	private boolean blnNullPeriodDisableChannel = false;
	// 0: SID voice, 1:Digidrum, 2:Sinus SID (TAO), 3: Sync Buzzer (TAO)
	//private boolean arrSpecialFXFilter[] = new boolean[]{true, true, true, true};
	private HashMap<SpecialFXType, Boolean> arrSpecialFXFilter = new HashMap<SpecialFXType, Boolean>(); 
		
	// InputPlugins definition
	private Vector<Class> arrInputPlugins = InputPlugin.getInputPlugins();

	// OutputPlugins definition
	private Vector<OutputPlugin> arrOutputPlugins  = new Vector<OutputPlugin>();
	private OutputPlugin current_OutputPlugin = null;
	
	// Loaded Chiptunes (indexed by path)
	private Hashtable<String, Chiptune> hmChiptune = new Hashtable<String, Chiptune>();
	private int hmChiptuneSize = 0;						// Those are
	private int hmChiptuneindex = 0;					// only used by
	private String strFileNameOnlyWithoutExt = "none";	// the Observer
	
	/** 
	 * Standard Constructor.
	 */
	public YMC_Model()
		{
			// Welcome message
			YMC_Tools.debug(strApplicationName);		

			// Initialize Input Plugin Class
			if ((arrInputPlugins == null) || (arrInputPlugins.size() <= 0))
			{
				System.out.println("Error : No available Input main.Plugins detected");
				System.exit(-1);
			}

			// Initialize OutputPlugins
			Vector<Class> arrClassOutputPlugins  = OutputPlugin.getOutputPlugins();
			if ((arrClassOutputPlugins == null) || (arrClassOutputPlugins.size() <= 0))
			{
				System.out.println("Error : No available Output main.Plugins detected");
				System.exit(-1);
			}
			try {
				for(Class OPClass : arrClassOutputPlugins)
				{
					arrOutputPlugins.add((OutputPlugin) OPClass.newInstance());
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			
			// Initialize Current OutputPlugin
			current_OutputPlugin = (OutputPlugin) arrOutputPlugins.get(0);
			
			// Initialize SpecialFX filters
			arrSpecialFXFilter.put(SpecialFXType.ATARI_SIDVOICE, true);
			arrSpecialFXFilter.put(SpecialFXType.ATARI_DIGIDRUM, true);
			arrSpecialFXFilter.put(SpecialFXType.ATARI_SINSID, true);
			arrSpecialFXFilter.put(SpecialFXType.ATARI_SYNCBUZZER, true);
			arrSpecialFXFilter.put(SpecialFXType.OTHER, true);
		}
					
	/**
	 * Return list of available plugins for the GUI.
	 * @return arrOutputPlugins Vector of available Output main.Plugins
	 */
	Vector<OutputPlugin> getOutuptPlugins()
	{return arrOutputPlugins;}
	
	
	public OutputPlugin getCurrent_OutputPlugin() {
		return current_OutputPlugin;
	}

	public void setCurrent_OutputPlugin(OutputPlugin current_OutputPlugin) {
		this.current_OutputPlugin = current_OutputPlugin;
	}		
	
	void toggleFilterEnv(int c)
	{
		arrEnvFilter[c] = !arrEnvFilter[c];
	}
	
	void toggleSpecialFXFilter(SpecialFXType c)
	{
		//arrSpecialFXFilter[c] = !arrSpecialFXFilter[c];
		arrSpecialFXFilter.put(c, !arrSpecialFXFilter.get(c));
	}

	void toggleAdjustFrequeny()
	{ blnAdjustFrequency = !blnAdjustFrequency;}
	
	/**
	 * Toggle the Register 7 filter bit of the inputed voice.
	 * @param voice int between 0 and 5 inclusive
	 * (0 = voiceA, 1= voiceB, 2=voiceC, 3=noiseA, 4=noiseB, 5=noiseC)
	 */
	void toggleFilterVoice(int mix)
	{
		if ((mix>=0) && (mix<=5))
		bytReg7Filter ^= (1<<mix);
	}
			
	/**
	 * Get the uncompressed file from the InputStream
	 * @param in InputStream of the compressed file
	 * @return Uncompressed Byte Vector
	 */	
	private Vector<Byte> getVectorFromInputStream(InputStream in)
	{
		Vector<Byte> arrData = new Vector<Byte>();
		try {
			while(in.available()>0)
			{
				Byte byte_read = new Byte((byte)in.read());
				arrData.addElement(byte_read);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return arrData;
	}
	
	/**
	 * Return Data contained in the chiptune file
	 * Will return a vector caintaining sub arrays of registers data (uninterleaved)
	 * @param strSourceFile String indicating the chiptune source filename
	 */	
	public Chiptune getData(String strSourceFile)
	{
		Vector<Byte> arrData = null;
		
		if (getFileType(strSourceFile, 2, 5).equals("-lh5-"))
		{
			// LHA Compressed
			try {
				LhaFile inpLhaFile = new LhaFile(strSourceFile);
				Enumeration e = inpLhaFile.entries();
				LhaEntry entry = (LhaEntry) e.nextElement();
				InputStream in=inpLhaFile.getInputStream(entry);
				arrData = getVectorFromInputStream(in);
				in.close();
				inpLhaFile.close();
			} catch (LhaException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		else	// Might be Zip, Gzip or not compressed
		{
			try
			{
				ZipFile zipFile = new ZipFile(strSourceFile);				
				Enumeration e = zipFile.entries();
				ZipEntry entry = (ZipEntry)e.nextElement();
				InputStream in=zipFile.getInputStream(entry);
				arrData = getVectorFromInputStream(in);
				in.close();
				zipFile.close();
			}
			catch (IOException e1) 
			{			
				try
				{
					InputStream in = new GZIPInputStream(new FileInputStream(strSourceFile));
					arrData = getVectorFromInputStream(in);
					in.close();
				}
				catch (IOException e3) 
				{
					// Source file isn't compressed
					try
					{
						// Wrap the FileInputStream with a DataInputStream
						FileInputStream file_input = new FileInputStream (strSourceFile);
						DataInputStream data_in    = new DataInputStream (file_input );					
						InputStream in = data_in;					
						arrData = getVectorFromInputStream(in);
						in.close();
						file_input.close();
					}
					catch  (IOException e2)
					{
						System.out.println(e2);
						return null;
					}
				}
			}
		}
		
		// return if something has gone wrong
		if (arrData == null) return null;
		
		/**
		 * Log info into Chiptune
		 */
		YMC_Tools.clearLog();
		YMC_Tools.info("#############");
		YMC_Tools.info("# Input Log #");
		YMC_Tools.info("#############");				
		
		
		/**
		 * Get File extension in order to guess the chiptune format
		 */
		  String strREFileNameOnly = "^.*\\" + File.separatorChar;
		  String strFileNameOnly = strSourceFile.replaceFirst(strREFileNameOnly, "");
		  String strExt = strFileNameOnly.replaceFirst("^.*\\.", "");
		
		/**
		 * Parse the list of available InputPlugins and try to find 
		 * one which can handle the format of the inputed chiptune
		 */
		for (int i=0;i<arrInputPlugins.size();i++)
		{
			Class InputPluginClass = (Class)arrInputPlugins.elementAt(i);	
			InputPlugin inp;
			try {
				inp = (InputPlugin) InputPluginClass.newInstance();
				
				// Get array of Registers' values
				// Basically a Vector of 14 Registers for which the frequency has been converted
				Chiptune chiptune = inp.getChiptune(arrData, blnConvertFrequency, strExt); 
				if (chiptune != null)
				{				
					// Log file type
					YMC_Tools.info("+ File Type = " + inp.strFileType);			
					chiptune.setSbLog(YMC_Tools.getLog());
					
					return chiptune;					
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}			
		}
		
		// No suitable input plugin
		return null;
	}
	
	/**
	 * Return a String identifying the type of the file:
	 * @param strSourceFile String indicating the chiptune source filename
	 * @param intOffset String offset (where to get the type of this file)
	 * @param intNbBytes Number of characters to return
	 */
	private String getFileType(String strSourceFile, int intOffset, int intNbBytes)
	{
		byte[] arrType = new byte[intNbBytes];
		
		try
		{
			RandomAccessFile file_input = new RandomAccessFile (strSourceFile, "r");
			
			try
			{
				// read intNbBytes bytes
				file_input.seek(intOffset);
				file_input.read(arrType);
			}
			catch (EOFException eof)
			{
				System.out.println(eof);
				System.exit(-1);
			}
			file_input.close();
		}
		catch  (IOException e)
		{
			System.out.println(e);
			System.exit(-1);
		}
		
		String strHeader = new String(arrType);
		return strHeader;
	}
	
	/**
	 *
	 * Crunch the hasmap of chiptunes using outputPlugin and store results at destPath.
	 * @param destPath String which represents the path where converted chiptunes will be crunched
	 * @param outputPlugin Plugin used for output conversion
	 */
	public void crunchList(String destPath/*, OutputPlugin outputPlugin*/)
	{
		// Used only for display purposes
		hmChiptuneSize = hmChiptune.size();
		hmChiptuneindex = 0;
		
		for(Enumeration<String> e = hmChiptune.keys();e.hasMoreElements();)
		{
			String strPathChiptune = e.nextElement();
			
			// Debug
			System.out.println("> " + strPathChiptune);
			
			Chiptune chiptune = getChiptune(strPathChiptune);
			
			//	Get Destination path
  		  	String strREFileNameOnly = "^.*\\" + File.separatorChar;
  		  	String strFileNameOnly = strPathChiptune.replaceFirst(strREFileNameOnly, "");
			strFileNameOnlyWithoutExt = strFileNameOnly.replaceFirst("\\.[^\\.]*$", "");
			String destination = destPath + File.separatorChar + strFileNameOnlyWithoutExt;

			// Update GUI with name and progress bar
			setChanged();notifyObservers();
			
			// process Crunching ...
			if (chiptune.isLoaded())
			{	
				// Add the input Log
				YMC_Tools.setLog(chiptune.getSbLog());
				YMC_Tools.info("\n##############");
				YMC_Tools.info("# Output Log #");
				YMC_Tools.info("##############");								
				
				// Clone chiptune and process it
				// + Filter Mixer
				// + Clock Frequency change
				Chiptune cloned_chiptune = chiptune.copyclone();			
				cloned_chiptune.filter(blnConvertFrequency, blnAdjustFrequency, YMC_Tools.YM_CPC_FREQUENCY, bytReg7Filter, arrEnvFilter, arrSpecialFXFilter, blnNullPeriodDisableChannel);
				
				// Convert to 50hz ?
				if (blnConvertTo50Hz) cloned_chiptune.force50Hz();				
				
				// Set SongName & Author Info if not available
				String strSongName = (chiptune.getStrSongName() == null)?strFileNameOnlyWithoutExt:chiptune.getStrSongName();
				String strAuthorName = (chiptune.getStrAuthorName() == null)?"":chiptune.getStrAuthorName();				
				cloned_chiptune.setStrSongName(strSongName);
				cloned_chiptune.setStrAuthorName(strAuthorName);
				
				// Crunch Registers Vector
				System.out.print("(+) ");
				current_OutputPlugin.crunchAndStore(destination, cloned_chiptune);
				
				// Unload the Chiptune from the HashMap (This should free the java heap)
				unloadChiptune(strPathChiptune);
			}
			else
			{
				YMC_Tools.info("Cannot decode chiptune.");
				System.out.println("(E) Error processing file : " + strPathChiptune);
			}
			
			// Log File creation
			FileWriter log_output;
			try {
				log_output = new FileWriter(destination + ".txt", false);
				log_output.write(YMC_Tools.getLog().toString());
				log_output.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
			// increase index
			hmChiptuneindex++;
		}
		
		// reset current counter
		hmChiptuneindex = 0;
		setChanged();notifyObservers();		
	}	
		
	/**
	 * Convert to CPC Psg frequency 
	 * Usefull for ATARI to CPC Conversion
	 */
	void ToggleFrequencyConvert() {
		blnConvertFrequency = !blnConvertFrequency;		
	}

	// Force replay frequency to 50hz
	public void ToggleReplayFrequencyConvert() {
		blnConvertTo50Hz = !blnConvertTo50Hz;		
	}
	
	public void ToggleNullPeriodDisableChannel() {
		blnNullPeriodDisableChannel = !blnNullPeriodDisableChannel;		
	}
	
	/******************************************************************************************
	 * Manage the Hashmap of Chiptune, the chiptunes are only loaded when we first access them.
	 * If they are "expanded" in the TreeList, then they are loaded.
	 * If not, they will be loaded at "crunch" time.
	 *****************************************************************************************/	
	
	/**
	 * Get a chiptune from the list of files that are being crunched
	 * @param path String that identify the chiptune in the HasMap
	 * @return the Chiptune (loaded)
	 */
	public Chiptune getChiptune(String path) {
		Chiptune chiptune = hmChiptune.get(path);
		if (!chiptune.isLoaded())
		{
			Chiptune newChiptune = getData(path);
			
			// Replace existing entry with the new one
			if (newChiptune != null)
			{
				hmChiptune.remove(path);			
				hmChiptune.put(path, newChiptune);
				chiptune = newChiptune;
			}
		}
		return chiptune;
	}

	/**
	 * Add a chiptune from the list of files that are being crunched
	 * @param path  String that identify the chiptune in the HasMap
	 */
	public void addChiptune(String path) {
		hmChiptune.put(path, new Chiptune());
	}
	
	/**
	 * Unload a chiptune from the heap
	 * @param path String that identify the chiptune in the HasMap
	 */
	public void unloadChiptune(String path) {
		Chiptune chiptune = hmChiptune.get(path);
		if (chiptune.isLoaded())
		{
			hmChiptune.remove(path);			
			hmChiptune.put(path, new Chiptune());
		}
	}
	
	/**
	 * Remove a chiptune from the list of files that are being crunched
	 * @param path String that identify the chiptune in the HasMap
	 */
	public void delChiptune(String path) {
		hmChiptune.remove(path);
	}
	
	/*************************************************************************************
	 * The following part is used only to signal the Position of the Cruncher in the list
	 ************************************************************************************/
	/**
	 * @return Returns the hmChiptuneindex.
	 */
	public int getHmChiptuneindex() {
		return hmChiptuneindex;
	}

	/**
	 * @return Returns the hmChiptuneSize.
	 */
	public int getHmChiptuneSize() {
		return hmChiptuneSize;
	}

	/**
	 * @return Returns the strChiptuneName.
	 */
	public String getStrChiptuneName() {
		return strFileNameOnlyWithoutExt;
	}
}
