package org.example.pay;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class PayContext {
    private final Map<String, PayStrategy> payStrategyMap;

    public PayContext() {
        this.payStrategyMap = loadStrategies();
    }

    public void payWithType(String type) {
        PayStrategy payStrategy = findPayStrategy(type);
        if (payStrategy != null) {
            payStrategy.pay();
        } else {
            System.out.println("未匹配到支付类型 支付失败！");
        }
    }

    private PayStrategy findPayStrategy(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return payStrategyMap.get(normalize(type));
    }

    public Map<String, PayStrategy> strategies() {
        return payStrategyMap;
    }

    /**
     * 基于 Java SPI 自动加载所有支付策略，并构建只读策略表。
     *
     * 设计目标：
     * 1. 失败要尽早：启动阶段就暴露配置问题，避免运行时随机失败。
     * 2. 错误要可定位：报错信息包含 strategy type 和实现类名，便于排查。
     * 3. 结果要稳定：返回不可变 Map，避免后续代码误修改路由表。
     *
     * SPI 约定：
     * - 文件路径：META-INF/services/org.example.pay.PayStrategy
     * - 文件内容：每行一个实现类全限定名
     */
    private Map<String, PayStrategy> loadStrategies() {
        // LinkedHashMap: 保证遍历顺序稳定，便于日志比对和问题复现。
        Map<String, PayStrategy> loaded = new LinkedHashMap<>();
        ServiceLoader<PayStrategy> loader = ServiceLoader.load(PayStrategy.class);

        try {
            for (PayStrategy payStrategy : loader) {
                // 统一 key 规范，避免 " pay_ali " / "PAY_ALI" 这种写法造成匹配失败。
                String key = normalize(payStrategy.type());
                if (key.isEmpty()) {
                    throw new IllegalStateException(
                            "支付策略 type 不能为空, class=" + payStrategy.getClass().getName()
                    );
                }

                // 防止重复路由：同一个 type 只能对应一个策略实现。
                PayStrategy previous = loaded.putIfAbsent(key, payStrategy);
                if (previous != null) {
                    throw new IllegalStateException(
                            "检测到重复支付策略 type=" + key
                                    + ", existedClass=" + previous.getClass().getName()
                                    + ", conflictClass=" + payStrategy.getClass().getName()
                    );
                }
            }
        } catch (ServiceConfigurationError e) {
            // ServiceLoader 在配置错误（类名写错/类不可实例化）时会抛该异常。
            throw new IllegalStateException(
                    "加载支付策略失败，请检查 SPI 文件 META-INF/services/org.example.pay.PayStrategy",
                    e
            );
        }

        // 生产环境建议在启动期就保证至少存在一条可用策略，避免系统“看起来启动成功但实际不可用”。
        if (loaded.isEmpty()) {
            throw new IllegalStateException("未加载到任何支付策略，请检查 SPI 配置是否正确。");
        }

        // 返回保序的不可变快照，既防误改，也保证遍历顺序可预测。
        return Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
    }

    private String normalize(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }
}
