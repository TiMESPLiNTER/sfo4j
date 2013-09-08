sfo4j
=====
sfo4j is a very easy to use open source library in java for reading sfo-files. You just need to download the jar-package put it in your BuildPath and use it.

It's free, it's open source! Modify it, expand it as you like to. Use it in your projects.

Sample
------
Here a small piece of code to show you how to use the library:

```java
SFOReader sfoReader = new SFOReader("C:/Data/PARAM_RAC.SFO");

SFOWriter sfoWriter = new SFOWriter("C:/Data/PARAM_NEW.SFO");

for(Map.Entry entry : sfoReader.getKeyValueMap().entrySet()){
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
```

Example output:

```
PS3_SYSTEM_VER = 01.9300
APP_VER = 01.00
PARENTAL_LEVEL = 
LICENSE = Library programs ©Sony Computer Entertainment Inc. Licensed for play on the PLAYSTATION®3 Computer Entertainment System or authorized PLAYSTATION®3 format systems. For full terms and conditions see the user's manual. This product is authorized and produced under license from Sony Computer Entertainment Inc. Use is subject to the copyright laws and the terms and conditions of the user's license.
RESOLUTION = ?
TITLE_ID = BCES00052
CATEGORY = DG
BOOTABLE = 
VERSION = 01.00
SOUND_FORMAT = 
TITLE_05 = Ratchet & Clank: Armi di distruzione™
TITLE_03 = Ratchet & Clank: Armados hasta los dientes™
TITLE = Ratchet & Clank: Tools of Destruction™
TITLE_02 = Ratchet & Clank: Opération Destruction™
```