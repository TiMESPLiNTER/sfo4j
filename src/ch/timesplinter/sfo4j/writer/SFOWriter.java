package ch.timesplinter.sfo4j.writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.timesplinter.sfo4j.common.SFODataValue;
import ch.timesplinter.sfo4j.common.SFOUtilities;

/**
 * This class writes a valid SFO file with all entries in the keyValueMap.
 * 
 * This class is copyrighted by TiMESPLiNTER (timesplinter.ch) 2011. Feel free
 * to modify this class and use it in your projects for free.
 * Please contact me if you made an important change or fixed/detected any bug.
 * 
 * @author TiMESPLiNTER
 * @version 1.0
 *
 */
public class SFOWriter {
	public static final byte DATA_ALIGNMENT_REQUIREMENTS = 4;
	public static final int INDEX_TABLE_ENTRY_LENGTH = 16;
	public static final int HEADER_LENGTH = 20;
	
	private String outputFileName;
	private Map<String,SFODataValue> keyValueMap;
	private List<byte[]> indexTableEntryList;
	private List<byte[]> keyTableEntryList;
	
	public SFOWriter(String outputFileName) {
		this.outputFileName = outputFileName;
		this.keyValueMap = new TreeMap<String,SFODataValue>();
		this.indexTableEntryList = new ArrayList<byte[]>();
		this.keyTableEntryList = new ArrayList<byte[]>();
	}
	
	/**
	 * Generates the SFO header for the file and returns it in bytes.
	 * 
	 * @param indexTableLength
	 * @param keyTableLength
	 * @param valueKeyTableLength
	 * @return byte[]
	 */
	private byte[] generateSFOHeader(int indexTableLength, int keyTableLength, int valueKeyTableLength) {
		byte[] sfoHeader = new byte[HEADER_LENGTH];
		
		// write file type ({null}PSF)
		byte[] fileType = {0x00, 0x50, 0x53, 0x46};
		sfoHeader = SFOUtilities.replaceBytesInByteArray(sfoHeader, fileType, 0);
		
		// write psf version
		byte[] psfVersion = {0x01,0x01,0x00,0x00};
		sfoHeader = SFOUtilities.replaceBytesInByteArray(sfoHeader, psfVersion, 4);
		
		// write offset key table
		byte[] keyTableOffset = SFOUtilities.intToByteArrayReverse(HEADER_LENGTH+indexTableLength);
		sfoHeader = SFOUtilities.replaceBytesInByteArray(sfoHeader, keyTableOffset, 8);
		
		// write offset value table
		byte[] valueTableOffset = SFOUtilities.intToByteArrayReverse(HEADER_LENGTH+indexTableLength+keyTableLength);
		sfoHeader = SFOUtilities.replaceBytesInByteArray(sfoHeader, valueTableOffset, 12);
		
		// write number of data elements (32-bit reverse Integer)
		byte[] numDataElmntsBytes = SFOUtilities.intToByteArrayReverse(keyValueMap.size());
		sfoHeader = SFOUtilities.replaceBytesInByteArray(sfoHeader, numDataElmntsBytes, 16);
		
		return sfoHeader;
	}
	
	/**
	 * Generates the index table and returns it in bytes.
	 * 
	 * @return byte[]
	 */
	private byte[] generateIndexTable() {
		byte[] indexTableBytes;
		byte[] lastKeyBytes = {};
		
		for(Map.Entry<String, SFODataValue> entry : keyValueMap.entrySet()){
			indexTableEntryList.add(generateIndexTableEntry(lastKeyBytes,entry.getKey(),entry.getValue().toBytes(), entry.getValue().getDataType()));
			lastKeyBytes = entry.getKey().getBytes();
		}
		
		indexTableBytes = new byte[indexTableEntryList.size()*INDEX_TABLE_ENTRY_LENGTH];
		int indexTableBytesPointer = 0;
		
		for(byte[] indexEntry : indexTableEntryList) {
			SFOUtilities.replaceBytesInByteArray(indexTableBytes, indexEntry, indexTableBytesPointer);
			indexTableBytesPointer += INDEX_TABLE_ENTRY_LENGTH;
		}
		
		return indexTableBytes;
	}
	
	/**
	 * Generates the key table and returns it in bytes
	 * 
	 * @return byte[]
	 */
	private byte[] generateKeyTable() {
		byte[] keyTableBytes;
		int keyTableLength = 0;
		
		for(Map.Entry<String, SFODataValue> entry : keyValueMap.entrySet()){
			byte[] keyTableEntry = generateKeyTableEntry(entry.getKey());
			keyTableEntryList.add(keyTableEntry);
			keyTableLength += keyTableEntry.length;
		}
		
		// gesamte länge muss durch 4 geteilt werden können!
		int finalSize = getDataAlignmentRequirementsLength(null,keyTableLength);
		
		keyTableBytes = new byte[finalSize];
		
		// Initialize with 0x00
		for(int i = 0; i < keyTableBytes.length; i++) {
			keyTableBytes[i] = 0x00;
		}
		
		int indexTableBytesPosition = 0;
		
		for(byte[] entry : keyTableEntryList) {
			SFOUtilities.replaceBytesInByteArray(keyTableBytes, entry, indexTableBytesPosition);
			indexTableBytesPosition += entry.length;
		}
		
		return keyTableBytes;
	}
	
	/**
	 * Generates the value table and return it in bytes.
	 * 
	 * @return byte[]
	 */
	private byte[] generateValueTable() {
		byte[] valueTableBytes;
		
		List<byte[]> valueTableEntries = new ArrayList<byte[]>();
		int valueTableLength = 0;
		
		for(Map.Entry<String, SFODataValue> entry : keyValueMap.entrySet()){
			byte[] valueTableEntry = generateValueTableEntry(entry.getKey(),entry.getValue().toBytes());
			valueTableEntries.add(valueTableEntry);
			valueTableLength += valueTableEntry.length;
		}
		
		valueTableBytes = new byte[valueTableLength];
		int valueTableBytesPosition = 0;
		
		for(byte[] value : valueTableEntries) {
			SFOUtilities.replaceBytesInByteArray(valueTableBytes, value, valueTableBytesPosition);
			valueTableBytesPosition += value.length;
		}
		
		return valueTableBytes;
	}
	
	/**
	 * Generates a index table entry and return it in bytes.
	 * 
	 * @param lastKey
	 * @param key
	 * @param value
	 * @param type
	 * @return byte[]
	 */
	private byte[] generateIndexTableEntry(byte[] lastKey, String key, byte[] value, byte type) {
		byte[] indexTableEntry = new byte[16];
		byte[] lastIndexTableEntry = null;
		if(indexTableEntryList.size()-1 >= 0)
			lastIndexTableEntry = indexTableEntryList.get(indexTableEntryList.size()-1);
		
		// if the value byteArray is less than 4 bytes fill it up to 4 bytes (because of the DATA_ALIGNMENT_REQUIREMENTS)
		int dataAlignmentRequirementLength = getDataAlignmentRequirementsLength(key,value.length);
		int offset = dataAlignmentRequirementLength-value.length;
		byte[] newValue = new byte[dataAlignmentRequirementLength];
		
		for(int i= 0; i < newValue.length; i++) {
			newValue[i] = 0x00;
		}
		value = SFOUtilities.replaceBytesInByteArray(newValue,value,0);
		
		// Write offset of key in key table (offset 0-1)
		short keyOffsetInBytes = 0;
		if(lastIndexTableEntry != null) {
			byte[] lastKeyOffsetBytes = {lastIndexTableEntry[0],lastIndexTableEntry[1]};
			// get Last Key Length
			short lastKeyLength = new Integer(lastKey.length).shortValue();
			keyOffsetInBytes = SFOUtilities.byteArrayReverseToShort(lastKeyOffsetBytes);
			keyOffsetInBytes += lastKeyLength+1;
		}
		
		indexTableEntry = SFOUtilities.replaceBytesInByteArray(indexTableEntry, SFOUtilities.shortToByteArrayReverse(keyOffsetInBytes), 0);
		
		// Write data alignment requirements (offset 2)
		indexTableEntry = SFOUtilities.replaceByteInByteArray(indexTableEntry, DATA_ALIGNMENT_REQUIREMENTS, 2);
		
		// Write value data type (offset 3)
		indexTableEntry = SFOUtilities.replaceByteInByteArray(indexTableEntry, type, 3);
		
		// Write value data size (offset 4-7)
		byte[] valueDataSize = SFOUtilities.intToByteArrayReverse(value.length-offset);
		indexTableEntry = SFOUtilities.replaceBytesInByteArray(indexTableEntry, valueDataSize, 4);
		
		// Write size of value data and Padding (offset 8-11)
		byte[] valueDataSizeWithPadding = SFOUtilities.intToByteArrayReverse(getDataAlignmentRequirementsLength(key,value.length));
		indexTableEntry = SFOUtilities.replaceBytesInByteArray(indexTableEntry, valueDataSizeWithPadding, 8);
		
		// Write offset of value data in Data Table (offset 12-15)
		// Method: last offset (12-15) + size of value and padding (8-11) 
		int valueOffsetInBytes = 0;
		if(lastIndexTableEntry != null) {
			byte[] lastOffsetBytes = {lastIndexTableEntry[12],lastIndexTableEntry[13],lastIndexTableEntry[14],lastIndexTableEntry[15]};
			byte[] lastValueAndPaddingBytes = {lastIndexTableEntry[8],lastIndexTableEntry[9],lastIndexTableEntry[10],lastIndexTableEntry[11]};
			int lastOffset = SFOUtilities.byteArrayReverseToInt(lastOffsetBytes);
			int lastValueAndPadding = SFOUtilities.byteArrayReverseToInt(lastValueAndPaddingBytes);
			valueOffsetInBytes = lastOffset + lastValueAndPadding;
		}
		
		indexTableEntry = SFOUtilities.replaceBytesInByteArray(indexTableEntry, SFOUtilities.intToByteArrayReverse(valueOffsetInBytes), 12);
		
		return indexTableEntry;
	}
	
	/**
	 * Generates a key table entry and return it in bytes.
	 * 
	 * @param value
	 * @return byte[]
	 */
	private byte[] generateKeyTableEntry(String value) {
		try {
			byte[] valueBytes = value.getBytes("UTF8");
			byte[] keyTableEntry = new byte[valueBytes.length+1];
			
			keyTableEntry[keyTableEntry.length-1] = 0x00;
			SFOUtilities.replaceBytesInByteArray(keyTableEntry, valueBytes, 0);
			
			return keyTableEntry;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Generates a value table entry (key is needed for special rules in getDataAlignmentRequirements())
	 * 
	 * @param key
	 * @param value
	 * @return byte[]
	 */
	private byte[] generateValueTableEntry(String key, byte[] value) {
		int finalSize = getDataAlignmentRequirementsLength(key,value.length);
		
		// respect the data alignment requirements
		byte[] newValue;
		newValue = new byte[finalSize];
		
		// Initialize with null bytes
		for(int i = 0; i < newValue.length; i++) {
			newValue[i] = 0x00;
		}
		
		SFOUtilities.replaceBytesInByteArray(newValue, value, 0);
		
		return newValue;
	}
	
	/**
	 * Returns the data alignment requirement respecting length of a value. Key is needed for special rules.
	 * For example if every LICENSE value has to be 512 bytes.
	 * 
	 * @param key
	 * @param valueLength
	 * @return Integer
	 */
	private int getDataAlignmentRequirementsLength(String key, int valueLength) {
		int finalLength;
		
		// handle some stupid special rules of the PS3 game PARAM.SFOs
		if(key != null && key.matches("^TITLE(_\\d{2})?$")) {
			finalLength = 128;
		} else if(key != null && key.equals("TITLE_ID")) {
			finalLength = 16;
		} else if(key != null && key.equals("LICENSE")) {
			finalLength = 512;
		} else {
			finalLength = 0;
			while(valueLength > finalLength) {
				finalLength += DATA_ALIGNMENT_REQUIREMENTS;
			}
		}
		
		return finalLength;
	}
	
	/**
	 * Write the SFO file. Returns true if successful and false if not.
	 * 
	 * @return boolean
	 * @throws IOException 
	 */
	public boolean write() throws IOException {
		byte[] sfoHeader;
		byte[] indexTable;
		byte[] keyTable;
		byte[] valueTable;
		
		
		valueTable = generateValueTable();
		keyTable = generateKeyTable();
		indexTable = generateIndexTable();
		sfoHeader = generateSFOHeader(indexTable.length,keyTable.length,valueTable.length);
		
		FileOutputStream fin = new FileOutputStream(new File(outputFileName).getAbsoluteFile());
	
		fin.write(sfoHeader);
		fin.write(indexTable);
		fin.write(keyTable);
		fin.write(valueTable);
		
		fin.close();
		
		return true;
	}
	
	/**
	 * Adds a key/value pair to the existing keyValueMap.
	 * 
	 * @param key
	 * @param value
	 * @throws UnsupportedEncodingException
	 */
	public void addKeyValuePair(String key, String value) throws UnsupportedEncodingException {
		byte[] valueUtf8Bytes = value.getBytes("UTF8");
		keyValueMap.put(key, new SFODataValue(valueUtf8Bytes,SFODataValue.DATATYPE_STRING));
	}
	
	/**
	 * Adds a key/value pair to the existing keyValueMap.
	 * 
	 * @param key
	 * @param value
	 */
	public void addKeyValuePair(String key, int value) {
		byte[] valueIntReverseBytes = SFOUtilities.intToByteArrayReverse(value);
		keyValueMap.put(key, new SFODataValue(valueIntReverseBytes,SFODataValue.DATATYPE_NUMBER));
	}
	
	/**
	 * Adds a key/value pair to the existing keyValueMap.
	 * 
	 * @param key
	 * @param value
	 */
	public void addkeyValuePair(String key, byte[] value) {
		keyValueMap.put(key, new SFODataValue(value, SFODataValue.DATATYPE_BINARY));
	}

	/**
	 * Returns the current keyValueMap with it's elements that will be written when
	 * you call the write() method of this class.
	 * 
	 * @return Map<String, SFODataValue>
	 */
	public Map<String, SFODataValue> getKeyValueMap() {
		return keyValueMap;
	}
	
	/**
	 * Sets the whole keyValueMap at one time.
	 * 
	 * @param keyValueMap
	 */
	public void setKeyValueMap(Map<String, SFODataValue> keyValueMap) {
		this.keyValueMap = keyValueMap;
	}

	/*public static void main(String[] args) {
		SFOReader sfoReader = new SFOReader("G:/Data/PARAM_RAC.SFO");
		
		SFOWriter sfoWriter = new SFOWriter("G:/Data/PARAM_NEW.SFO");
		
		for(Map.Entry<String, SFODataValue> entry : sfoReader.getKeyValueMap().entrySet()){
			System.out.print(entry.getKey() + " = ");
			if(entry.getValue().getDataType() == SFODataValue.DATATYPE_STRING) {
				System.out.println(entry.getValue().toString());
			} else if(entry.getValue().getDataType() == SFODataValue.DATATYPE_NUMBER) {
				System.out.println(entry.getValue().toInt());
			} else {
				System.out.println(entry.getValue().toBytes());
			}
		}
		
		System.out.println("\n=========\n");
		
		sfoWriter.setKeyValueMap(sfoReader.getKeyValueMap());
		
		boolean writeProcess = sfoWriter.write();
		
		if(writeProcess) {
			System.out.println("SFO file successfully created");
		} else {
			System.err.println("Could not create SFO file");
		}
		
		SFOReader sfoReader2 = new SFOReader("G:/Data/PARAM_NEW.SFO");
		
		for(Map.Entry<String, SFODataValue> entry : sfoReader2.getKeyValueMap().entrySet()){
			System.out.print(entry.getKey() + " = ");
			if(entry.getValue().getDataType() == SFODataValue.DATATYPE_STRING) {
				System.out.println(entry.getValue().toString());
			} else if(entry.getValue().getDataType() == SFODataValue.DATATYPE_NUMBER) {
				System.out.println(entry.getValue().toInt());
			} else {
				System.out.println(entry.getValue().toBytes());
			}
		}
	}*/
}
