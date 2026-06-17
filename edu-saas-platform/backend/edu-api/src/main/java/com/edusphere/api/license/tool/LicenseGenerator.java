package com.edusphere.api.license.tool;

import com.edusphere.api.license.LicensePayload;
import com.edusphere.api.license.LicenseSignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 厂商侧命令行工具：生成密钥对、签发授权文件。<b>不参与运行时</b>，仅供发布授权使用。
 *
 * <p>用法（在 backend 目录）：</p>
 * <pre>
 *   mvn -q -pl edu-api compile
 *   # 1) 生成密钥对（公钥填入 edu.license.public-key，私钥厂商自行妥善保管）
 *   mvn -q -pl edu-api exec:java -Dexec.mainClass=com.edusphere.api.license.tool.LicenseGenerator -Dexec.args="keygen"
 *
 *   # 2) 签发授权（输出授权字符串，交给客户上传激活）
 *   mvn -q -pl edu-api exec:java -Dexec.mainClass=com.edusphere.api.license.tool.LicenseGenerator \
 *     -Dexec.args="sign --privateKey=BASE64 --customer=某某教育 --edition=standard --expires=2027-12-31 \
 *                  --graceDays=15 --maxAccounts=50 --maxStudents=2000 --maxCampuses=5 \
 *                  --features=attendance,report,marketing,contract,notification --fingerprint=1A2B-3C4D-5E6F-7A8B"
 * </pre>
 */
public final class LicenseGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("用法: keygen | sign --privateKey=... --customer=... --expires=YYYY-MM-DD [可选项]");
            return;
        }
        switch (args[0]) {
            case "keygen" -> keygen();
            case "sign" -> sign(parseArgs(Arrays.copyOfRange(args, 1, args.length)));
            default -> System.out.println("未知命令: " + args[0]);
        }
    }

    private static void keygen() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair keyPair = generator.generateKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        System.out.println("=== Ed25519 密钥对（请妥善保管私钥，切勿提交到仓库） ===");
        System.out.println("public-key (填入配置 edu.license.public-key):");
        System.out.println(publicKey);
        System.out.println();
        System.out.println("private-key (厂商保管，用于签发授权):");
        System.out.println(privateKey);
    }

    private static void sign(Map<String, String> opts) {
        String privateKey = require(opts, "privateKey");
        LicensePayload payload = new LicensePayload(
                opts.getOrDefault("licenseId", UUID.randomUUID().toString()),
                require(opts, "customer"),
                opts.getOrDefault("edition", "standard"),
                LocalDate.now(),
                LocalDate.parse(require(opts, "expires")),
                parseInt(opts.get("graceDays"), 15),
                parseInt(opts.get("maxAccounts"), null),
                parseInt(opts.get("maxStudents"), null),
                parseInt(opts.get("maxCampuses"), null),
                parseList(opts.get("features")),
                emptyToNull(opts.get("fingerprint"))
        );
        try {
            String json = MAPPER.writeValueAsString(payload);
            String license = LicenseSignature.sign(json, privateKey);
            System.out.println("=== 授权文件（交付给客户，在系统“授权管理”页上传激活） ===");
            System.out.println(license);
        } catch (Exception e) {
            throw new IllegalStateException("签发失败: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                int eq = arg.indexOf('=');
                map.put(arg.substring(2, eq), arg.substring(eq + 1));
            }
        }
        return map;
    }

    private static String require(Map<String, String> opts, String key) {
        String value = opts.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少必填参数 --" + key);
        }
        return value;
    }

    private static Integer parseInt(String value, Integer defaultValue) {
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
    }

    private static List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LicenseGenerator() {
    }
}
