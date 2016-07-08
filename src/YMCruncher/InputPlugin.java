package YMCruncher;


import java.util.Vector;

/**
 * Abstract class to be extended by further Plugins such as those used
 * to get data from YM or VTX files
 * <p>
 * This class is an attempt to reach an architecture a bit more
 * plugin-friendly than it was ;)
 * @author F-key/RevivaL
 */

public abstract class InputPlugin {
	
	/**
	 * List of available Input plugins
	 */
	public static String[] INPUT_PLUGINS = {
			"VtxInputPlugin",	// Ok
			"YmInputPlugin",	// Ok
			"VgmInputPlugin",	// Ok
			"MymInputPlugin",	// Ok
			//"ModInputPlugin",	// Studying ...
			//"SndhInputPlugin"};	// Studying ...
			};
	
	/**
	 * Private data needed for further convertion
	 */
	public String strFileType = "unknown";
		
	/**
	 * Return available input plugins
	 */
	@SuppressWarnings("unchecked")
	public static Vector<Class> getInputPlugins()
	{
		Vector arrInputPlugins = new Vector<Class>();
		for(int i=0;i<INPUT_PLUGINS.length;i++)
		{
			Class inp;
			try {
				inp = (Class)Class.forName("Plugins." + INPUT_PLUGINS[i]);
				arrInputPlugins.add(inp);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return arrInputPlugins;
	}
			
	/**
	 * Return a Chiptune from the inputed <Byte>Vector
	 * @param arrRawChiptune <Byte>Vector of uncompressed chiptune data
	 * @param strExt String Extension of the file (i.e. "mod")
	 * @return Chiptune
	 */
	abstract protected Chiptune getPreProcessedChiptune(Vector arrRawChiptune, String strExt);
	
	/**
	 * Wrapper for the previous function
	 * @param arrRawChiptune Vector of uncompressed input chiptune data 
	 * @return Chiptune which hold the input chiptune
	 */
	public Chiptune getChiptune(Vector arrRawChiptune, boolean blnConvertFrequency, String strExt)
	{
		// get Chiptune
		Chiptune chiptune = this.getPreProcessedChiptune(arrRawChiptune, strExt);
		
		// Printing various Informations
		if (chiptune != null)
		{
			int intDuration = chiptune.getLength()/chiptune.getPlayRate();
			YMC_Tools.debug("+ File Informations");
			YMC_Tools.debug("  - Frames : " + chiptune.getLength());	
			YMC_Tools.debug("  - Time : " + intDuration + " secondes.");
		}
		
		return chiptune;
	}			
}
