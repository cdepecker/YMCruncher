package ymcruncher.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertFalse;

public interface OutputPluginTest {

    default String getTmpDir() throws IOException {
        final String tmpdir = Files.createTempDirectory("tmpDirPrefix").toFile().getAbsolutePath();
        final String tmpDirsLocation = System.getProperty("java.io.tmpdir");
        assert (tmpdir).startsWith(tmpDirsLocation);
        return tmpdir;
    }

    default void delDir(String tmpDir) throws IOException {
        Path dirPath = Paths.get(tmpDir);
        Files.walk(dirPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);

        assertFalse(Files.exists(dirPath), "Directory still exists");
    }

}
