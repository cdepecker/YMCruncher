package ymcruncher.plugins;

import com.google.auto.service.AutoService;
import ymcruncher.core.Chiptune;
import ymcruncher.core.OutputPlugin;

import java.util.ArrayList;

@AutoService(OutputPlugin.class)
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
