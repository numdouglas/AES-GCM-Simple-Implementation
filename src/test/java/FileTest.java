import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileTest {

    @Test
    public void writeStrToFile() {
        File file= Path.of("t_file.txt").toFile();

        assertTrue(file.isFile());
    }

}