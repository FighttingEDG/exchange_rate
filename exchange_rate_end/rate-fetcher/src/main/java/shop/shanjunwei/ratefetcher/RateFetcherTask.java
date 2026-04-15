package shop.shanjunwei.ratefetcher;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import shop.shanjunwei.common.RateDto;
import shop.shanjunwei.ratefetcher.config.ExchangeProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class RateFetcherTask {
    private final KafkaTemplate<String, RateDto> kafkaTemplate;
    private final ExchangeProperties exchangeProperties;
    private final RestTemplate restTemplate;
    private final Random random = new Random();

    public RateFetcherTask(KafkaTemplate<String, RateDto> kafkaTemplate,
                          ExchangeProperties exchangeProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.exchangeProperties = exchangeProperties;
        this.restTemplate = new RestTemplate();
    }

    // 测试用：每3秒执行一次；生产环境改为 cron="0 */8 6,14,22 * * *"（每天 6:00, 14:00, 22:00）
    @Scheduled(fixedRate = 3000)
    public void fetchRate() {
        if ("FAKE".equals(exchangeProperties.getMode())) {
            fetchFakeRate();
        } else {
            fetchRealRate();
        }
    }

    private void fetchFakeRate() {
        List<String> rates = exchangeProperties.getRates();
        for (String pair : rates) {
            RateDto rate = generateFakeRate(pair);
            kafkaTemplate.send("rate-topic", rate);
            System.out.println("模拟发送到Kafka: " + rate);
        }
    }

    private RateDto generateFakeRate(String ratePair) {
        String[] parts = ratePair.split("-");
        String from = parts[0];
        String to = parts[1];
        BigDecimal value = BigDecimal.valueOf(0.14 + random.nextDouble() * 0.02)
                .setScale(4, RoundingMode.HALF_UP);
        return new RateDto(from, to, value);
    }

    private void fetchRealRate() {
        try {
            // 构建请求 URL
            String url = buildApiUrl();

            // 调用第三方 API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                System.err.println("API 调用失败: " + response);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> ratesJson = (Map<String, Object>) response.get("rates");

            if (ratesJson == null) {
                System.err.println("rates 字段为空");
                return;
            }

            // 获取 EUR 转各币种的汇率
            BigDecimal eurToCny = getBigDecimal(ratesJson, "CNY");

            for (String pair : exchangeProperties.getRates()) {
                String[] split = pair.split("-");
                String from = split[0];
                String to = split[1];

                BigDecimal value = BigDecimal.ZERO;

                if ("CNY".equals(from)) {
                    // 计算 CNY 转 其他币种 = (EUR 转 其他币种) / (EUR 转 CNY)
                    BigDecimal eurToTarget = getBigDecimal(ratesJson, to);
                    if (eurToCny != null && eurToTarget != null) {
                        value = eurToTarget.divide(eurToCny, 6, RoundingMode.HALF_UP);
                    }
                } else if ("EUR".equals(from)) {
                    value = getBigDecimal(ratesJson, to);
                }

                if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
                    RateDto rate = new RateDto(from, to, value);
                    kafkaTemplate.send("rate-topic", rate);
                    System.out.println("真实发送到Kafka: " + rate);
                }
            }
        } catch (Exception e) {
            System.err.println("调用第三方 API 异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildApiUrl() {
        List<String> rates = exchangeProperties.getRates();
        StringBuilder symbols = new StringBuilder();
        for (String pair : rates) {
            String[] split = pair.split("-");
            symbols.append(split[1]).append(",");
        }
        if (symbols.length() > 0) {
            symbols.setLength(symbols.length() - 1);  // 去掉最后一个逗号
        }

        return String.format("%s?access_key=%s&symbols=%s",
                exchangeProperties.getApiUrl(),
                exchangeProperties.getAccessKey(),
                symbols);
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
