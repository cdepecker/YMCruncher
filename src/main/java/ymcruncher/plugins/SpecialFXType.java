/**
 * 
 */
package ymcruncher.plugins;

/**
 * @author eu734
 *
 */
public enum SpecialFXType {
	OTHER,	
	ATARI_DIGIDRUM,
	ATARI_SIDVOICE,
	ATARI_SINSID,	
	ATARI_SYNCBUZZER,
	NO_FX;	
	
	public static SpecialFXType[] filterValues()
	{
		return new SpecialFXType[]
		        {ATARI_DIGIDRUM,
				ATARI_SIDVOICE,
				ATARI_SINSID,	
				ATARI_SYNCBUZZER,
				OTHER};
	}
}
