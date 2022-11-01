package ymcruncher.plugins.input;


import com.google.auto.service.AutoService;
import lha.LhaEntry;
import lha.LhaFile;
import lha.LhaInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ymcruncher.core.Chiptune;
import ymcruncher.core.InputPlugin;
import ymcruncher.core.Tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * This class handle the VTX chiptune format input to the core cruncher.
 * VTX is a format defined for speccy chiptunes and as the chip to generate sound
 * in a cpc is compatible, we should be able to convert nost of the VTX chiptunes to AYC format
 *
 * @author F-key/RevivaL
 */
@AutoService(InputPlugin.class)
public class VtxInputPlugin extends InputPlugin {

    // Logger
    private static final Logger LOGGER = LogManager.getLogger(VtxInputPlugin.class.getName());

    /**
     * This method should basically return an array of PSG registers' values
     * Let's call Rx the Register number x, then this function should return
     * an array of all the R0 values for all frames then all the R1 values for all frames
     * and so on ...
     *
     * @param arrRawChiptune ArrayList representing the uncompressed inputed chiptune
     * @return ArrayList that should contain the PSG registers' values.
     */
    protected ArrayList getPSGRegistersValues(ArrayList arrRawChiptune, String strExt) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean getPreProcessedChiptune(Chiptune chiptune, ArrayList arrRawChiptune, String strExt) {
        // Get type of file
        strFileType = Tools.getString(arrRawChiptune, 0, 4);
        if (strFileType.subSequence(0, 2).equals("ay")
                || strFileType.subSequence(0, 2).equals("ym")) {
            Short bytPlayingMode = Tools.getLEByte(arrRawChiptune, 2);
            Integer shtLoopVBL = Tools.getLEShort(arrRawChiptune, 3);
            Long intFrequency = Tools.getLEInt(arrRawChiptune, 5);
            Short bytPlayerfrequency = Tools.getLEByte(arrRawChiptune, 9);
            Integer shtCreationDate = Tools.getLEShort(arrRawChiptune, 10);
            Long intPackedDataSize = Tools.getLEInt(arrRawChiptune, 12);

            Tools.info("+ Playing Mode = 0x" + Long.toHexString(bytPlayingMode.longValue()).toUpperCase());
            Tools.info("+ Loop VBL = 0x" + Long.toHexString(shtLoopVBL.longValue()).toUpperCase());
            Tools.info("+ Chip Frequency = " + intFrequency + "Hz");
            Tools.info("+ Player Frequency = " + bytPlayerfrequency + "Hz");
            Tools.info("+ Creation Date = " + shtCreationDate);
            Tools.info("+ Size of Packed Data = 0x" + Long.toHexString(intPackedDataSize).toUpperCase());

            // Other Info
            int intOffset = 16;
            String strSongName = Tools.getNTString(arrRawChiptune, intOffset, false);
            String strAuthorName = Tools.getNTString(arrRawChiptune, intOffset += strSongName.length() + 1, false);
            String strEditor = Tools.getNTString(arrRawChiptune, intOffset += strAuthorName.length() + 1, false);
            String strSourceProgram = Tools.getNTString(arrRawChiptune, intOffset += strEditor.length() + 1, false);
            String strComments = Tools.getNTString(arrRawChiptune, intOffset += strSourceProgram.length() + 1, false);
            Tools.info("+ Song Name = " + strSongName);
            Tools.info("+ Author = " + strAuthorName);
            Tools.info("+ Source Program = " + strSourceProgram);
            Tools.info("+ Editor Name = " + strEditor);
            Tools.info("+ Comments = " + strComments);

            // only keep data
            intOffset += strComments.length() + 1;
            arrRawChiptune = new ArrayList(arrRawChiptune.subList(intOffset, arrRawChiptune.size()));

            byte[] arrBytesHeader = {
                    31,                                    // HeaderLength-2
                    0,                                    // Checksum
                    '-', 'l', 'h', '5', '-',                // Compression type
                    0, 0, 0, 0,                            // Compressed file size
                    0, 0, 0, 0,                            // UnCompressed file size
                    0, 0, 0, 0,                            // TimeStamp
                    0,                                    // File Attribute
                    0,                                    // Header Level
                    7 + 2,                                // FileName Length
                    'V', 'T', 'X', 'F', 'I', 'L', 'E', 0, 'S',    // FileName + 0 + FileType ("S" ?) -> "VTXFILE"
                    0, 0                                    // File CheckSum
            };

            // Set Compressed file Size
            arrBytesHeader[7] = (byte) (arrRawChiptune.size() & 0xFF);
            arrBytesHeader[8] = (byte) ((arrRawChiptune.size() & 0xFF00) >> 8);
            arrBytesHeader[9] = (byte) ((arrRawChiptune.size() & 0xFF0000) >> 16);
            arrBytesHeader[10] = (byte) ((arrRawChiptune.size() & 0xFF000000) >> 24);

            // Set UnCompressed file Size
            arrBytesHeader[11] = (byte) (intPackedDataSize.intValue() & 0xFF);
            arrBytesHeader[12] = (byte) ((intPackedDataSize.intValue() & 0xFF00) >> 8);
            arrBytesHeader[13] = (byte) ((intPackedDataSize.intValue() & 0xFF0000) >> 16);
            arrBytesHeader[14] = (byte) ((intPackedDataSize.intValue() & 0xFF000000) >> 24);

            byte[] arrBytes = new byte[arrRawChiptune.size() + arrBytesHeader.length + 1];
            for (byte b = 0; b < arrBytesHeader.length; b++) {
                arrBytes[b] = arrBytesHeader[b];
            }
            for (int i = 0; i < arrRawChiptune.size(); i++)
                arrBytes[i + arrBytesHeader.length] = (Byte) arrRawChiptune.get(i);
            arrBytes[arrBytes.length - 1] = 0;

            // the following info are stored direcctly in the array of bytes
            // Set File Checksum -
            // XXX - The File Checksum cannot be known ???

            // Set HeaderChecksum
            byte bytHeaderChecksum = 0;
            for (byte b = 2; b < arrBytesHeader.length; b++) {
                bytHeaderChecksum += arrBytes[b];
            }
            arrBytes[1] = bytHeaderChecksum;

            // We should decompress and process next data
            LhaInputStream lhaInp = new LhaInputStream(new ByteArrayInputStream(arrBytes));

            LhaEntry lhaCompressedEntry;
            try {
                lhaCompressedEntry = lhaInp.getNextEntry();
                // Hack of the lha library
                lhaCompressedEntry.setOffset(33);
                Tools.debug("+ Entry = " + lhaCompressedEntry);

                // Decompress YM3 data
                ArrayList arrData = new ArrayList();
                arrData.add((byte) 'Y');
                arrData.add((byte) 'M');
                arrData.add((byte) '3');
                arrData.add((byte) '!');

                // We need a file to decompress
                FileOutputStream tmpFile = new FileOutputStream("VTXFILE.LHA");
                tmpFile.write(arrBytes);
                tmpFile.close();

                File inpFile = new File("VTXFILE.LHA");
                LhaFile inpLhaFile = new LhaFile(inpFile);
                InputStream inputStreamEntry = inpLhaFile.getInputStream(lhaCompressedEntry);

                int intBytesRead = 0;
                while ((inputStreamEntry.available() > 0) && (intBytesRead++ < lhaCompressedEntry.getOriginalSize())) {
                    Byte byte_read = (byte) inputStreamEntry.read();
                    arrData.add(byte_read);
                }
                inputStreamEntry.close();
                inpLhaFile.close();
                inpFile.delete();

                // get uncompressed data from YM plugin
                YmInputPlugin ymplug = new YmInputPlugin();
                ArrayList arrRegistersValues = ymplug.getPSGRegistersValues(arrData, "YM");

                chiptune.setStrSongName(strSongName);
                chiptune.setStrAuthorName(strAuthorName);
                chiptune.setArrFrame(arrRegistersValues);
                chiptune.setPlayRate(bytPlayerfrequency);
                chiptune.setFrequency(intFrequency);

                return true;

            }  catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("An error occured while trying to read from file " + chiptune.getFile().getAbsolutePath());
            }
        }
        return false;
    }

    @Override
   public String getMenuLabel() {
        return "VTX Format";
    }
}
