package ymcruncher.core;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;


/**
 * Abstract class to be extended by further main.Plugins such as the one used
 * to crunch chiptunes in AYC format
 * <p>
 * This class is an attempt to reach an architecture a bit more
 * plugin-friendly than it was ;)
 * @author F-key/RevivaL
 */

public abstract class OutputPlugin extends Observable{
	
	/* Note on volume output
	 * 
	 * It seems that the formula is something like volume = ((2^0.5)/2)^(15-a) V
	 * Where 'a' is from 0-15 (volume level).
	 * In fact the volume level is decreased by the half square root 2 each step.
	 * i.e.
	 * 	(a=15)  volume = ((2^0.5)/2)^(15-15) = ((2^0.5)/2)^0 = 1V
	 *  (a=14)  volume = ((2^0.5)/2)^(15-14) = ((2^0.5)/2)^1 = 0.707V
	 * */
	
	// Convert 4bits to 8bits sample (Extracted from STSound by Leonard)
	final protected byte[] arr4to8bits = { 0x0, 0x1, 0x2, 0x2, 0x4, 0x6, 0x9, 0xC, 0x11, 0x18, 0x23, 0x30, 0x48, 0x67, (byte)0xA5, (byte)0xFF };
	
	// Convert 8bits to 4bits sample (Given by gwem from the Atari scene)
	final protected byte[] arr8to4bits = { 0x0, 0x1, 0x2, 0x3, 0x3, 0x4, 0x5, 0x5, 0x6, 0x6, 0x6, 0x7, 0x7, 0x7, 0x7, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0x9, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xA, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xB, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xC, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xD, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xE, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF};
	
	/**
	 * List of available Input plugins
	 */
	private static String[] OUTPUT_PLUGINS = {
			"YmOutputPlugin",		// Ok
			"AycOutputPlugin",		// Ok but moving
			"WavOutputPlugin",		// Studying ...
			"FakeOutputPlugin"};	// Ok Fake one (log only)
	
	
	// Options registered for a Plugin	
	private Map<String, Boolean> hmBooleanOption = new TreeMap<String, Boolean>();
	private Map<String, OptionList> hmListOption = new TreeMap<String, OptionList>();
	private int hmOptionOrderCnt = 0;
	
	/**
	 * Return available input plugins
	 */
	@SuppressWarnings("unchecked")
	public static Vector<Class> getOutputPlugins()
	{
		Vector arrOutputPlugins = new Vector<Class>();
		for(int i=0;i<OUTPUT_PLUGINS.length;i++)
		{
			Class inp;
			try {
				inp = (Class)Class.forName("main.Plugins." + OUTPUT_PLUGINS[i]);
				arrOutputPlugins.add(inp);
			}catch (ClassNotFoundException e) {
				e.printStackTrace();
			} 
		}
		return arrOutputPlugins;
	}
	
	/**
	 * member data for Completion
	 */
	private int intCurrentCompletion = 0;
	
	/**
	 * Set the completion percentage of the overall compression process
	 * Should only be used once the compression process has started.
	 */
	protected void setCompletionRatio(int intComp)
	{
		if ((intComp != intCurrentCompletion) && (intComp>=0) && (intComp<=100))
		{
			intCurrentCompletion = intComp;
			setChanged();notifyObservers();
			
			// Progress information
			if (intCurrentCompletion>0) System.out.print("#");
		}
	}
	
	/**
	 * Get the completion percentage of the overall compression process
	 * Should only be used once the compression process has started.
	 * @return Completion percentage 
	 */
	public int getCompletionRatio()
	{
		return intCurrentCompletion;
	}
	
	/**
	 * Helper function used to convert a BitSet to a Byte
	 * @param bs BitSet to be converted
	 * @return Byte from BitSet
	 */
	protected Byte bsToByte(BitSet bs)
	{
		byte bytRet = 0;

		// Loop through bits
		for(byte b=7;b>0;b--)
		{
			bytRet |= bs.get(b)?1:0;
			bytRet <<= 1;
		}
		bytRet |= bs.get(0)?1:0;

		return new Byte(bytRet);
	}
	
	/**
	 * Set a variable in an Array of Bytes using Big Endian style
	 * @param arrBytes ArrayList<Byte> where the var should be written to
	 * @param lngVar long variable to insert into the array
	 * @param bytNbbits int Number of bytes to insert (cannot be>4) 
	 */
	protected void addBEvar(ArrayList<Byte> arrBytes, long lngVar, int bytNbbytes)
	{
		if (bytNbbytes>4) return;
		for(int i=8*(bytNbbytes-1);i>=0;i-=8)
		{
			byte bytVal = (byte)((lngVar >> i) & 0xFF);
			arrBytes.add(bytVal);
		}
	}	
	
	/**
	 * Set a variable in an Array of Bytes using little Endian style
	 * @param arrBytes ArrayList<Byte> where the var should be written to
	 * @param lngVar long variable to insert into the array
	 * @param bytNbbits int Number of bytes to insert (cannot be>4) 
	 */
	protected void addLEvar(ArrayList<Byte> arrBytes, long lngVar, int bytNbbits)
	{
		if (bytNbbits>4) return;
		for(int i=0;i<=8*(bytNbbits-1);i+=8)
		{
			byte bytVal = (byte)((lngVar >> i) & 0xFF);
			arrBytes.add(bytVal);
		}
	}
	
	/**
	 * Set a variable in an Array of Bytes using little Endian style
	 * @param arrBytes ArrayList<Byte> where the var should be written to
	 * @param lngVar long variable to insert into the array
	 * @param bytNbbits int Number of bytes to insert (cannot be>4) 
	 * @param offset int that indicate where to set the variable
	 */
	protected void setLEvar(ArrayList<Byte> arrBytes, long lngVar, int bytNbbits, int offset)
	{
		if (bytNbbits>4) return;
		for(int i=0;i<=8*(bytNbbits-1);i+=8)
		{
			byte bytVal = (byte)((lngVar >> i) & 0xFF);
			arrBytes.set(offset++, bytVal);
		}
	}			
	
	/**
	 * Set a variable in an Array of Bytes using little Endian style
	 * TODO explain what it does
	 * @param ?
	 * @param lngVar long variable to insert into the array
	 * @param bytNbbits int Number of bytes to insert (cannot be>4) 
	 */
	protected void setLEvar(byte arrBytes[], int index, long lngVar, int bytNbbits)
	{
		if (bytNbbits>4) return;
		for(int i=0;i<=8*(bytNbbits-1);i+=8)
		{
			byte bytVal = (byte)((lngVar >> i) & 0xFF);
			arrBytes[index++] = bytVal;
		}
	}
	
	/**
	 * Set a variable in an Array of Bytes using Big Endian style
	 * @param arrBytes ArrayList<Byte> where the var should be written to
	 * @param strToAdd String Message to add in the array
	 * @param isNullTerminated boolean indicating wether or not the String must be NT 
	 */
	protected void addStringToArray(ArrayList<Byte> arrBytes, String strToAdd, boolean isNullTerminated)
	{
		byte[] arrChars = strToAdd.getBytes();
		for(int i=0;i<arrChars.length;i++)
			arrBytes.add(new Byte(arrChars[i]));
		
		if (isNullTerminated) arrBytes.add(new Byte((byte)0));
		
	}	
	
	/**
	 * Abstract function that should return the String that will be displayed in the Menu
	 * @return String
	 */
	public abstract String getMenuLabel();
		
	/**
	 * Abstract class that should return the crunched chiptunes
	 * This should include the header of the file so that the only action needed
	 * by the Core is to persist data in a file
	 * @param arrPSGValues PSG values that will be crunched 
	 * @param intlength int Length of the chiptune (number of frames)
	 * @return ArrayList<Byte> which will be persisted
	 */
	public abstract ArrayList<Byte> doCrunch(String strDestFile, Chiptune chiptune);
	
	/**
	 * Should return the Plugin extension (i.e. "ayc")
	 * @return String fileextension
	 */
	public abstract String getExtension();
	
	/**
	 * Wrapper that will call the doCrunch function and store the crunched dqtq 
	 * destinqtion file
	 * @param destination String which will be used as the file location
	 * @param arrPSGValues PSG values that will be crunched 
	 * @param intlength int Length of the chiptune (number of frames)
	 */
	public void crunchAndStore(String destination, Chiptune chiptune)
	{	
		String strDestFile = destination + "." + getExtension();
		
		// Data 
		ArrayList<Byte> arrCrunchedData = doCrunch(strDestFile, chiptune);
		
		if (arrCrunchedData != null)
		{
			try {
				FileOutputStream file_output = new FileOutputStream(strDestFile);
				
				// Copy data in file
				for(int i=0;i<arrCrunchedData.size();i++)
				{
					byte bytVal = arrCrunchedData.get(i).byteValue();
					file_output.write(bytVal);			
				}
				
				// Close file
				file_output.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Print filename in the console
		System.out.println(" " + strDestFile);
		
		// reset Completion at 0
		setCompletionRatio(0);

	}

	
	/******************************************************************************************
	 * Manage the Hashmap of Options
	 *****************************************************************************************/	
	
	public boolean blnHasOptions()
	{
		return 	blnHasBooleanOptions()
				|| blnHasListOptions();
	}
	
	//
	// Booleans
	//
	public boolean blnHasBooleanOptions()
	{
		return 	!hmBooleanOption.isEmpty();
	}
	
	public Set getBooleanOptionList() {
		return hmBooleanOption.entrySet();
	}
	
	public Boolean getBooleanOption(String key) {
		Boolean b = hmBooleanOption.get(key);
		return (b!=null)?b:false;
	}

	public void setBooleanOption(String key, Boolean value) {
		hmBooleanOption.put(key, value);
	}
	
	//
	// List
	//
	public boolean blnHasListOptions()
	{
		return 	!hmListOption.isEmpty();
	}
	
	class OptionList
	{
		private String label = null; 
		private Object aList[] = null;
		private int selected = 0;
		private boolean radio = false;
		
		public OptionList(String label, Object paList[], int pselected, boolean radio)
		{
			this.label = label;
			this.aList = paList;
			this.selected = pselected;
			this.radio = radio;
		}
	}
	
	public Set getListOptionList() {
		return hmListOption.entrySet();
	}	
	
	public void setListOption(String key, Object aList[], int selected, boolean radio) {
		hmListOption.put(key, new OptionList(key, aList, selected, radio));
		hmOptionOrderCnt++;
	}
	
	public void setListOption(String key, Object aList[], int selected) {
		this.setListOption(key, aList, selected, false);
	}
	
	public void setListOptionIndex(String key, int selected) {
		OptionList ol = hmListOption.get(key);
		ol.selected = selected;
	}
	
	public int getListOptionIndex(String key) {
		OptionList ol = (OptionList) hmListOption.get(key);
		return ol.selected;
	}
	
	public Object getListOptionSelected(String key) {
		OptionList ol = (OptionList) hmListOption.get(key);
		return ol.aList[ol.selected];
	}

	public String[] getListOptionArray(String key) {
		OptionList ol = (OptionList) hmListOption.get(key);
		if (ol.aList == null) return null;
		String arrList[] = new String[ol.aList.length];
		for (int i=0;i<ol.aList.length;i++)
			arrList[i] = ol.aList[i].toString();
		return arrList;
	}

	public boolean isListOptionRadioType(String key) {
		OptionList ol = (OptionList) hmListOption.get(key);
		return ol.radio;
	}
}