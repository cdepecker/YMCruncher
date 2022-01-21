package ymcruncher.plugins;

import ymcruncher.core.YMC_Tools;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

class Mod {
    static final String voice_mk = "M.K.";
    static final String voice_mk2 = "M!K!";
    static final String voice_mk3 = "M&K!";
    static final String voice_flt4 = "FLT4";
    static final String voice_flt8 = "FLT8";
    static final String voice_28ch = "28CH";
    static final String voice_8chn = "8CHN";
    static final String voice_6chn = "6CHN";
    static final String voice_31_list[] = {
            voice_mk, voice_mk2, voice_mk3, voice_flt4,
            voice_flt8, voice_8chn, voice_6chn, voice_28ch
    };
    String name;
    int numtracks;
    int track_shift;
    int numpatterns;
    byte patterns[][];
    ModInstrument insts[];
    byte positions[/*256*/];
    int song_length_patterns;
    int song_repeat_patterns;
    //    int               bpm;
//    int               tempo;
    boolean s3m;

    Mod(ArrayList<Byte> arrRawChiptune) throws IOException {
        //DataInputStream in = new DataInputStream(instream);
        int i;
        int numinst = 15;
        numtracks = 4;
        // read mod header
        name = YMC_Tools.getNTString(arrRawChiptune, 0, false);
//        System.out.println(name);
        // get the int at offset 1080 to see if 15 or 31 mod
        String strSignature = YMC_Tools.getString(arrRawChiptune, 1080, 4);
        YMC_Tools.debug("+ ID : " + strSignature);

        // see if it matches any of our bytes
        for (i = 0; i < voice_31_list.length; i++) {
            // System.out.println(Integer.toString(voice_31_list[i],16));
            if (strSignature.equals(voice_31_list[i])) {
                numinst = 31;
                break;
            }
        }
        if (numinst == 31) {
            if (strSignature.equals(voice_8chn))
                numtracks = 8;
            else if (strSignature.equals(voice_6chn))
                numtracks = 6;
            else if (strSignature.equals(voice_28ch))
                numtracks = 28;
        }

        // read instruments
        int offset = 20;
        insts = new ModInstrument[numinst];
        for (i = 0; i < numinst; i++) {
            insts[i] = readInstrument(arrRawChiptune, offset);
            offset += 30;
        }
        // read sequence
        readSequence(arrRawChiptune, offset);
        offset += 1 + 1 + 128;
        // skip pattern header
        offset += 4;
        // read mod patterns
        readPatterns(arrRawChiptune, offset);
        offset += numtracks * 4 * 64 * numpatterns;
        // read the sample data
        try {
            for (i = 0; i < numinst; i++) {
//                System.out.println(i + " " + samples[i].sample_length);
                readSampleData(arrRawChiptune, offset, insts[i]);
                offset += insts[i].sample_length;
            }
        } catch (EOFException e) {
            System.out.println("Warning: EOF on MOD file");
        }
    }

    static final int FOURCC(String s) {
        return
                (s.charAt(3) & 0xff) |
                        ((s.charAt(2) & 0xff) << 8) |
                        ((s.charAt(1) & 0xff) << 16) |
                        ((s.charAt(0) & 0xff) << 24);
            /*
        int a = 0;
        for (int i=0; i<4; i++)
            a |= ((s.charAt(3-i) & 0xff) << (i*3));
        return a;
        */
    }

    static final String readText(DataInputStream in, int len)
            throws IOException {
        byte[] b = new byte[len];
        in.readFully(b, 0, len);
        for (int i = len - 1; i >= 0; i--) {
            if (b[i] != 0)
                return new String(b, 0, 0, i + 1);
        }
        return "";
    }

    static final int readu8(DataInputStream in) throws IOException {
        return (int) (in.readByte()) & 0xff;
    }

    static final int readu16(DataInputStream in) throws IOException {
        return (int) (in.readShort()) & 0xffff;
    }

    static ModInstrument readInstrument(ArrayList<Byte> arrRawChiptune, int offset) throws IOException {
        ModInstrument inst = new ModInstrument();
        inst.name = YMC_Tools.getNTString(arrRawChiptune, offset, false);
        offset += 22;
        inst.sample_length = YMC_Tools.getBEShort(arrRawChiptune, offset) << 1;
        offset += 2;
        inst.samples = new byte[inst.sample_length + 8];  // a little padding for interp.
        inst.finetune_value = (int) ((byte) ((YMC_Tools.getLEByte(arrRawChiptune, offset) & 0x0F) << 4));
        offset++;
        inst.volume = YMC_Tools.getLEByte(arrRawChiptune, offset) & 0x7F;
        offset++;
        inst.repeat_point = YMC_Tools.getBEShort(arrRawChiptune, offset) << 1;
        offset += 2;
        inst.repeat_length = YMC_Tools.getBEShort(arrRawChiptune, offset) << 1;
        offset += 2;
        if (inst.repeat_point > inst.sample_length)
            inst.repeat_point = inst.sample_length;
        if ((inst.repeat_point + inst.repeat_length) > inst.sample_length)
            inst.repeat_length = inst.sample_length - inst.repeat_point;
        return inst;
    }

    static void readSampleData(ArrayList<Byte> arrRawChiptune, int offset, ModInstrument inst) throws IOException {
        // Read fully
        for (int j = 0; j < inst.sample_length; j++)
            inst.samples[j] = YMC_Tools.getLEByte(arrRawChiptune, offset++).byteValue();

        if (inst.repeat_length > 3)
            System.arraycopy(inst.samples, inst.repeat_point,
                    inst.samples, inst.sample_length, 8);
    }

    void readSequence(ArrayList<Byte> arrRawChiptune, int offset) throws IOException {
        positions = new byte[128];
        song_length_patterns = YMC_Tools.getLEByte(arrRawChiptune, offset);
        offset++;
        song_repeat_patterns = YMC_Tools.getLEByte(arrRawChiptune, offset);
        offset++;

        // Read fully
        for (int i = 0; i < 128; i++) positions[i] = YMC_Tools.getLEByte(arrRawChiptune, offset++).byteValue();

        if (song_repeat_patterns > song_length_patterns)
            song_repeat_patterns = song_length_patterns;
        numpatterns = 0;
        for (int i = 0; i < positions.length; i++)
            if (positions[i] > numpatterns)
                numpatterns = positions[i];
        numpatterns++;
    }

    void readPatterns(ArrayList<Byte> arrRawChiptune, int offset) throws IOException {
        int patternsize = numtracks * 4 * 64;
        patterns = new byte[numpatterns][];
        for (int i = 0; i < numpatterns; i++) {
            patterns[i] = new byte[patternsize];

            // Read fully
            for (int j = 0; j < patternsize; j++)
                patterns[i][j] = YMC_Tools.getLEByte(arrRawChiptune, offset++).byteValue();
        }
    }

    public String getName() {
        return name;
    }

    public int getNumTracks() {
        return numtracks;
    }

    public int getNumPatterns() {
        return numpatterns;
    }

    public String toString() {
        return name + " (" + numtracks + " tracks, " +
                numpatterns + " patterns, " +
                insts.length + " samples)";
    }
}

