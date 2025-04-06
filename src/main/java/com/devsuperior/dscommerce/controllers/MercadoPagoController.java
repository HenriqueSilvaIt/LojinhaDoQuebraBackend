package com.devsuperior.dscommerce.controllers;

import com.devsuperior.dscommerce.services.MercadoPagoAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class MercadoPagoController {



    @Value("${mercado-pago.device-id}")
    private String deviceId;

    private final WebClient webClient;
    private final MercadoPagoAuthService authService;

    private final WebSocketController webSocketController; // Declared as final


    public MercadoPagoController(WebClient.Builder webClientBuilder,
                                 MercadoPagoAuthService authService,
                                 WebSocketController webSocketController) { // WebSocketController COMO PARÂMETRO
        this.webClient = webClientBuilder.baseUrl("https://api.mercadopago.com").build();
        this.authService = authService;
        this.webSocketController = webSocketController; // Agora a atribuição usa a instância injetada
    }
    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoController.class);

    @PostMapping("/mercado-pago-payment")
    public Mono<Map> mercadoPagoPayment(@RequestBody Map<String, Object> paymentData) {
        logger.info("Dados do pagamento recebidos: {}", paymentData);

        return authService.getAccessToken()
                .flatMap(accessToken -> {
                    logger.info("acc    essToken obtido: {}", accessToken);
                    return webClient.post()
                            .uri("/point/integration-api/devices/{deviceId}/payment-intents", deviceId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(paymentData))
                            .retrieve()
                            .bodyToMono(Map.class)
                            .doOnSuccess(response -> logger.info("Resposta da API do Mercado Pago: {}", response))
                            .doOnError(error -> logger.error("Erro na requisição para a API do Mercado Pago: {}", error));
                });
    }

    @GetMapping("/mercado-pago-payment-status/{paymentintentid}")
    public Mono<Map> getPaymentStatus(@PathVariable String paymentintentid) {
        logger.info("Obtendo status do pagamento para paymentintentid: {}", paymentintentid);

        return authService.getAccessToken()
                .flatMap(accessToken -> {
                    logger.info("accessToken obtido: {}", accessToken);
                    return webClient.get()
                            .uri("/point/integration-api/payment-intents/{paymentintentid}", paymentintentid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .header("x-test-scope", "sandbox")
                            .retrieve()
                            .bodyToMono(Map.class)
                            .doOnSuccess(response -> logger.info("Resposta da API do Mercado Pago: {}", response))
                            .doOnError(error -> logger.error("Erro na requisição para a API do Mercado Pago: {}", error));
                });
    }


    @PostMapping("/mercado-pago/notificacoes") // Defina a URL do seu endpoint de notificação
    public ResponseEntity<String> receberNotificacao(@RequestBody Map<String, Object> notificationData,
                                                     @RequestHeader("x-signature") String signature) {
        logger.info("Notificação do Mercado Pago recebida:");
        logger.info("Dados: {}", notificationData);
        logger.info("Assinatura (x-signature): {}", signature);

        // ---------------------------------------------------------------------
        // PASSO IMPORTANTE: Validar a origem da notificação
        // Consulte a documentação do Mercado Pago para detalhes sobre a validação
        // usando a assinatura secreta.
        // ---------------------------------------------------------------------
        boolean validNotification = authService.isValidNotification(notificationData, signature); // Você precisará implementar este método

        if (validNotification) {
            String topic = (String) notificationData.get("type");
            if ("point_integration_wh".equals(topic)) {
                Map<String, Object> data = (Map<String, Object>) notificationData.get("data");
                String paymentIntentId = (String) data.get("payment_intent_id");
                String status = (String) data.get("status");

                // Atualize seu banco de dados com o status do pagamento para paymentIntentId

                // Notifique o frontend via WebSocket
                webSocketController.sendPaymentStatus(paymentIntentId, status);
            }
            return new ResponseEntity<>("Notificação recebida e processada", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Notificação inválida", HttpStatus.UNAUTHORIZED);
        }

    }
}
