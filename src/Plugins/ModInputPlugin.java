package Plugins;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import sun.audio.AudioStream;
import YMCruncher.Chiptune;
import YMCruncher.Frame;
import YMCruncher.InputPlugin;
import YMCruncher.YMC_Tools;

class ModTrackInfo
{	
  byte[]     samples;
  int        position;
  int        length;
  int        repeat;
  int        replen;
  int          volume;
  int          error;
  int        pitch;
  int        old_position;

//  ModInstrument   inst;
  int         start_period;
  int         period;
  int        step;
  /*u*/int         effect;
  int        portto;
  /*u*/int         vibpos;
  /*u*/int         trempos;
  int        oldsampofs;
  int         arp[/*3*/];
  int        arpindex;

  int        oldperiod;
  int        vol_slide;
  int        port_inc;
  int        port_up;
  int        port_down;
  int        vib_rate;
  int        vib_depth;
  int        trem_rate;
  int        trem_depth;
  /*u*/byte         retrig;

  int        finetune_rate;
  int        period_low_limit;
  int        period_high_limit;

  ModTrackInfo()
  {
    oldperiod = 1;
    arp = new int[3];
  }
}

public class ModInputPlugin  extends InputPlugin
{
  static final String VERSION = "0.9";
  static final String COPYRIGHT = "Copyright (c) 1997 HAMCO Software. All Rights Reserved.";

  static final int EFF_VOL_SLIDE = 0x01;
  static final int EFF_PORT_DOWN = 0x02;
  static final int EFF_PORT_UP   = 0x04;
  static final int EFF_VIBRATO   = 0x08;
  static final int EFF_ARPEGGIO  = 0x10;
  static final int EFF_PORT_TO   = 0x20;
  static final int EFF_TREMOLO   = 0x40;
  static final int EFF_RETRIG    = 0x80;

  static final int MIX_BUF_SIZE = 2048;
  static final int DEF_TEMPO_NTSC = 6;
  static final int DEF_TEMPO_PAL  = 6;
  static final int DEF_BPM_NTSC = 125;
  static final int DEF_BPM_PAL = 145;
  static final int MIDCRATE = 8448;

  static final int MAX_SAMPLES = 100;
  static final int MAX_TRACKS = 32;

  static final int S3M_MAGIC1 = 0x101A;
  static final int S3M_MAGIC2 = Mod.FOURCC("SCRM");
  static final int S3M_INSTR2 = Mod.FOURCC("SCRS");


static final int normal_vol_adj[] =
{
   0,   1,   2,   3,   4,   5,   6,   7,
   8,   9,  10,  11,  12,  13,  14,  15,
  16,  17,  18,  19,  20,  21,  22,  23,
  24,  25,  26,  27,  28,  29,  30,  31,
  32,  33,  34,  35,  36,  37,  38,  39,
  40,  41,  42,  43,  44,  45,  46,  47,
  48,  49,  50,  51,  52,  53,  54,  55,
  56,  57,  58,  59,  60,  61,  62,  63,
  63
};

static final int loud_vol_adj[] =
{
   0,   0,   1,   2,   2,   3,   3,   4,
   5,   6,   7,   8,   9,  10,  12,  14,
  16,  18,  20,  22,  24,  26,  28,  30,
  32,  34,  36,  38,  40,  42,  44,  46,
  47,  48,  49,  50,  51,  52,  53,  53,
  54,  55,  55,  56,  56,  57,  57,  58,
  58,  59,  59,  60,  60,  61,  61,  61,
  62,  62,  62,  63,  63,  63,  63,  63,
  63
};

static final int sintable[] =
{
   0,25,50,74,98,120,142,162,180,197,212,225,
   236,244,250,254,255,254,250,244,236,225,
   212,197,180,162,142,120,98,74,50,25
};

static final int period_set[] =
  {
      0x06B0,0x0650,0x05F5,0x05A0,0x054F,0x0503,0x04BB,0x0477,0x0436,0x03FA,0x03C1,0x038B  ,
      0x0358,0x0328,0x02FB,0x02D0,0x02A7,0x0281,0x025D,0x023B,0x021B,0x01FD,0x01E0,0x01C5  ,
      0x01AC,0x0194,0x017D,0x0168,0x0154,0x0141,0x012F,0x011E,0x010E,0x00FE,0x00F0,0x00E3  ,
      0x00D6,0x00CA,0x00BF,0x00B4,0x00AA,0x00A0,0x0097,0x008F,0x0087,0x007F,0x0078,0x0071  ,
      0x006B,0x0065,0x005F,0x005A,0x0055,0x0050,0x004C,0x0047,0x0043,0x0040,0x003C,0x0039  ,
      0x0035,0x0032,0x0030,0x002D,0x002A,0x0028,0x0026,0x0024,0x0022,0x0020,0x001E,0x001C  ,
      0x001B,0x0019,0x0018,0x0016,0x0015,0x0014,0x0013,0x0012,0x0011,0x0010,0x000F,0x000E
  };

static final int period_set_step[] =
{
    0x0680,0x0622,0x05CA,0x0577,0x0529,0x04DF,0x0499,0x0456,0x0418,0x03DD,0x03A6,0x0371,
    0x0340,0x0311,0x02E5,0x02BB,0x0294,0x026F,0x024C,0x022B,0x020C,0x01EE,0x01D2,0x01B8,
    0x01A0,0x0188,0x0172,0x015E,0x014A,0x0138,0x0126,0x0116,0x0106,0x00F7,0x00E9,0x00DC,
    0x00D0,0x00C4,0x00B9,0x00AF,0x00A5,0x009B,0x0093,0x008B,0x0083,0x007B,0x0074,0x006E,
    0x0068,0x0062,0x005C,0x0057,0x0052,0x004E,0x0049,0x0045,0x0041,0x003E,0x003A,0x0037,
    0x0033,0x0031,0x002E,0x002B,0x0029,0x0027,0x0025,0x0023,0x0021,0x001F,0x001D,0x001B,
    0x001A,0x0018,0x0017,0x0015,0x0014,0x0013,0x0012,0x0011,0x0010,0x000F,0x000E,0x000E
};

    // options
    int def_tempo, def_bpm;

    boolean use_pal = false;
    
    Thread playThread;
    OutputStream output;

    byte vol_table[];
    int[] vol_adj;
    int vol_shift;

    Mod                  mod;
    int                   order_pos;
    int                   tempo;
    int                   tempo_wait;
    int                   bpm;
    int                   row;
    int                   break_row;
    int                  bpm_samples;
    int pattofs;
    byte[] patt;
    int                 numtracks;
    ModTrackInfo[]       tracks;
    int                  mixspeed;
    boolean              mod_done;

    public boolean bit16;
    public int samplingrate = 8013; /* 8012.821 ? */
    public int oversample = 1;
    public int audiobuflen = 32768;
    public int gain = 256;
    public QueuedInputStream audioqueue;
    public boolean loop = false;

    public void outputTo(OutputStream out) throws IOException
    {
        output = out;
        if (bit16)
            play_mod16(false);
        //else
        //    play_mod(false);
    }

    static final byte[] sunfmt = {
        0x2E,0x73,0x6E,0x64,0x00,0x00,0x00,0x18,
        0x7f,0x7f,0x7f,0x7f,0x00,0x00,0x00,0x01,
        0x00,0x00,0x1F,0x4C/*or 40?*/,0x00,0x00,0x00,0x01,
        0x00,0x00,0x00,0x00,

    };

    void writeSunAudioHeader()
    {
        audioqueue.write(sunfmt, 0, sunfmt.length);
    }

    final void make_vol_table8()
    {
         int j;
         vol_table = new byte[16640];
         for (j=0;j<16640;j++)
            vol_table[j] = (byte)( (vol_adj[j >> 8] * (int)((byte)j) ) >> (8+vol_shift) );
    }

/*
    final void make_vol_table16()
    {
         int j;
         vol_table16 = new int[16640];
         for (j=0;j<16640;j++)
            vol_table16[j] = (vol_adj[j >> 8] * (byte)j) >> vol_shift;
    }
*/

    final void beattrack(ModTrackInfo track)
    {

      /* Quick FIX */
      if (track.period_low_limit==0)
         track.period_low_limit=1;

      if ((track.effect & EFF_VOL_SLIDE) != 0)
      {
        track.volume += track.vol_slide;
        if (track.volume < 0) track.volume = 0;
        if (track.volume > 64) track.volume = 64;
      }


      if ((track.effect & EFF_PORT_DOWN) != 0)
      {
        /* Portamento Down */
        if ((track.period += track.port_down) > track.period_high_limit)
          track.period = track.period_high_limit;
        track.pitch = track.finetune_rate / track.period;
      }

      if ((track.effect & EFF_PORT_UP) != 0)
      {
        /* Portamento Up */
        if ((track.period -= track.port_up) < track.period_low_limit)
        {
          if (mod.s3m)
             track.period = track.period_high_limit;
             else
             track.period = track.period_low_limit;
        }
        track.pitch = track.finetune_rate / track.period;
      }

      if ((track.effect & EFF_PORT_TO) != 0)
      {
        if (track.portto < track.period)
        {
          if ((track.period += track.port_inc) > track.portto)
            track.period = track.portto;
        }
        else if (track.portto > track.period)
        {
          if ((track.period -= track.port_inc) < track.portto)
            track.period = track.portto;
        }
        track.pitch = track.finetune_rate / track.period;
/*        System.out.println("effect " + track.effect +
            ", period " + track.period +
            ", portto " + track.portto +
            ", portinc " + track.port_inc +
            ", pitch " + track.pitch);*/
      }


      if ((track.effect & EFF_VIBRATO) != 0)
      {
        int playing_period;
        /* Vibrato */
        track.vibpos += (track.vib_rate) << 2;
        playing_period =
          (sintable[(track.vibpos >> 2) & 0x1F] *
             (track.vib_depth)) >> 7;
        if ((track.vibpos & 0x80) != 0)
          playing_period = -playing_period;
        playing_period += track.period;
        if (playing_period < track.period_low_limit)
          playing_period = track.period_low_limit;
        if (playing_period > track.period_high_limit)
          playing_period = track.period_high_limit;
        track.pitch = track.finetune_rate / playing_period;
      }
      if ((track.effect & EFF_ARPEGGIO) != 0)
      {
        /* Arpeggio */
        track.pitch = track.finetune_rate / track.arp[track.arpindex];
        track.arpindex++;
        if (track.arpindex >= 3)
          track.arpindex = 0;
      }
    /* if (track.effect & EFF_TREMOLO)
      {
        track.trempos += (track.trem_rate) >> 2;
        t =  ((uint16)sintable[(track.trempos >> 2) & 0x1F] *
             (track.trem_depth)) >> 7;
        if (track.trempos & 0x80) t = -t;
        t = track.volume;
        if (t < 0) t = 0;
        if (t > 63) t = 63;
        track.volume = t;
      }
      if (track.effect & EFF_RETRIG)
      {
        if (tempo_wait == track.retrig)
          track.position = 0;
        track.note_hit = 1;

      } */
    }

    final int get_track(ModTrackInfo track, byte[] pattern, int patternpos, int voice, Frame frame)
    {
        int i;
        int sample = pattern[patternpos] & 0xF0;

        int period = ((pattern[patternpos++] & 0x0F) << 8);
        period |= pattern[patternpos++] & 0xFF;
        int effect = pattern[patternpos] & 0x0F;
        sample |= (pattern[patternpos++] & 0xF0) >> 4;
        int param = pattern[patternpos++];
        track.effect = 0;

      if (sample != 0)
      {
        sample--;
        ModInstrument inst = mod.insts[sample];
        track.volume = inst.volume;
        track.length = inst.sample_length;
        track.repeat = inst.repeat_point;
        track.replen = inst.repeat_length;
        track.finetune_rate = inst.finetune_rate;
        track.samples = inst.samples;
        track.period_low_limit = inst.period_low_limit;
        track.period_high_limit = inst.period_high_limit;
//        System.out.println(sample + " " + period + " " + effect + " " + param + " " + track.samples);
        
//      FIXME Set Drum
        if ((voice==0)||(voice==1))
        {
        	double sampleF = 7093789.2 / (period * 2);
			//double realF = sampleF / ((7093789.2/(2*254))/440);
       		//arrSampleLog.get(posLog)[voice-1] = new SampleInstance(sample, sampleF/*1396.91*/);
       		frame.setSI(voice&3, new SampleInstance(sample, sampleF/*1396.91*/));
        }
      }
      if (period != 0)
      {
        track.portto = period;
        if ((effect != 3) && (effect != 5))
        {
          track.start_period = track.period = period;
          track.pitch = track.finetune_rate / period;
          track.position = 0;
//          track.note_hit = 1;
        }
      }

      if ((effect != 0) || (param != 0))
      {
        System.out.println("effect " + effect + ", param " + param + ", flags " + track.effect + ", period " + track.period);
        switch (effect)
        {
          /* Set the three periodtable amounts for each */
          /* half note pitch for arpeggio */
          case 0:
                  for (i=12;i<48;i++)
                     if (track.period >= period_set[i])
                        break;
                  track.arp[0] = period_set[i];
                  track.arp[1] = period_set[i+(param & 0x0F)];
                  track.arp[2] = period_set[i+((param & 0xF0) >> 4)];
                  track.arpindex = 0;
         	      track.effect |= EFF_ARPEGGIO;
                  break;
          case 1:
                track.effect |= EFF_PORT_UP;
    	        if (param != 0)
                     track.port_up = param;
    	        break;
          case 2: track.effect |= EFF_PORT_DOWN;
    	      if (param != 0)
                     track.port_down = param;
    	      break;
          /* Sets up portamento to for new note */
          case 3: if (param != 0)
    	              track.port_inc = param & 0xff;
         	      track.effect |= EFF_PORT_TO;
                  break;
          /* Initialize vibrato effect */
          case 4: if ((param & 0x0F) != 0)
    	           track.vib_depth = param & 0x0F;
                  if ((param & 0xF0) != 0)
    	           track.vib_rate = (param & 0xF0) >> 4;
                  if (period != 0)
                       track.vibpos = 0;
        	      track.effect |= EFF_VIBRATO;
                  break;
          /* Choose and offset starting into the sample */
          case 9: if (param == 0)
                       param = track.oldsampofs;
                  track.oldsampofs = param;
                  track.position = (param & 0xff) << 8;
                  break;
          case 5:    track.effect |= EFF_PORT_TO;
          case 6:    if (effect == 6)
      	           track.effect |= EFF_VIBRATO;
          case 0x0A: track.vol_slide = ((param & 0xF0) >> 4) - (param & 0x0F);
    	         track.effect |= EFF_VOL_SLIDE;
    	         break;
          /* Jump to a different position */
          case 0x0B: if (!loop)
                        break;
           	         order_pos = param & 0xff;
                     row = 64;
                     break;
          /* Set volume of this track */
          case 0x0C: if (param > 64 || param < 0)
                        track.volume = 64;
                       else track.volume = param;
                     break;
          /* Jumps to specified pattern-position in next song position */
          case 0x0D: break_row = ((param & 0xF0) >> 4) * 10 + (param & 0x0F);
                     row = 64;
                     break;
          case 0x0E: i = param & 0xF0;
                     param &= 0x0F;
                     switch (i)
                     {
                       case 1: track.period += param;
                               if (track.period > track.period_high_limit)
    			      track.period = track.period_high_limit;
                               track.pitch = track.finetune_rate / track.period;
                               break;
                       case 2: track.period -= param;
                               if (track.period < track.period_low_limit)
    			      track.period = track.period_low_limit;
                               track.pitch = track.finetune_rate / track.period;
                               break;
                     }
                     break;
          /* Set song speed */
          case 0x0F: if (param != 0)
                     {
                       param &= 0xff;
                       if (param <= 32)
                       {
                         tempo = param;
                         tempo_wait = param;
                         break;
                       }
                       bpm = param;
                       bpm_samples = (samplingrate / ((103 * param) >> 8)) * oversample;
                                              /*  103 */
                     }
                     break;

        }
      }
      return patternpos;
    }

    private static final int ERROR_SHIFT = 12;
    private static final int ERROR_MASK  = (1<<ERROR_SHIFT)-1;
    // 12 semitones in an octave, each division is 1/128 semitone,
    // 12*128 = 1536 (???)
    private static final long ratediv = (428l * MIDCRATE * 1536) << ERROR_SHIFT;

    final void startplaying(boolean loud)
    {
        int i;
        vol_adj = loud ? loud_vol_adj : normal_vol_adj;
        mixspeed = samplingrate * oversample;
/*
      uint16 i;
      track_info_ptr track;
      uint32 pitch_const;
      int16 j;
      int8 *vol_adj =
*/
      order_pos = 0;
      tempo_wait = tempo = def_tempo;
      bpm = def_bpm;
      row = 64;
      break_row = 0;
      bpm_samples = (samplingrate / ((24*bpm)/60)) * oversample;
                            /*   24*DEF_BPM  */
      numtracks = mod.numtracks;
      tracks = new ModTrackInfo[numtracks];
      for (i=0; i<tracks.length; i++)
          tracks[i] = new ModTrackInfo();

      if (mod.s3m)
      {
        for (i=0;i<mod.insts.length;i++)
        {
            ModInstrument inst = mod.insts[i];
            inst.finetune_rate = (int)
              ((428l * inst.finetune_value) << 8) / mixspeed;
            inst.period_low_limit = 0xE;
            inst.period_high_limit = 0x6B0;
        }
      } else
      {
        for (i=0;i<mod.insts.length;i++)
        {
            ModInstrument inst = mod.insts[i];
//            inst.finetune_rate = ((428 * MIDCRATE) << 8) / mixspeed;
            inst.finetune_rate = (int)(ratediv / (mixspeed * (1536 - inst.finetune_value)));
//            System.out.println(inst.finetune_rate + " " + inst.finetune_value);
            inst.period_low_limit = 113;
            inst.period_high_limit = 856;
        }
      }

      if (numtracks > 8)
        vol_shift = 2;
      else if (numtracks > 4)
        vol_shift = 1;
      else
        vol_shift = 0;

      if (!bit16)
          make_vol_table8();
/*      else
          make_vol_table16();*/

    }

    final void mixtrack_8_mono(ModTrackInfo track, byte[] buffer,
        int bufferpos, int buflen)
    {
        byte[] samples;
        int samplepos;
        int endtr;
        int volume, lopitch, hipitch, error;

        samples = track.samples;
        samplepos = track.position;
        volume = (track.volume*gain) << 16;
        error = track.error;
        lopitch = track.pitch & ERROR_MASK;
        hipitch = track.pitch >> ERROR_SHIFT;

          if (track.replen < 3)
          {
            endtr = track.length;
            int bufend = bufferpos + buflen;
            while ((samplepos < endtr) && (bufferpos < bufend))
            {
                buffer[bufferpos++] +=
                    vol_table[(samples[samplepos] & 0xff) | volume];
                samplepos += hipitch + ((error += lopitch) >> ERROR_SHIFT);
                error &= ERROR_MASK;
            }
            track.error = error;
            track.position = samplepos;
          }
          else
          {
            endtr = track.replen + track.repeat;
            while (buflen > 0)
            {
                if (samplepos >= endtr)
                    samplepos -= track.replen;
                buffer[bufferpos++] +=
                    vol_table[(samples[samplepos] & 0xff) | volume];
                samplepos += hipitch + ((error += lopitch) >> ERROR_SHIFT);
                error &= ERROR_MASK;
                buflen--;
            }
            track.error = error;
            track.position = samplepos;
          }
    }

    final void mixtrack_16_mono(ModTrackInfo track, int[] buffer,
        int bufferpos, int buflen)
    {
        byte[] samples;
        int samplepos;
        int endtr;
        int volume, lopitch, hipitch, error;

        samples = track.samples;
        samplepos = track.position;
        volume = (vol_adj[track.volume]*gain) >> (vol_shift+8);
        error = track.error;
        lopitch = track.pitch & ERROR_MASK;
        hipitch = track.pitch >> ERROR_SHIFT;

          if (track.replen < 3)
          {
            endtr = track.length;
            if (samplepos >= endtr)
                return;
            int bufend = bufferpos + buflen;
            if (track.pitch < (ERROR_MASK+1))    // then antialias
                while ((samplepos < endtr) && (bufferpos < bufend))
                {
                    buffer[bufferpos++] +=
                        ((samples[samplepos]*((ERROR_MASK+1)-error) +
                        samples[samplepos+1]*error)*volume) >> ERROR_SHIFT;
                    samplepos += hipitch + ((error += lopitch) >> ERROR_SHIFT);
                    error &= ERROR_MASK;
                }
            else
                while ((samplepos < endtr) && (bufferpos < bufend))
                {
                    buffer[bufferpos++] += samples[samplepos]*volume;
                    samplepos += hipitch + ((error += lopitch) >> ERROR_SHIFT);
                    error &= ERROR_MASK;
                }
            track.error = error;
            track.position = samplepos;
          }
          else
          {
            endtr = track.replen + track.repeat;
            if (track.pitch < (ERROR_MASK+1))
                while (buflen > 0)
                {
                    if (samplepos >= endtr)
                        samplepos -= track.replen;
                    buffer[bufferpos++] +=
                        ((samples[samplepos]*((ERROR_MASK+1)-error) +
                        samples[samplepos+1]*error)*volume) >> ERROR_SHIFT;
                    samplepos += hipitch + ((error += lopitch) >> ERROR_SHIFT);
                    error &= ERROR_MASK;
                    buflen--;
                }
            else
                while (buflen > 0)
                {
                    if (samplepos >= endtr)
                        samplepos -= track.replen;
                    buffer[bufferpos++] += samples[samplepos]*volume;
                    samplepos += hipitch + ((error += lopitch) >> ERROR_SHIFT);
                    error &= ERROR_MASK;
                    buflen--;
                }
            track.error = error;
            track.position = samplepos;
          }
    }

    final void updatetracks(Frame frame)
    {
        ModTrackInfo track;
        int count;

      tempo_wait = tempo;
      if (row >= 64)
      {
        if (order_pos >= mod.song_length_patterns)
        {
        	// FIXME
          /*order_pos = mod.song_repeat_patterns;
          if (order_pos >= mod.song_length_patterns)
          {*/
            order_pos = 0;
            mod_done = true;
          //}
        }
        row = break_row;
        break_row = 0;
        if (mod.positions[order_pos] == 0xFF)
        {
            if (!loop)
            {
                mod_done = true;
                return;
            } else {
                // restart mod
                order_pos = 0;
                row = 0;
            }
        }
        /*
        if (mod.s3m)
          pattofs = mod.patterns[mod.positions[order_pos]];
          else
          */
          patt = mod.patterns[mod.positions[order_pos]];
          pattofs = row * 4 * numtracks;
          order_pos++;
      }
      row++;
      /*
      if (mod.s3m)
        get_track_s3m(&note);
        else
        */
        {
           for (count=0;count<numtracks;count++)
           {
              pattofs = get_track(tracks[count], patt, pattofs, count, frame);
           }
        }
        /*
      track = tracks;
      for (count=0;count<mod.tracks;count++)
      {
        track->playing_period = track->period;
        track->playing_volume = track->volume;
        track++;
      }
      */

    }

//    final void play_mod(boolean loud) throws IOException
//    {
//        bit16 = false;
//        startplaying(loud);
//        mod_done = false;
//
//        int i,count;
//        byte buf[] = new byte[mixspeed]; /* ??? */
//        byte emptybuf[] = new byte[mixspeed];
//
//        while (!mod_done || !loop)
//        {
//            if ((--tempo_wait) > 0)
//            {
//    	        for (count=0;count<numtracks;count++)
//    	            beattrack(tracks[count]);
////                System.out.print(".");
//            }
//            else
//            {
//                updatetracks();
////                System.out.println(pattofs);
//            }
//
////            track = tracks;
////            for (c=&buf.rot_buf[bpm_samples];c > buf.rot_buf;*(--c) = 0x80);
//            System.arraycopy(emptybuf, 0, buf, 0, bpm_samples);
//            for (i=0;i<numtracks;i++)
//            {
//                mixtrack_8_mono(tracks[i], buf, 0, bpm_samples);
//            }
//            // write to a file if required
//            if (output != null)
//            {
//                for (i=0; i<bpm_samples; i++)
//                    buf[i] = (byte)(buf[i] ^ 0x80);
//                output.write(buf, 0, bpm_samples);
//            }
//            // write to audio
//            if (audioqueue != null)
//            {
//                for (i=0; i<bpm_samples; i++)
//                    buf[i] = UlawUtils.linear2ulaw(buf[i]);
//                audioqueue.write(buf, 0, bpm_samples);
//            }
//        }
//    }

    final Chiptune play_mod16(boolean loud) throws IOException
    {
        bit16 = true;
        startplaying(loud);
        mod_done = false;

        int i,count;
        int buf[] = new int[mixspeed];
        int emptybuf[] = new int[mixspeed];
        byte outbuf[] = new byte[mixspeed];

        // FIXME
        /**
		 * Parse the whole song
		 */
        
        // Create array of Frames
        Vector<Frame> arrFrame = new Vector<Frame>();
		
		//byte arrPSGValues[] = {0, 0, 0, 0, 0, 0, 0, (7<<3)+7, 0, 0, 0, 0, 0, (byte)0xFF};
		//Vector<Byte> arrRegistersValues = new Vector();
		//arrSampleLog = new Vector<SampleInstance[]>();
		
		// FIXME Just to test drums in Black Reign mod
        Sample[] arrDigiDrums = new Sample[31];
		for(int j=0;j<31;j++)
			arrDigiDrums[j] = new Sample(	"" + j,
											mod.insts[j].sample_length,
											(byte)8,
											true,
											(byte)mod.insts[j].finetune_value, 
											(byte)mod.insts[j].volume,
											mod.insts[j].repeat_point,
											mod.insts[j].repeat_length,
											mod.insts[j].samples);
		
        
		//FIXME
        while (!mod_done)
        {
        	// FIXME we create the Sample log here
        	//arrSampleLog.add(new SampleInstance[numtracks]);
        	
        	// Create a new Frame for the current tick
        	Frame frame = new Frame(
    				new double[]{0,0,0},	// Freq channels 0,1,2
    				0,						// Freq Noise
    				0,						// Freq Env
    				(byte)0x3F,				// Mixer (all off)
    				new byte[]{0,0,0},		// Vol channels 0,1,2
    				(byte)0xFF,				// Env 
    				new SampleInstance[numtracks] // SampleInstances
    			);

        	
            if ((--tempo_wait) > 0)
            { 
            	// Update effects on ticks
    	        for (count=0;count<numtracks;count++)
    	            beattrack(tracks[count]);
            }
            else
            {
                updatetracks(frame);
            }

            System.arraycopy(emptybuf, 0, buf, 0, bpm_samples);
            for (i=0;i<numtracks;i++)
            {
            	// Mix tracks to a mon buffer 
                mixtrack_16_mono(tracks[i], buf, 0, bpm_samples);
                
                // FIXME Brutal hack
                if (i<3)
                {
                	// Set frequency
                	int pitch = tracks[i].pitch==0?1:tracks[i].pitch;
                	double realF = pitch / ((7093789.2/(2*254))/440);
                	long intCPCPeriod = (long)(YMC_Tools.YM_CPC_FREQUENCY/(realF*8));
                	//frame.setDbFreq(i, intCPCPeriod);
                	
                	// Set Volume
					//arrPSGValues[8+i] = (byte)tracks[i].volume;
                	byte bytVal = (byte)tracks[i].volume;
                	bytVal = (bytVal == 0)?0:(byte)(((bytVal-1) >> 2) & 0xF);
                	//frame.setBytVol(i, bytVal);
                }
            }
            
            arrFrame.add(frame);
            
            // FIXME
            /*for(byte r=0;r<YMC_Tools.CPC_REGISTERS;r++)
			{
				byte bytVal = arrPSGValues[r];
				
				// Adjust Volume
				if ((r>=8) && (r<=10))
				{
					bytVal = (bytVal == 0)?0:(byte)(((bytVal-1) >> 2) & 0xF);
				}
				
				arrRegistersValues.add(bytVal);							
			}*/
            
            int real_samples = bpm_samples;
            // do oversampling if required
            if (oversample > 1)
            {
                int k=0;
                real_samples = bpm_samples/oversample;
                if (oversample == 2)
                {
                    for (i=0; i<real_samples; i++)
                    {
                        buf[i] = (buf[k] + buf[k+1]) >> 1;
                        k += 2;
                    }
                } else {
                    for (i=0; i<real_samples; i++)
                    {
                        int sum = buf[k++];
                        for (int j=1; j<oversample; j++)
                            sum += buf[k++];
                        buf[i] = sum/oversample;
                    }
                }
            }
            // write to a file if required
            if (output != null)
            {
                for (i=0; i<real_samples; i++)
                {
                    int x = buf[i];
                    if (x < -32768) x = -32768;
                    else if (x > 32767) x = 32767;
                    output.write(buf[i] & 0xff);
                    output.write(buf[i] >> 8);
                }
            }
            // write to audio
            if (audioqueue != null)
            {
                for (i=0; i<real_samples; i++)
                    outbuf[i] = UlawUtils.linear2ulawclip(buf[i]);
                // FIXME audioqueue.write(outbuf, 0, real_samples);
            }
            
            // FIXME
            //System.out.println(posLog);
            //posLog++;
        }
        return new Chiptune(null,
					null,
					arrFrame, 
					YMC_Tools.CPC_REPLAY_FREQUENCY,
					YMC_Tools.YM_CPC_FREQUENCY,
					false,
					0,
					arrDigiDrums);
    }

    protected Vector getPSGRegistersValues(Vector arrRawChiptune, String strExt)
    {return null;}
    
	@Override
	protected Chiptune getPreProcessedChiptune(Vector arrRawChiptune, String strExt) {
		
		if (strExt.toUpperCase().equals("MOD"))
		{
			AudioStream audiostream = null;
			
	        try 
	        {
				mod = new Mod(arrRawChiptune);
			
		        if (use_pal)
		        {
		            def_tempo = DEF_TEMPO_PAL;
		            def_bpm   = DEF_BPM_PAL;
		        }
		        else
		        {
		            def_tempo = DEF_TEMPO_NTSC;
		            def_bpm   = DEF_BPM_NTSC;
		        }
		        
		        // add gain
		        gain = 384;
		
		        audioqueue = new QueuedInputStream(audiobuflen);
		        writeSunAudioHeader();
		        audiostream = new AudioStream(audioqueue);
		        /* this is required by MS's JM (???) */
		        for (int i=0; i<800; i++)
		            audioqueue.write((byte)0);
		        //AudioPlayer.player.start(audiostream);
		        audioqueue.blockUntilFull = true;
		        return play_mod16(false);
		       	        
		        // Return if nothing
				//if (arrRegistersValues == null) return null;
				
				// Uninterleave data
				/*Vector<Byte> arrUninterleavedRawChiptune = new Vector<Byte>();
				int intVBL = arrRegistersValues.size()/YMC_Tools.CPC_REGISTERS;
				for(int reg=0;reg<YMC_Tools.CPC_REGISTERS;reg++)
				{
					for (int i=0;i<intVBL;i++)
					{
						arrUninterleavedRawChiptune.add(arrRegistersValues.elementAt(i*YMC_Tools.CPC_REGISTERS+reg));
					}
				}
				arrRegistersValues = arrUninterleavedRawChiptune;*/
				
				//return arrRegistersValues;
				/*return new Chiptune(null,
						null,
						null,		// FIXME !!! arrFrame here ! 
						YMC_Tools.CPC_REPLAY_FREQUENCY,
						YMC_Tools.YM_CPC_FREQUENCY,
						false,
						0,
						arrDigiDrums);*/
			}
	        catch (Exception e) 
	        {
	        	e.printStackTrace();
	        }
	        finally
	        {
	        	//AudioPlayer.player.stop(audiostream);
	        }
		}
	    
    	// Nothing to return yet
		return null;
	}

}
