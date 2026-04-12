# Train-Websockets

Esse é um excelente projeto! Ele aborda conceitos fundamentais de sistemas reativos e distribuídos, e é perfeito para demonstrar como WebSockets superam o long-polling tradicional em aplicações de tempo real.

Como você solicitou a implementação de um artefato (código para um projeto completo), preparei a estrutura do servidor Spring Boot, o cliente web e o modelo do README.md prontos para você utilizar e subir para o seu GitHub.

Aqui está o guia completo para a implementação.
1. O Servidor (Spring Boot)

Para começar, crie um projeto no Spring Initializr com as dependências: Spring Web e WebSocket.

A. Dependência do pom.xml
Certifique-se de ter essa dependência no seu arquivo:
XML

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

B. Habilitando o Agendamento
Na sua classe principal (a que tem @SpringBootApplication), adicione a anotação para permitir o uso do @Scheduled:
Java

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClimaMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClimaMonitorApplication.class, args);
    }
}

C. Configuração do WebSocket
Crie uma classe para configurar os endpoints e o broker de mensagens.
Java

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Habilita o SockJS fallback e permite conexões de outras origens
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefixo para mensagens enviadas do servidor para o cliente
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}

D. Serviço de Monitoramento (Lógica e API Open-Meteo)
Usaremos a Open-Meteo API por ser gratuita e não exigir chaves de autenticação.
Java

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
public class ClimaService {

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

    public ClimaService(SimpMessagingTemplate messagingTemplate) {
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
            messagingTemplate.convertAndSend("/topic/clima", payload);
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

2. O Cliente (Web - HTML/JS)

Crie um arquivo index.html. Você pode colocar isso na pasta src/main/resources/static/ do Spring Boot para que seja servido automaticamente no http://localhost:8080/.
HTML

<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Monitoramento Climático - WebSockets</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.5.1/sockjs.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
    
    <style>
        body { font-family: Arial, sans-serif; background: #f4f4f9; padding: 20px; }
        .header { display: flex; justify-content: space-between; align-items: center; }
        .status { padding: 8px 15px; border-radius: 20px; font-weight: bold; }
        .connected { background: #d4edda; color: #155724; }
        .disconnected { background: #f8d7da; color: #721c24; }
        
        #dashboard { display: flex; flex-wrap: wrap; gap: 15px; margin-top: 20px; }
        .card { 
            padding: 20px; border-radius: 10px; width: 200px; color: white; 
            box-shadow: 0 4px 6px rgba(0,0,0,0.1); transition: transform 0.2s;
        }
        .card h3 { margin: 0 0 10px 0; font-size: 1.2em; }
        .card .temp { font-size: 2em; font-weight: bold; margin-bottom: 5px; }
        .card .desc { font-size: 0.9em; opacity: 0.9; }
        .card .time { font-size: 0.8em; margin-top: 15px; text-align: right; opacity: 0.7; }
    </style>
</head>
<body>

    <div class="header">
        <h1>Dashboard Climático Real-Time</h1>
        <div id="status" class="status disconnected">Desconectado</div>
    </div>

    <div id="dashboard">
        </div>

    <script>
        let stompClient = null;

        function conectar() {
            // Conecta ao endpoint configurado no Spring Boot
            const socket = new SockJS('/ws');
            stompClient = Stomp.over(socket);
            
            // Desativa os logs de debug do STOMP no console (opcional)
            stompClient.debug = null;

            stompClient.connect({}, function (frame) {
                document.getElementById('status').innerText = 'Conectado';
                document.getElementById('status').className = 'status connected';
                
                // Inscreve no tópico para receber as atualizações
                stompClient.subscribe('/topic/clima', function (mensagem) {
                    const dados = JSON.parse(mensagem.body);
                    atualizarDashboard(dados);
                });
            }, function(error) {
                document.getElementById('status').innerText = 'Desconectado';
                document.getElementById('status').className = 'status disconnected';
                // Tenta reconectar após 5 segundos em caso de queda
                setTimeout(conectar, 5000);
            });
        }

        function atualizarDashboard(dados) {
            const dashboard = document.getElementById('dashboard');
            
            // Verifica se a cidade já tem um card, se sim, atualiza; se não, cria um novo
            let cardId = 'card-' + dados.cidade.replace(/\s+/g, '-');
            let card = document.getElementById(cardId);

            if (!card) {
                card = document.createElement('div');
                card.id = cardId;
                card.className = 'card';
                dashboard.prepend(card); // Adiciona os novos no topo
            }

            // Define a cor baseada na temperatura
            let corFundo = '#6c757d'; // Default
            if (dados.temperatura < 18) corFundo = '#007bff'; // Frio (Azul)
            else if (dados.temperatura >= 18 && dados.temperatura < 26) corFundo = '#ffc107'; // Agradável (Amarelo)
            else if (dados.temperatura >= 26) corFundo = '#dc3545'; // Calor (Vermelho)

            card.style.backgroundColor = corFundo;

            card.innerHTML = `
                <h3>${dados.cidade}</h3>
                <div class="temp">${dados.temperatura}°C</div>
                <div class="desc">${dados.descricao}</div>
                <div class="time">Atualizado às ${dados.horario}</div>
            `;
        }

        // Inicia a conexão quando a página carregar
        window.onload = conectar;
    </script>
</body>
</html>

3. Modelo de Entrega (README.md para o GitHub)

Crie um arquivo chamado README.md na raiz do seu projeto e cole o seguinte conteúdo:
Markdown

# Monitoramento de Temperatura em Tempo Real com WebSockets 🌡️

Este projeto demonstra a implementação de um sistema distribuído de monitoramento climático utilizando **Spring Boot**, **WebSockets (STOMP/SockJS)** e a API pública do **Open-Meteo**.

O objetivo principal é ilustrar o comportamento de um servidor que faz o *push* (envio) de dados em tempo real para os clientes conectados, eliminando a necessidade de requisições de *polling* por parte do front-end.

## 🚀 Como Rodar o Projeto

### Pré-requisitos
* Java 17+ instalado
* Maven instalado (ou use o *wrapper* `./mvnw`)

### Passos
1. Clone este repositório:
   ```bash
   git clone [https://github.com/SEU-USUARIO/SEU-REPOSITORIO.git](https://github.com/SEU-USUARIO/SEU-REPOSITORIO.git)

    Navegue até a pasta do projeto:
    Bash

    cd nome-do-repositorio

    Execute o projeto usando o Maven:
    Bash

    mvn spring-boot:run

    Abra o navegador e acesse: http://localhost:8080

🔄 Explicação do Fluxo de Mensagens

    Agendamento (Servidor): A cada 5 segundos, a anotação @Scheduled dispara um método no ClimaService.

    Coleta de Dados: O servidor escolhe aleatoriamente uma de 10 cidades pré-definidas e faz uma requisição HTTP REST para a API do Open-Meteo para buscar o clima daquele exato momento.

    Broadcast (Servidor -> Cliente): Após tratar os dados da API (montando o JSON com cidade, temperatura, descrição e hora), o servidor usa o SimpMessagingTemplate para enviar o payload para o tópico /topic/clima.

    Recepção (Cliente): O navegador do usuário, conectado via biblioteca stomp.js, está "inscrito" (subscribed) neste tópico. Assim que a mensagem chega, o callback do Javascript é acionado instantaneamente.

    Atualização da Interface: O DOM (HTML) é atualizado. Se a cidade já estiver na tela, seus dados são sobrescritos. O card muda de cor de acordo com a temperatura (Azul para frio < 18°C, Amarelo para ameno 18°C a 26°C e Vermelho para calor > 26°C).

📸 Demonstração da Tela

(Adicione aqui a imagem do seu sistema rodando! Exclua esse texto e use o formato abaixo)
🛠️ Tecnologias Utilizadas

    Back-end: Java, Spring Boot, Spring WebSockets, RestTemplate.

    Front-end: HTML5, CSS3, JavaScript puro, SockJS, STOMP.js.

    API de Terceiros: Open-Meteo.
