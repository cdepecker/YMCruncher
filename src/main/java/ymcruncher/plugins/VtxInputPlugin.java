package ymcruncher.plugins;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import ymcruncher.core.Chiptune;
import ymcruncher.core.InputPlugin;
import ymcruncher.core.YMC_Tools;

import lha.LhaEntry;
import lha.LhaException;
import lha.LhaFile;
import lha.LhaInputStream;

/**
 * This class handle the VTX chiptune format input to the core cruncher.
 * VTX is a format defined for speccy chiptunes and as the chip to generate sound
 * in a cpc is compatible, we should be able to convert nost of the VTX chiptunes to AYC format
 * @author F-key/RevivaL
 */
public class VtxInputPlugin extends InputPlugin {
	
	/**
	 * This method should basically return an array of PSG registers' values
	 * Let's call Rx the Register number x, then this function should return
	 * an array of all the R0 values for all frames then all the R1 values for all frames 
	 * and so on ...
	 * @param arrRawChiptune Vector representing the uncompressed inputed chiptune
	 * @return Vector that should contain the PSG registers' values.
	 */
	protected Vector getPSGRegistersValues(Vector arrRawChiptune, String strExt)
	{return null;}
	
	@SuppressWarnings("unchecked")
	protected Chiptune getPreProcessedChiptune(Vector arrRawChiptune, String strExt)
	{
		Vector arrRegistersValues = null;
		
		// Get type of file
		strFileType = YMC_Tools.getString(arrRawChiptune, 0, 4);
		if (strFileType.subSequence(0,2).equals("ay")
				|| strFileType.subSequence(0,2).equals("ym"))
		{
			Short bytPlayingMode = YMC_Tools.getLEByte(arrRawChiptune,2);
			Integer shtLoopVBL = YMC_Tools.getLEShort(arrRawChiptune,3);
			Long intFrequency = YMC_Tools.getLEInt(arrRawChiptune,5);
			Short bytPlayerfrequency = YMC_Tools.getLEByte(arrRawChiptune,9);
			Integer shtCreationDate = YMC_Tools.getLEShort(arrRawChiptune,10);
			Long intPackedDataSize = YMC_Tools.getLEInt(arrRawChiptune,12);
			
			YMC_Tools.info("+ Playing Mode = 0x" + Long.toHexString(bytPlayingMode.longValue()).toUpperCase());
			YMC_Tools.info("+ Loop VBL = 0x" + Long.toHexString(shtLoopVBL.longValue()).toUpperCase());
			YMC_Tools.info("+ Chip Frequency = " + intFrequency.longValue() + "Hz");
			YMC_Tools.info("+ Player Frequency = " + bytPlayerfrequency.shortValue() + "Hz");
			YMC_Tools.info("+ Creation Date = " + shtCreationDate.intValue());
			YMC_Tools.info("+ Size of Packed Data = 0x" + Long.toHexString(intPackedDataSize.longValue()).toUpperCase());
			
			// Other Info
			int intOffset = 16;
			String strSongName = YMC_Tools.getNTString(arrRawChiptune, intOffset, false);
			String strAuthorName = YMC_Tools.getNTString(arrRawChiptune, intOffset += strSongName.length()+1, false);
			String strEditor = YMC_Tools.getNTString(arrRawChiptune, intOffset += strAuthorName.length()+1, false);
			String strSourceProgram = YMC_Tools.getNTString(arrRawChiptune, intOffset += strEditor.length()+1, false);
			String strComments = YMC_Tools.getNTString(arrRawChiptune, intOffset += strSourceProgram.length()+1, false);
			YMC_Tools.info("+ Song Name = " + strSongName);
			YMC_Tools.info("+ Author = " + strAuthorName);
			YMC_Tools.info("+ Source Program = " + strSourceProgram);
			YMC_Tools.info("+ Editor Name = " + strEditor);
			YMC_Tools.info("+ Comments = " + strComments);
			
			// only keep data
			intOffset += strComments.length()+1;
			arrRawChiptune = new Vector(arrRawChiptune.subList(intOffset, arrRawChiptune.size()));
			
			byte[] arrBytesHeader = {
				31,									// HeaderLength-2
				0,									// Checksum
				'-','l','h','5','-',				// Compression type
				0,0,0,0,							// Compressed file size
				0,0,0,0,							// UnCompressed file size
				0,0,0,0,							// TimeStamp
				0,									// File Attribute
				0,									// Header Level
				7+2,								// FileName Length
				'V','T','X','F','I','L','E',0,'S',	// FileName + 0 + FileType ("S" ?) -> "VTXFILE"
				0,0									// File CheckSum
			};
			
			// Set Compressed file Size
			arrBytesHeader[7] = (byte)(arrRawChiptune.size() & 0xFF);
			arrBytesHeader[8] = (byte)((arrRawChiptune.size() & 0xFF00) >> 8);
			arrBytesHeader[9] = (byte)((arrRawChiptune.size() & 0xFF0000) >> 16);
			arrBytesHeader[10] = (byte)((arrRawChiptune.size() & 0xFF000000) >> 24);
			
			// Set UnCompressed file Size
			arrBytesHeader[11] = (byte)(intPackedDataSize.intValue() & 0xFF);
			arrBytesHeader[12] = (byte)((intPackedDataSize.intValue() & 0xFF00) >> 8);
			arrBytesHeader[13] = (byte)((intPackedDataSize.intValue() & 0xFF0000) >> 16);
			arrBytesHeader[14] = (byte)((intPackedDataSize.intValue() & 0xFF000000) >> 24);
			
			byte[] arrBytes = new byte[arrRawChiptune.size() + arrBytesHeader.length +1];			
			for(byte b=0;b<arrBytesHeader.length;b++){arrBytes[b] = arrBytesHeader[b];}
			for(int i=0;i<arrRawChiptune.size();i++) arrBytes[i+arrBytesHeader.length] = ((Byte)arrRawChiptune.elementAt(i)).byteValue();
			arrBytes[arrBytes.length-1] = 0;
			
			// the following info are stored direcctly in the array of bytes
			// Set File Checksum - 
			// XXX - The File Checksum cannot be known ???
			
			// Set HeaderChecksum
			byte bytHeaderChecksum = 0;
			for(byte b=2;b<arrBytesHeader.length;b++){bytHeaderChecksum+=(byte)arrBytes[b];}
			arrBytes[1] = bytHeaderChecksum;
			
			// We should decompress and process next data
			ByteArrayInputStream arrBytesIS = new ByteArrayInputStream(arrBytes);
			InputStream  in = arrBytesIS;
			LhaInputStream lhaInp = new LhaInputStream(in);
			
			LhaEntry lhaCompressedEntry;
			try {
				lhaCompressedEntry = lhaInp.getNextEntry();
				// Hack of the lha library
				lhaCompressedEntry.setOffset(33);
				YMC_Tools.debug("+ Entry = " + lhaCompressedEntry);
				
				// Decompress YM3 data
				Vector arrData = new Vector();
				arrData.addElement(new Byte((byte)'Y'));
				arrData.addElement(new Byte((byte)'M'));
				arrData.addElement(new Byte((byte)'3'));
				arrData.addElement(new Byte((byte)'!'));
				
				// We need a file to decompress
				FileOutputStream tmpFile = new FileOutputStream("VTXFILE.LHA");
				tmpFile.write(arrBytes);
				tmpFile.close();
								
				File inpFile = new File("VTXFILE.LHA");
				LhaFile inpLhaFile = new LhaFile(inpFile);
				InputStream  inputStreamEntry = inpLhaFile.getInputStream(lhaCompressedEntry);				
				
				int intBytesRead = 0;
				while((inputStreamEntry.available()>0) && (intBytesRead++<lhaCompressedEntry.getOriginalSize()))
				{
					Byte byte_read = new Byte((byte)inputStreamEntry.read());
					arrData.addElement(byte_read);
				}
				inputStreamEntry.close();
				inpLhaFile.close();
				inpFile.delete();
				
				// get uncompressed data from YM plugin
				YmInputPlugin ymplug = new YmInputPlugin();
				arrRegistersValues = ymplug.getPSGRegistersValues(arrData, "YM");
				return new Chiptune(strSongName,
						strAuthorName,
						arrRegistersValues,
						bytPlayerfrequency,
						intFrequency.longValue(),
						false,
						0,
						null/*,
						null*/);
			} catch (LhaException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
}
