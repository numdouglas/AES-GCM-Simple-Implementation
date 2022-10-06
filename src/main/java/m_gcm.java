import org.apache.commons.io.IOUtils;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.regex.Matcher;

public class m_gcm {
    public static byte[] OnGcmEncrypt(final SecretKey secretKey, final byte[] iv, final byte[] plainText) throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

        return cipher.doFinal(plainText);
    }

    public static byte[] OnGcmDecrypt(final char[] password, final byte[] cipherText)
            throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        final int iteration_count = 65536;

        final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WITHHMACSHA256", "BCFIPS");

        byte[] salt = new byte[256];
        byte[] iv = new byte[256];
        byte[] raw_cipher = new byte[cipherText.length - (salt.length + iv.length)];

        ByteBuffer iv_buffer = ByteBuffer.wrap(salt);
        iv_buffer.put(cipherText, 0, salt.length);
        salt = iv_buffer.array();
        iv_buffer.clear();

        iv_buffer = ByteBuffer.wrap(iv);
        iv_buffer.put(cipherText, salt.length, iv.length);
        iv = iv_buffer.array();
        iv_buffer.clear();
        iv_buffer = ByteBuffer.wrap(raw_cipher);
        iv_buffer.put(cipherText, salt.length + iv.length, raw_cipher.length);
        raw_cipher = iv_buffer.array();
        iv_buffer.clear();

        byte[] secret_k = secretKeyFactory.generateSecret(new PBEKeySpec(password, salt, iteration_count, 256)).getEncoded();
        final SecretKey sec_key = new SecretKeySpec(secret_k, "AES");

        cipher.init(Cipher.DECRYPT_MODE, sec_key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(raw_cipher);
    }

    byte[] readFileBytes(String path) throws IOException {
        byte[] read_output_stream;

        try (InputStream inputStream = new FileInputStream(path)) {
            read_output_stream = IOUtils.toByteArray(inputStream);
        }

        return read_output_stream;
    }

    //Java uses utf-16, to allow support on more systems, we limit ourselves to utf-8
    byte[] charsAreUTF8(char[] arr) throws CharacterCodingException {
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer encoded = encoder.encode(CharBuffer.wrap(arr));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return bytes;
    }

    void writeFileBytes(File file_path, byte[]... salt_iv_file_blob) throws IOException {
        final FileOutputStream fileOutputStream = new FileOutputStream(file_path);
        if (salt_iv_file_blob.length == 3) {
            byte[] finalOutputBytes =
                    new byte[salt_iv_file_blob[0].length + salt_iv_file_blob[1].length + salt_iv_file_blob[2].length];

            final ByteBuffer byteBuffer = ByteBuffer.wrap(finalOutputBytes);
            finalOutputBytes = byteBuffer.put(salt_iv_file_blob[0])
                    .put(salt_iv_file_blob[1]).put(salt_iv_file_blob[2]).array();
            byteBuffer.clear();

            fileOutputStream.write(finalOutputBytes);
        } else {
            fileOutputStream.write(salt_iv_file_blob[0]);
        }
        fileOutputStream.close();
    }

    //main.jar <mode[enc|dec]> <file_path> <out_file_name>
    public static void main(String[] args) throws IOException, GeneralSecurityException {

        Security.addProvider(new BouncyCastleFipsProvider());

        m_gcm mM_gcm = new m_gcm();

        final Console console = System.console();

        byte[] secret_k = new byte[]{};
        byte[] plain_text = new byte[]{};
        byte[] cipher_text = new byte[]{};
        char[] password = new char[]{};
        char[] password_confirmation = new char[]{};
        byte[] salt = new byte[256];
        byte[] iv = new byte[256];

        try {
            final String op_mode, file_path, output_file_name;
            op_mode = args[0];

            password = console.readPassword("Enter password");//scanner.nextLine().toCharArray();

            file_path = args[1].replaceFirst("^~",
                    Matcher.quoteReplacement(System.getProperty("user.home")));
            output_file_name = args[2];

            //check all console input is mappable to utf-8
            mM_gcm.charsAreUTF8(password);

            final File input_file = Path.of(file_path).toFile();

            if (op_mode.equals("dec")) {
                cipher_text = mM_gcm.readFileBytes(file_path);
                plain_text = OnGcmDecrypt(password, cipher_text);
                mM_gcm.writeFileBytes(Path.of(input_file.getParent(), output_file_name).toFile(), plain_text);

            } else if (op_mode.equals("enc")) {
                password_confirmation = console.readPassword("Confirm password");//.toCharArray();
                if (!Arrays.equals(password, password_confirmation)) {
                    throw new IOException("Passwords do not match");
                }

                new SecureRandom().nextBytes(iv);
                new SecureRandom().nextBytes(salt);

                final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WITHHMACSHA256", "BCFIPS");
                final int iteration_count = 65536;
                secret_k = secretKeyFactory.generateSecret(new PBEKeySpec(password, salt, iteration_count, 256)).getEncoded();
                final SecretKey sec_key = new SecretKeySpec(secret_k, "AES");

                plain_text = mM_gcm.readFileBytes(file_path);
                cipher_text = OnGcmEncrypt(sec_key, iv, plain_text);
                //make path platform delimiter agnostic
                mM_gcm.writeFileBytes(Path.of(input_file.getParent(), output_file_name).toFile(), salt, iv, cipher_text);
            }
        } finally {
            mM_gcm = null;
            Arrays.fill(iv, (byte) 0);
            Arrays.fill(secret_k, (byte) 0);
            Arrays.fill(plain_text, (byte) 0);
            Arrays.fill(cipher_text, (byte) 0);
            Arrays.fill(password, (char) 0);
            Arrays.fill(password_confirmation, (char) 0);
            Arrays.fill(salt, (byte) 0);

            console.writer().close();

            System.gc();
        }
    }
}