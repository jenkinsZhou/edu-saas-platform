package com.edusphere.api.license;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 授权文件的离线签名/验签工具，基于 JDK 内置 Ed25519（JDK 15+）。
 *
 * <p>授权字符串格式（类 JWT）：{@code base64url(payloadJson) + "." + base64url(signature)}。
 * 签名对象是 payloadJson 的原始字节，避免任何 JSON 规范化歧义。</p>
 */
public final class LicenseSignature {

    private static final String ALGORITHM = "Ed25519";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private LicenseSignature() {
    }

    /** 厂商侧：用 PKCS#8（Base64）私钥对载荷 JSON 签名，返回完整授权字符串。 */
    public static String sign(String payloadJson, String privateKeyBase64) {
        try {
            byte[] pkcs8 = Base64.getDecoder().decode(privateKeyBase64);
            PrivateKey privateKey = KeyFactory.getInstance(ALGORITHM)
                    .generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
            byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(payloadBytes);
            byte[] sig = signature.sign();
            return URL_ENCODER.encodeToString(payloadBytes) + "." + URL_ENCODER.encodeToString(sig);
        } catch (Exception e) {
            throw new IllegalStateException("授权签名失败: " + e.getMessage(), e);
        }
    }

    /**
     * 系统侧：用 X.509（Base64）公钥验签，验签通过返回载荷 JSON 原文，否则返回 {@code null}。
     */
    public static String verifyAndExtract(String licenseText, String publicKeyBase64) {
        if (licenseText == null || licenseText.isBlank() || publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            return null;
        }
        int dot = licenseText.indexOf('.');
        if (dot <= 0 || dot >= licenseText.length() - 1) {
            return null;
        }
        try {
            byte[] payloadBytes = URL_DECODER.decode(licenseText.substring(0, dot));
            byte[] sigBytes = URL_DECODER.decode(licenseText.substring(dot + 1));
            byte[] x509 = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance(ALGORITHM)
                    .generatePublic(new X509EncodedKeySpec(x509));
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(payloadBytes);
            if (!signature.verify(sigBytes)) {
                return null;
            }
            return new String(payloadBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
