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
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

public class m_gcm {
    public static byte[] OnGcmEncrypt(final SecretKey secretKey, final byte[] iv, final byte[] plainText) throws GeneralSecurityException, UnsupportedEncodingException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

        return cipher.doFinal(plainText);
    }

    public static byte[] OnGcmDecrypt(final SecretKey secretKey, final AlgorithmParameterSpec gcmParameters, final byte[] cipherText)
            throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameters);

        return cipher.doFinal(cipherText);
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

    void writeFileBytes(byte[] file_blob, File file_path) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file_path);
        fileOutputStream.write(file_blob);
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
        char[] iv_confirmation = new char[]{};
        char[] iv_chars = new char[]{};
        byte[] salt = new byte[]{};
        byte[] iv = new byte[]{};

        try {
            final String op_mode, file_path, output_file_name;
            op_mode = args[0];

            password = console.readPassword("Enter password");//scanner.nextLine().toCharArray();

            if (op_mode.equals("enc")) {
                password_confirmation = console.readPassword("Confirm password");//.toCharArray();
            }

            //System.out.println("Enter iv");
            //halt converting to bytes till we're sure we have utf-8 string
            iv_chars = console.readPassword("Enter iv");//.toCharArray();

            if (op_mode.equals("enc")) {
                iv_confirmation = console.readPassword("Confirm iv");//.toCharArray();
            }

            file_path = args[1];
            output_file_name = args[2];

            if (op_mode.equals("enc")) {
                if (!Arrays.equals(password, password_confirmation))
                    throw new IOException("Passwords do not match");
                else if (!Arrays.equals(iv_chars, iv_confirmation))
                    throw new IOException("Ivs do not match");
            }

            //check all console input is mappable to utf-8
            mM_gcm.charsAreUTF8(password);
            iv = mM_gcm.charsAreUTF8(iv_chars);


            final File input_file = Path.of(file_path).toFile();

            if (!input_file.exists()) {
                throw new IOException("File does not exist");
            }
            salt = new byte[]{2, 4, 5, 5, 3, 7, 0, 3, 1, 4, 2, 3, 5, 6, 3, 2};
            final int iteration_count = 65536;

            final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WITHHMACSHA256", "BCFIPS");

            secret_k = secretKeyFactory.generateSecret(new PBEKeySpec(password, salt, iteration_count, 256)).getEncoded();

            final SecretKey sec_key = new SecretKeySpec(secret_k, "AES");


            if (op_mode.equals("enc")) {

                plain_text = mM_gcm.readFileBytes(file_path);
                cipher_text = OnGcmEncrypt(sec_key, iv, plain_text);
                //make path platform delimiter agnostic
                mM_gcm.writeFileBytes(cipher_text, Path.of(input_file.getParent(), output_file_name).toFile());

            } else if (op_mode.equals("dec")) {

                cipher_text = mM_gcm.readFileBytes(file_path);
                plain_text = OnGcmDecrypt(sec_key, new GCMParameterSpec(128, iv), cipher_text);
                mM_gcm.writeFileBytes(plain_text, Path.of(input_file.getParent(), output_file_name).toFile());

            }
        } finally {
            mM_gcm = null;
            Arrays.fill(iv, (byte) 0);
            Arrays.fill(secret_k, (byte) 0);
            Arrays.fill(plain_text, (byte) 0);
            Arrays.fill(cipher_text, (byte) 0);
            Arrays.fill(password, (char) 0);
            Arrays.fill(iv_chars, (char) 0);
            Arrays.fill(password_confirmation, (char) 0);
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(iv_confirmation, (char) 0);

            console.writer().close();

            System.gc();
        }
    }
}