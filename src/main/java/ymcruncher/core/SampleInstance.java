package ymcruncher.core;

/**
 * Sample Instance
 * An object of type SampleInstance represents basically a Sample identified by it's number (integer) played
 * at a given rate.
 *
 * @author FKey
 */
public class SampleInstance {
    // Static variables
    final private static int[] mfpPrediv = {0, 4, 10, 16, 50, 64, 100, 200};
    final private static long MFP_CLOCK = 2457600L;

    // Private data
    private int sample;
    private double rate = 0;

    // Special for atari samples (more accurate)
    private SpecialFXType type = SpecialFXType.OTHER;
    private int tc = 0; // timer_count
    private int tp = 0; // indice in the mfp_table
    private int origFxNb = 0; // registers pair ? (1,6,14 or 3,8,15)

    // ******************************************
    // Constructors
    // ******************************************
    public SampleInstance(int psample, double prate) {
        this.sample = psample;
        this.rate = prate;
    }

    // Atari SpecialFx
    public SampleInstance(SpecialFXType ptype, int psample, int ptp, int ptc, int origFxNb) {
        this.type = ptype;
        this.sample = psample;
        this.tp = ptp;
        this.tc = ptc;
        this.origFxNb = origFxNb;
    }

    // ******************************************
    // clone
    // ******************************************
    public SampleInstance copyclone() {
        SampleInstance si = new SampleInstance(sample, rate);

        // If It's an AtariFX, then copy atari's info
        if (isAtari()) {
            si.type = type;
            si.tp = tp;
            si.tc = tc;
            si.origFxNb = origFxNb;
        }

        return si;
    }

    public boolean isAtari() {
        return type != SpecialFXType.OTHER;
    }

    public double getRate() {
        if (isAtari() && (tc != 0))
            return MFP_CLOCK / ((long) tc * mfpPrediv[tp]);

        // Not an Atari Sample
        return rate;
    }

    public int getSample() {
        return sample;
    }

    public void setSample(int sample) {
        this.sample = sample;
    }

    public int getTc() {
        return tc;
    }

    public int getTp() {
        return tp;
    }

    public SpecialFXType getType() {
        return type;
    }

    public void setType(SpecialFXType type) {
        this.type = type;
    }

    /**
     * @return the origFxNb
     */
    public int getOrigFxNb() {
        return origFxNb;
    }
}