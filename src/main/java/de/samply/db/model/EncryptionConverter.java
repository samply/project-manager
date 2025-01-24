package de.samply.db.model;

import de.samply.app.ProjectManagerConst;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

    private final String encryptionAlgorithm;
    private final SecretKeySpec secretKey;

    // Modify the constructor to accept the Base64-encoded private key
    public EncryptionConverter(
            @Value(ProjectManagerConst.DB_ENCRYPTION_ALGORITHM_SV) String encryptionAlgorithm,
            @Value(ProjectManagerConst.DB_ENCRYPTION_PRIVATE_KEY_IN_BASE64_SV) String base64PrivateKey) {
        this.encryptionAlgorithm = encryptionAlgorithm;

        if (encryptionAlgorithm == null) {
            throw new IllegalArgumentException("Encryption algorithm cannot be null");
        }

        if (base64PrivateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }

        // Decode the Base64-encoded private key
        byte[] privateKeyBytes = Base64.getDecoder().decode(base64PrivateKey);

        // Create the SecretKeySpec from the decoded private key bytes
        this.secretKey = new SecretKeySpec(privateKeyBytes, encryptionAlgorithm);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            if (attribute == null) {
                return null;
            }
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(attribute.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Error encrypting attribute", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null) {
                return null;
            }
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(dbData);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Error decrypting attribute", e);
        }
    }

}
