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
import java.util.Scanner;

public class m_gcm {
    private byte[] gcmEncrypt(final SecretKey secretKey, final byte[] iv, final byte[] plainText) throws GeneralSecurityException, UnsupportedEncodingException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

        return cipher.doFinal(plainText);
    }

    private byte[] gcmDecrypt(final SecretKey secretKey, final AlgorithmParameterSpec gcmParameters, final byte[] cipherText)
            throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameters);

        return cipher.doFinal(cipherText);
    }

    private byte[] readFileBytes(String path) throws IOException {
        InputStream inputStream = null;
        byte[] read_output_stream;

        try {
            inputStream = new FileInputStream(path);

        } finally {
            read_output_stream = IOUtils.toByteArray(inputStream);
        }
        return read_output_stream;
    }

    private void writeFileBytes(byte[] file_blob, File file_path) throws IOException {
        final FileOutputStream fileOutputStream = new FileOutputStream(file_path);
        fileOutputStream.write(file_blob);
        fileOutputStream.close();
    }

    private byte[] charsAreUTF8(char[] arr) throws CharacterCodingException {
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer encoded = encoder.encode(CharBuffer.wrap(arr));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return bytes;
    }


    //main.jar <mode[enc|dec]> <file_path> <out_file_name>
    public static void main(String[] args) throws IOException, GeneralSecurityException {
        Security.addProvider(new BouncyCastleFipsProvider());
        m_gcm mM_gcm = new m_gcm();


        final Scanner sc = new Scanner(System.in);

        char[] password = new char[]{};
        char[] password_confirmation = new char[]{};
        char[] iv_confirmation = new char[]{};
        char[] iv_chars = new char[]{};
        byte[] iv = new byte[]{};
        byte[] secret_k = new byte[]{};

        byte[] plain_text = new byte[]{};
        byte[] cipher_text = new byte[]{};

        try {
            System.out.println("Enter password");
            password = sc.nextLine().toCharArray();
            System.out.println("Confirm password");
            password_confirmation = sc.nextLine().toCharArray();
            System.out.println("Enter iv");
            //halt converting to bytes till we're sure we have utf-8 string
            iv_chars = sc.nextLine().toCharArray();
            System.out.println("Confirm iv");
            iv_confirmation = sc.nextLine().toCharArray();
            sc.close();

            final String op_mode = args[0];
            final String file_path = args[1];
            final String output_file_name = args[2];

            final File input_file = Path.of(file_path).toFile();

            if (!input_file.exists()) {
                throw new IOException("File does not exist");
            }

            final int error_flag = Boolean.compare(Arrays.equals(password, password_confirmation),
                    Arrays.equals(iv_chars, iv_confirmation));

            //check all console input is mappable to utf-8
            mM_gcm.charsAreUTF8(password);
            iv = mM_gcm.charsAreUTF8(iv_chars);

            if (error_flag < 0)
                throw new IOException("Passwords do not match");
            else if (error_flag > 0) throw new IOException("Ivs do not match");

            final byte[] salt = new byte[]{2, 4, 5, 5, 3, 7, 0, 3, 1, 4, 2, 3, 5, 6, 3, 2};
            final int iteration_count = 65536;

            final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WITHHMACSHA256", "BCFIPS");

            secret_k = secretKeyFactory.generateSecret(new PBEKeySpec(password, salt, iteration_count, 256)).getEncoded();
            final SecretKey sec_key = new SecretKeySpec(secret_k, "AES");


            if (op_mode.equals("enc")) {

                plain_text = mM_gcm.readFileBytes(file_path);
                cipher_text = mM_gcm.gcmEncrypt(sec_key, iv, plain_text);
                //make path platform delimiter agnostic
                mM_gcm.writeFileBytes(cipher_text, Path.of(input_file.getParent(), output_file_name).toFile());

            } else if (op_mode.equals("dec")) {

                cipher_text = mM_gcm.readFileBytes(file_path);
                plain_text = mM_gcm.gcmDecrypt(sec_key, new GCMParameterSpec(128, iv), cipher_text);
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
            Arrays.fill(iv_confirmation, (char) 0);

            System.gc();
        }
    }
}