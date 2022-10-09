import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

public class FileTest {
    ClassLoader mClassLoader = getClass().getClassLoader();

    @Test
    public void writeStrToFile() throws IOException {
        File file = new File(mClassLoader.getResource("my_doc.txt").getFile());
        assertTrue(file.isFile());
        byte[] sampleCipher = """
                31heahdjajdh22enwdbsbdse2ea
                """.getBytes();
        byte[] sampleIv = HexFormat.of().formatHex("hello".getBytes()).getBytes();


        FileOutputStream fileOutputStream = new FileOutputStream(file);

        byte[] alltext = new byte[sampleCipher.length + sampleIv.length];

        for (int i = 0; i < sampleIv.length; i++) {
            alltext[i] = sampleIv[i];
        }
        for (int i = 0; i < sampleCipher.length; i++) {
            alltext[sampleIv.length + i] = sampleCipher[i];
        }

        fileOutputStream.write(alltext);

        fileOutputStream.close();
    }


    @Test
    public void sampleFileRead() throws IOException {
        File file = new File(mClassLoader.getResource("my_doc.txt").getFile());
        assertTrue(file.isFile());
        byte[] ros = IOUtils.toByteArray(new FileInputStream(file));

        byte[] first = new byte[5];
        ByteBuffer byteBuffer = ByteBuffer.wrap(first);
        first = byteBuffer.put(ros, 0, 5).array();
        byte[] second = new byte[ros.length - 5];
        byteBuffer.clear();
        byteBuffer = ByteBuffer.wrap(second);
        second = byteBuffer.put(ros, 5, second.length).array();

        assertEquals(new String(first), "first");
//        assertEquals(new String(second), "oneout");
    }

    @Test
    public void checkRandomness() {
        byte[] str = new byte[10];
        new SecureRandom().nextBytes(str);
        System.out.println(str);
    }

    @Test
    public void checkHex() {
        System.out.println(HexFormat.of().formatHex("s5sol".getBytes()));
    }

    @Test
    public void checkTildeSupport() {
        File file = Path.of("~/videos/mfile.txt"
                        .replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home"))))
                .toFile();

        System.out.println(file.getPath());
        assertTrue(file.isFile());

    }

    @Test
    public void testPathNullPtr() {
        File file = Path.of("mfile.txt"
                        .replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home"))))
                .toFile();

        System.out.println(Path.of(file.getAbsoluteFile().getParent()));
    }
}