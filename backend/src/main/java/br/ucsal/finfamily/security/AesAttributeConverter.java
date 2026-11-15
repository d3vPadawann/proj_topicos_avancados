package br.ucsal.finfamily.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Implementacao de Criptografia AES-256-GCM para dados sensiveis no banco de dados (RNF-003).
 */
@Component
@Converter
public class AesAttributeConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTE = 12;
    private static final int TAG_LENGTH_BIT = 128;

    private static SecretKeySpec secretKey;

    @Value("${finfamily.security.aes-key:FinFamilyAES256KeyUCSAL2026Sec!!}")
    public void setAesKey(String keyStr) {
        initKey(keyStr);
    }

    private static synchronized void initKey(String keyStr) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(keyStr.getBytes(StandardCharsets.UTF_8));
            key = Arrays.copyOf(key, 32); // 256 bits
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao inicializar chave AES-256", e);
        }
    }

    private static SecretKeySpec getKey() {
        if (secretKey == null) {
            initKey("FinFamilyAES256KeyUCSAL2026Sec!!");
        }
        return secretKey;
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            return "ENC256:" + Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao criptografar com AES-256", e);
        }
    }

    public static String decrypt(String encryptedText) {
        if (encryptedText == null || !encryptedText.startsWith("ENC256:")) {
            return encryptedText;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText.substring(7));
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "***PROTEGIDO_AES256***";
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return decrypt(dbData);
    }
}
