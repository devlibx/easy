package io.gitbub.devlibx.easy.helper.enc;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionUtils {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private final SecretKey key;

    /**
     * Generate a secure random AES-256 key that can be used with this utility.
     * 
     * IMPORTANT: This method should only be used ONCE to generate a key for your application.
     * The generated key should be stored securely (e.g., in a secure key management system,
     * hardware security module, or encrypted configuration) and reused for all future
     * encryption/decryption operations.
     * 
     * DO NOT generate a new key for each encryption operation as this will make it impossible
     * to decrypt previously encrypted data.
     * 
     * Example usage:
     * 1. Generate the key ONCE:
     *    String key = EncryptionUtils.generateSecureKey();
     *    // Store this key securely
     * 
     * 2. Use the same key for all encryption/decryption:
     *    EncryptionUtils utils = new EncryptionUtils(storedKey);
     * 
     * @return A base64 encoded string representing a secure random 256-bit AES key
     * @throws NoSuchAlgorithmException if AES is not available in the JVM
     */
    public static String generateSecureKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, new SecureRandom());
        SecretKey key = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Initialize with a base64 encoded secret key
     *
     * @param base64Key Base64 encoded secret key
     */
    public EncryptionUtils(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        this.key = new SecretKeySpec(decodedKey, "AES");
    }

    /**
     * Initialize with a raw secret key
     *
     * @param key Secret key bytes
     */
    public EncryptionUtils(byte[] key) {
        this.key = new SecretKeySpec(key, "AES");
    }

    /**
     * Encrypt a string using AES-256-GCM
     *
     * @param plaintext The text to encrypt
     * @return Base64 encoded encrypted string
     * @throws Exception if encryption fails
     */
    public String encrypt(String plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);

        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    /**
     * Decrypt an encrypted string using AES-256-GCM
     *
     * @param encryptedText Base64 encoded encrypted text
     * @return Decrypted string
     * @throws Exception if decryption fails
     */
    public String decrypt(String encryptedText) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);

        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }
}
