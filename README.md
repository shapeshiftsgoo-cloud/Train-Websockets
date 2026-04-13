# Train-Websockets

# Websockets - Monitoramento de temperatura em tempo real

Este projeto demonstra a implementação de um sistema distribuído de monitoramento climático utilizando Spring Boot, WebSockets e a API pública do Open-Meteo.

O objetivo principal é ilustrar o comportamento de um servidor que faz o *push* (envio) de dados em tempo real para os clientes conectados, eliminando a necessidade de requisições de *polling* por parte do front-end.

## Como usar o projeto

### Pré-requisitos
* Java 21 instalado
* Maven instalado

### Passos
1. Clone este repositório:
   ```bash
   git clone https://github.com/shapeshiftsgoo-cloud/Train-Websockets.git

    Navegue até a pasta do projeto:
    Bash

    cd websockets

    Execute o projeto usando o Maven:
    Bash

    mvn spring-boot:run

    Abra o navegador e acesse: http://localhost:8080

## Explicação do Fluxo de Mensagens

    Agendamento (Servidor): A cada 5 segundos, a anotação @Scheduled dispara um método no ClimaService.

    Coleta de Dados: O servidor escolhe aleatoriamente uma de 10 cidades pré-definidas e faz uma requisição HTTP REST para a API do Open-Meteo para buscar o clima daquele exato momento.

    Broadcast (Servidor -> Cliente): Após tratar os dados da API (montando o JSON com cidade, temperatura, descrição e hora), o servidor usa o SimpMessagingTemplate para enviar o payload para o tópico /topic/clima.

    Recepção (Cliente): O navegador do usuário, conectado via biblioteca stomp.js, está "inscrito" (subscribed) neste tópico. Assim que a mensagem chega, o callback do Javascript é acionado instantaneamente.

    Atualização da Interface: O DOM (HTML) é atualizado. Se a cidade já estiver na tela, seus dados são sobrescritos. O card muda de cor de acordo com a temperatura (Azul para frio < 18°C, Amarelo para ameno 18°C a 26°C e Vermelho para calor > 26°C).

Demonstração da Tela

<img width="1366" height="498" alt="image" src="https://github.com/user-attachments/assets/13f1914d-79c3-4ff0-8b9a-b133bfb20387" />


## Tecnologias Utilizadas

    Back-end: Java, Spring Boot, Spring WebSockets, RestTemplate.

    Front-end: HTML5, CSS3, JavaScript puro, SockJS, STOMP.js.

    API de Terceiros: Open-Meteo.
