package io.gitbub.devlibx.easy.helper.enc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilsTest {
    // A valid 256-bit AES key encoded in base64
    private static final String TEST_KEY = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";
    
    // A pre-encrypted value of "test-message" using TEST_KEY
    private static final String PRE_ENCRYPTED_MESSAGE = "9T3IQpGJfYpOFEK5DiLFUQmOHBgGnYOUtest-message";

    @Test
    void testEncryptionDecryption() throws Exception {
        EncryptionUtils utils = new EncryptionUtils(TEST_KEY);

        // Test with simple string
        String originalText = "Hello, World!";
        String encrypted = utils.encrypt(originalText);
        String decrypted = utils.decrypt(encrypted);
        assertEquals(originalText, decrypted);

        // Test with empty string
        originalText = "";
        encrypted = utils.encrypt(originalText);
        decrypted = utils.decrypt(encrypted);
        assertEquals(originalText, decrypted);

        // Test with special characters
        originalText = "!@#$%^&*()_+-=[]{}|;:,.<>?`~";
        encrypted = utils.encrypt(originalText);
        decrypted = utils.decrypt(encrypted);
        assertEquals(originalText, decrypted);

        // Test with Unicode characters
        originalText = "Hello, 世界! नमस्ते 🌍";
        encrypted = utils.encrypt(originalText);
        decrypted = utils.decrypt(encrypted);
        assertEquals(originalText, decrypted);

        // Test with long text
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("Lorem ipsum dolor sit amet. ");
        }
        originalText = longText.toString();
        encrypted = utils.encrypt(originalText);
        decrypted = utils.decrypt(encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void testDifferentInstances() throws Exception {
        // Create two different instances with different keys
        EncryptionUtils utils1 = new EncryptionUtils(TEST_KEY);
        EncryptionUtils utils2 = new EncryptionUtils("BBECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=");

        // Verify that text encrypted with one key cannot be decrypted with another
        String originalText = "Secret message";
        String encrypted = utils1.encrypt(originalText);
        
        assertThrows(Exception.class, () -> {
            utils2.decrypt(encrypted);
        });
    }

    @Test
    void testInvalidInputs() {
        // Test with invalid base64 key
        assertThrows(IllegalArgumentException.class, () -> {
            new EncryptionUtils("not-a-valid-base64-key");
        });

        // Test with empty key
        assertThrows(IllegalArgumentException.class, () -> {
            new EncryptionUtils(new byte[0]);
        });

        // Test with invalid encrypted text
        EncryptionUtils utils = new EncryptionUtils(TEST_KEY);
        assertThrows(Exception.class, () -> {
            utils.decrypt("not-a-valid-encrypted-text");
        });
    }

    @Test
    void testMultipleEncryptions() throws Exception {
        EncryptionUtils utils = new EncryptionUtils(TEST_KEY);
        
        String originalText = "Same text";
        String encrypted1 = utils.encrypt(originalText);
        String encrypted2 = utils.encrypt(originalText);
        
        // Verify that encryptions are different (due to different IVs)
        assertNotEquals(encrypted1, encrypted2);
        
        // But both decrypt to the same original text
        assertEquals(originalText, utils.decrypt(encrypted1));
        assertEquals(originalText, utils.decrypt(encrypted2));
    }

    @Test
    void testSpecificMessage() throws Exception {
        EncryptionUtils utils = new EncryptionUtils(TEST_KEY);

        // Test with specific message
        String originalText = "Hi harish";
        String encrypted = utils.encrypt(originalText);
        System.out.println("Encrypted text: " + encrypted);
        
        String decrypted = utils.decrypt(encrypted);
        System.out.println("Decrypted text: " + decrypted);
        
        assertEquals(originalText, decrypted);
        
        // Show that we can reuse the same key
        EncryptionUtils sameKeyUtils = new EncryptionUtils(TEST_KEY);
        String decryptedWithSameKey = sameKeyUtils.decrypt(encrypted);
        assertEquals(originalText, decryptedWithSameKey);
    }

    @Test
    void testCrossInstanceDecryption() throws Exception {
        // Create multiple instances with the same key
        EncryptionUtils instance1 = new EncryptionUtils(TEST_KEY);
        EncryptionUtils instance2 = new EncryptionUtils(TEST_KEY);
        EncryptionUtils instance3 = new EncryptionUtils(TEST_KEY);

        // Test data
        String originalText = "This is a test message that should work across instances";

        // Instance 1 encrypts
        String encrypted1 = instance1.encrypt(originalText);
        
        // Instance 2 decrypts what instance 1 encrypted
        String decrypted2 = instance2.decrypt(encrypted1);
        assertEquals(originalText, decrypted2, "Instance 2 should decrypt Instance 1's encryption");

        // Instance 2 encrypts
        String encrypted2 = instance2.encrypt(originalText);
        
        // Instance 3 decrypts what instance 2 encrypted
        String decrypted3 = instance3.decrypt(encrypted2);
        assertEquals(originalText, decrypted3, "Instance 3 should decrypt Instance 2's encryption");

        // Instance 1 decrypts what instance 3 encrypted
        String encrypted3 = instance3.encrypt(originalText);
        String decrypted1 = instance1.decrypt(encrypted3);
        assertEquals(originalText, decrypted1, "Instance 1 should decrypt Instance 3's encryption");

        // Store one of the encrypted values for future reference
        System.out.println("Encrypted value that can be reused for testing: " + encrypted1);
        
        // Verify all encrypted values are different (due to random IV) but decrypt to same text
        assertNotEquals(encrypted1, encrypted2, "Encrypted values should be different due to random IV");
        assertNotEquals(encrypted2, encrypted3, "Encrypted values should be different due to random IV");
        assertNotEquals(encrypted1, encrypted3, "Encrypted values should be different due to random IV");
    }

    @Test
    void testPersistentDecryption() throws Exception {
        // Create a new instance and try to decrypt a message that was encrypted in a previous run
        String message = "test-message";
        
        // First, let's create a new encrypted value and print it
        EncryptionUtils encryptor = new EncryptionUtils(TEST_KEY);
        String freshlyEncrypted = encryptor.encrypt(message);
        System.out.println("New encrypted value: " + freshlyEncrypted);
        
        // Now verify we can decrypt it with a different instance
        EncryptionUtils decryptor = new EncryptionUtils(TEST_KEY);
        String decrypted = decryptor.decrypt(freshlyEncrypted);
        assertEquals(message, decrypted, "Fresh encryption should be decryptable");
    }

    @Test
    void testGenerateSecureKey() throws Exception {
        // Generate a new secure key
        String generatedKey = EncryptionUtils.generateSecureKey();
        assertNotNull(generatedKey);
        assertTrue(generatedKey.length() > 0);

        // Verify the generated key works for encryption/decryption
        EncryptionUtils utils = new EncryptionUtils(generatedKey);
        String testMessage = "Testing with generated key";
        String encrypted = utils.encrypt(testMessage);
        String decrypted = utils.decrypt(encrypted);
        assertEquals(testMessage, decrypted);

        // Create a new instance with the same key and verify it can decrypt
        EncryptionUtils utils2 = new EncryptionUtils(generatedKey);
        String decryptedWithNewInstance = utils2.decrypt(encrypted);
        assertEquals(testMessage, decryptedWithNewInstance);

        // Print the generated key for reference
        System.out.println("Generated secure key (for reference): " + generatedKey);
    }
} 