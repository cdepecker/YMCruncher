package Plugins;

import java.util.ArrayList;

import YMCruncher.OutputPlugin;
import YMCruncher.Chiptune;

public class FakeOutputPlugin extends OutputPlugin {

	@Override
	public ArrayList<Byte> doCrunch(String strDestFile, Chiptune chiptune) {
		return null;
	}

	@Override
	public String getExtension() {
		return null;
	}

	@Override
	public String getMenuLabel() {
		return "Fake";
	}		
	
}
