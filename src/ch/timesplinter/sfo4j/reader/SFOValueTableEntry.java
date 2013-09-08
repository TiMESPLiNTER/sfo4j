package ch.timesplinter.sfo4j.reader;

import java.io.FileInputStream;
import java.io.IOException;


public class SFOValueTableEntry {
	private int valueBytesReaded;
	
	public SFOValueTableEntry() {
		this.valueBytesReaded = 0;
	}
	
	/**
	 * Reads an entry of the dataValueTable an return it as String
	 * 
	 * @param fIn
	 * @param sfoIndexTableEntry
	 * @return String
	 * @throws IOException
	 */
	public byte[] readEntry(FileInputStream fIn, SFOIndexTableEntry sfoIndexTableEntry) throws IOException {
		byte[] entryByteArray = new byte[sfoIndexTableEntry.getSizeValueData()];
		
		fIn.read(entryByteArray,0,sfoIndexTableEntry.getSizeValueData());
		valueBytesReaded += sfoIndexTableEntry.getSizeValueData();
		
		long offsetNextValue = sfoIndexTableEntry.getOffsetDataValueInDataTable()+sfoIndexTableEntry.getSizeValueDataAndPadding(); // korrekt!
		long skipBytes = (offsetNextValue)-valueBytesReaded;
		fIn.skip(skipBytes);
		valueBytesReaded += skipBytes;
		
		return entryByteArray;
	}
}
