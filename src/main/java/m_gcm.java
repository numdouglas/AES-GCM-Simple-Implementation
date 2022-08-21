import org.apache.commons.io.IOUtils;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

public class m_gcm {
    public static byte[] gcmEncrypt(final SecretKey secretKey, final byte[] iv, final byte[] plainText) throws GeneralSecurityException, UnsupportedEncodingException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

        return cipher.doFinal(plainText);
    }

    public static byte[] gcmDecrypt(final SecretKey secretKey, final AlgorithmParameterSpec gcmParameters, final byte[] cipherText)
            throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameters);

        return cipher.doFinal(cipherText);
    }

    static byte[] readFileBytes(String path) throws IOException {
        InputStream inputStream = null;
        byte[] read_output_stream;

        try {
            inputStream = new FileInputStream(path);

        } finally {
            read_output_stream = IOUtils.toByteArray(inputStream);
        }
        return read_output_stream;
    }

    static void writeFileBytes(byte[] file_blob, File file_path) throws IOException {
        final FileOutputStream fileOutputStream = new FileOutputStream(file_path);
        fileOutputStream.write(file_blob);
        fileOutputStream.close();
    }


    //main.jar <mode[enc|dec]> <file_path> <out_file_name> <password> <iv>
    public static void main(String[] args) throws IOException, GeneralSecurityException {
        Security.addProvider(new BouncyCastleFipsProvider());

        final String op_mode = args[0];
        final String file_path = args[1];
        final String output_file_name = args[2];
        final char[] password = args[3].toCharArray();
        final byte[] iv = args[4].getBytes(StandardCharsets.UTF_8);


        final File input_file = Path.of(file_path).toFile();

        if (!input_file.exists()) {
            throw new IOException("File does not exist");
        }
        final byte[] salt = new byte[]{2, 4, 5, 5, 3, 7, 0, 3, 1, 4, 2, 3, 5, 6, 3, 2};
        final int iteration_count = 65536;

        final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WITHHMACSHA256", "BCFIPS");

        final byte[] secret_k = secretKeyFactory.generateSecret(new PBEKeySpec(password, salt, iteration_count, 256)).getEncoded();
        final SecretKey sec_key = new SecretKeySpec(secret_k, "AES");

        byte[] plain_text = new byte[]{};
        byte[] cipher_text = new byte[]{};

        if (op_mode.equals("enc")) {

            plain_text = readFileBytes(file_path);
            cipher_text = gcmEncrypt(sec_key, iv, plain_text);
            //make path platform delimiter agnostic
            writeFileBytes(cipher_text, Path.of(input_file.getParent(), output_file_name).toFile());

        } else if (op_mode.equals("dec")) {

            cipher_text = readFileBytes(file_path);
            plain_text = gcmDecrypt(sec_key, new GCMParameterSpec(128, iv), cipher_text);
            writeFileBytes(plain_text, Path.of(input_file.getParent(), output_file_name).toFile());

        }


        Arrays.fill(iv, (byte) 0);
        Arrays.fill(secret_k, (byte) 0);
        Arrays.fill(plain_text, (byte) 0);
        Arrays.fill(cipher_text, (byte) 0);
        Arrays.fill(password, (char) 0);

        System.gc();
    }
}
