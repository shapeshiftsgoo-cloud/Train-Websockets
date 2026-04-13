package com.faculdade.websockets;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class WebSocketAppService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    // Lista de 10 cidades com latitude e longitude (para a API Open-Meteo)
    private final List<CidadeInfo> cidades = List.of(
            new CidadeInfo("São Paulo", -23.5505, -46.6333),
            new CidadeInfo("Rio de Janeiro", -22.9068, -43.1729),
            new CidadeInfo("Brasília", -15.7801, -47.9292),
            new CidadeInfo("Salvador", -12.9714, -38.5014),
            new CidadeInfo("Fortaleza", -3.7172, -38.5431),
            new CidadeInfo("Belo Horizonte", -19.9208, -43.9378),
            new CidadeInfo("Manaus", -3.1019, -60.0250),
            new CidadeInfo("Curitiba", -25.4284, -49.2733),
            new CidadeInfo("Recife", -8.0476, -34.8770),
            new CidadeInfo("Porto Alegre", -30.0346, -51.2177)
    );

    public WebSocketAppService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 5000)
    public void enviarClima() {
        // Escolhe uma cidade aleatória
        CidadeInfo cidade = cidades.get(random.nextInt(cidades.size()));

        // Faz a requisição na API Open-Meteo
        String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true",
                cidade.lat(), cidade.lon());

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> currentWeather = (Map<String, Object>) response.get("current_weather");

            double temperatura = (double) currentWeather.get("temperature");
            int weatherCode = (int) currentWeather.get("weathercode");

            // Monta o payload JSON (usando um Map ou DTO)
            Map<String, Object> payload = Map.of(
                    "cidade", cidade.nome(),
                    "temperatura", temperatura,
                    "descricao", traduzirCodigoClima(weatherCode),
                    "horario", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            );

            // Realiza o Broadcast para o tópico
            messagingTemplate.convertAndSend("/topic/clima", (Object) payload);
            System.out.println("Dado enviado: " + payload);

        } catch (Exception e) {
            System.err.println("Erro ao buscar clima: " + e.getMessage());
        }
    }

    // Helper para traduzir os códigos da WMO da Open-Meteo
    private String traduzirCodigoClima(int code) {
        return switch (code) {
            case 0 -> "Céu Limpo";
            case 1, 2, 3 -> "Parcialmente Nublado";
            case 45, 48 -> "Neblina";
            case 51, 53, 55 -> "Garoa";
            case 61, 63, 65 -> "Chuva";
            case 95, 96, 99 -> "Tempestade";
            default -> "Indefinido";
        };
    }

    // Record auxiliar para armazenar os dados da cidade
    record CidadeInfo(String nome, double lat, double lon) {}
}

