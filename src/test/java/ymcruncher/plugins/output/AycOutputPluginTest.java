package ymcruncher.plugins.output;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class AycOutputPluginTest implements OutputPluginTest {

    AycOutputPlugin aycOutputPlugin = new AycOutputPlugin();

    @BeforeEach
    void setUp() {
    }

    @Test
    void getMenuLabel() {
        assertEquals(AycOutputPlugin.AYC_FORMAT, aycOutputPlugin.getMenuLabel());
    }

    @Test
    void doCrunch() {
//        String tmpDir = getTmpDir();
//        final String destFile = tmpDir + separator + "testFile";
//        final Chiptune chiptune = new Chiptune();
//        aycOutputPlugin.doCrunch(destFile, chiptune);
//        delDir(tmpDir);
        fail("Unimplemented test");
    }

    @Test
    void getExtension() {
        assertEquals(AycOutputPlugin.AYC, aycOutputPlugin.getExtension());
    }

    @Test
    void doAYCCrunch() {
        ArrayList input = new ArrayList(Arrays.asList((byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0x7F, (byte) 0x7F, (byte) 0x7F, (byte) 0x7F));
        ArrayList output = aycOutputPlugin.doAYCCrunch(input, 0x100);
        System.out.println(output);
        ArrayList outputCheck = new ArrayList(Arrays.asList((byte) 0, (byte) -96, (byte) -3, (byte) 0, (byte) 127, (byte) -3, (byte) 4));
        assertEquals(outputCheck, output);
    }
}