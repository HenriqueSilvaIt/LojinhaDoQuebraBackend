package com.devsuperior.dscommerce.services;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class MercadoPagoAuthService {


    @Value("${mercado-pago.client-id}")
    private String clientId;

    @Value("${mercado-pago.client-secret}")
    private String clientSecret;

    @Value("${mercado-pago.webhook-secret}") // Sua assinatura secreta do painel do Mercado Pago
    private String webhookSecret;

    private final WebClient webClient;

    public MercadoPagoAuthService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.mercadopago.com").build();
    }

    @Cacheable("accessToken") // Armazenar o token em cache
    public Mono<String> getAccessToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        return webClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("access_token"));

}

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoAuthService.class);

    public boolean isValidNotification(Map<String, Object> notificationData, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isEmpty()) {
            return false;
        }

        String[] parts = signatureHeader.split(",");
        String ts = null;
        String v1 = null;

        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                if (key.equals("ts")) {
                    ts = value;
                } else if (key.equals("v1")) {
                    v1 = value;
                }
            }
        }

        if (ts == null || v1 == null) {
            return false;
        }

        String dataId = null;
        Map<String, Object> data = (Map<String, Object>) notificationData.get("data");
        if (data != null && data.containsKey("id")) {
            Object idObject = data.get("id");
            if (idObject instanceof String) {
                dataId = (String) idObject;
            } else if (idObject != null) {
                dataId = String.valueOf(idObject);
            }
        }

        String requestId = ""; // Você precisaria obter o x-request-id do header se necessário

        String manifest = String.format("id:%s;request-id:%s;ts:%s;",
                (dataId != null ? dataId : ""), requestId, ts);

        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Hex.encodeHexString(hmacBytes);
            return calculatedSignature.equals(v1);
        } catch (Exception e) {
            logger.error("Erro ao validar a assinatura da notificação: {}", e.getMessage());
            return false;
        }
    }
}
