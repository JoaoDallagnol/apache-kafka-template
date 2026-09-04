# Roadmap Kafka - Order Processing

Este projeto faz parte de um workspace de estudos Kafka em Java/Spring:

- `../kafka-template/`: template minimo ja finalizado para producer, consumer e topic simples.
- `./`: projeto evolutivo com `order-service`, `payment-service` e `notification-service`.

O objetivo do `order-processing` e simular um fluxo comum em microservices: um pedido e criado por uma API, salvo no banco local do `order-service`, publicado como evento no Kafka e processado de forma assincrona por outros servicos.

## 1. Base Sem Kafka

### 1.1 Estrutura dos microservices

- [x] Validar que os tres servicos sobem separadamente:
  - `order-service` na porta `8081`
  - `payment-service` na porta `8082`
  - `notification-service` na porta `8083`
- [x] Entender a separacao por camada:
  - `controller`: entrada REST.
  - `service`: regra de negocio.
  - `repository`: acesso ao banco.
  - `entity`: modelo persistido.
  - `dto`: contratos HTTP.
  - `event`: contratos de eventos e portas para Kafka.

Por que fazer: antes de Kafka entrar, cada MS precisa funcionar sozinho. Kafka nao deve ser usado para esconder falta de dominio, transacao ou persistencia local.

### 1.2 Criacao de pedido via REST

- [x] Criar um pedido em `POST /api/v1/orders`.
- [x] Listar pedidos em `GET /api/v1/orders`.
- [x] Consultar um pedido por id em `GET /api/v1/orders/{id}`.

O que Kafka fara depois: apos salvar o pedido, o `order-service` vai publicar um evento `OrderCreatedEvent`.

### 1.3 Portas de evento sem implementacao

- [x] Localizar `OrderEventPublisher` no `order-service`.
- [x] Localizar `PaymentEventPublisher` no `payment-service`.
- [x] Entender que as classes `NoOp...Publisher` existem apenas para a API funcionar antes da implementacao Kafka.

Por que fazer: o service conhece uma porta de publicacao, nao conhece diretamente `KafkaTemplate`. Isso evita acoplar regra de negocio na tecnologia de mensageria.

## 2. Primeiro Fluxo Kafka

### 2.1 Subir Kafka local

- [x] Criar um `docker-compose.yaml` na raiz do projeto `order-processing/`.
- [x] Subir Kafka local em `localhost:9092`.
- [x] Validar com `kafka-topics.sh --list`.

O que praticar: bootstrap server, broker local, CLI do Kafka e diferenca entre acessar Kafka de fora do container e de dentro do container.

### 2.2 Criar topicos iniciais via Spring

- [x] Adicionar dependencia Spring Kafka em `order-service/pom.xml`.
- [x] Criar o arquivo `order-service/src/main/java/com/example/orderprocessing/order/config/kafka/KafkaTopicConfig.java`.
- [x] Declarar um `@Bean NewTopic ordersCreatedTopic()` para o topic `orders.created`.
- [x] Definir `orders.created` com 3 particoes e replication factor 1.
- [x] Manter o nome do topic em `order-service/src/main/resources/application.yaml`:
  - `app.kafka.topics.orders-created=orders.created`.
- [x] Criar o arquivo `order-service/src/main/java/com/example/orderprocessing/order/config/kafka/KafkaTopicProperties.java`.
- [x] Mapear `app.kafka.topics.orders-created` em `KafkaTopicProperties`.
- [x] Usar `KafkaTopicProperties` dentro de `KafkaTopicConfig`, em vez de hardcoded string.
- [x] Subir o `order-service` e validar se o topic foi criado com `kafka-topics.sh --describe --topic orders.created`.

Arquivo esperado:

```text
order-service/
  src/main/java/com/example/orderprocessing/order/config/kafka/
    KafkaTopicConfig.java
    KafkaTopicProperties.java
```

Por que fazer: neste projeto, os topics serao criados por `@Bean NewTopic` para que a configuracao fique visivel no codigo. O topic continua tendo nome de evento/fato de negocio: `orders.created` representa algo que ja aconteceu, nao um comando para outro servico.

### 2.3 Producer no order-service

- [ ] Criar o arquivo `order-service/src/main/java/com/example/orderprocessing/order/config/kafka/KafkaProducerConfig.java`.
- [ ] Criar producer para key `String` e value `OrderCreatedEvent`.
- [ ] Configurar serializacao JSON do value.
- [ ] Configurar idempotencia do producer.
- [ ] Criar o arquivo `order-service/src/main/java/com/example/orderprocessing/order/messaging/kafka/OrderKafkaEventPublisher.java`.
- [ ] Fazer `OrderKafkaEventPublisher` implementar `OrderEventPublisher`.
- [ ] Publicar `OrderCreatedEvent` no topic `orders.created`.
- [ ] Usar `orderId` como message key.

Arquivos esperados:

```text
order-service/
  src/main/java/com/example/orderprocessing/order/config/kafka/
    KafkaProducerConfig.java
    KafkaTopicProperties.java
  src/main/java/com/example/orderprocessing/order/messaging/kafka/
    OrderKafkaEventPublisher.java
```

Como funciona: a key define a particao. Usar `orderId` garante que eventos do mesmo pedido tendem a manter ordem dentro da mesma particao.

### 2.4 Consumer no payment-service

- [ ] Adicionar dependencia Spring Kafka em `payment-service/pom.xml`.
- [ ] Criar o arquivo `payment-service/src/main/java/com/example/orderprocessing/payment/config/kafka/KafkaTopicProperties.java`.
- [ ] Mapear `app.kafka.topics.orders-created=orders.created`.
- [ ] Mapear `app.kafka.topics.payments-processed=payments.processed`.
- [ ] Criar o arquivo `payment-service/src/main/java/com/example/orderprocessing/payment/config/kafka/KafkaConsumerConfig.java`.
- [ ] Criar consumer para key `String` e value `OrderCreatedEvent`.
- [ ] Configurar desserializacao JSON do value.
- [ ] Criar o arquivo `payment-service/src/main/java/com/example/orderprocessing/payment/config/kafka/KafkaProducerConfig.java`.
- [ ] Criar producer para key `String` e value `PaymentProcessedEvent`.
- [ ] Criar o arquivo `payment-service/src/main/java/com/example/orderprocessing/payment/config/kafka/KafkaTopicConfig.java`.
- [ ] Declarar um `@Bean NewTopic paymentsProcessedTopic()` para o topic `payments.processed`.
- [ ] Criar o arquivo `payment-service/src/main/java/com/example/orderprocessing/payment/messaging/kafka/OrderCreatedKafkaListener.java`.
- [ ] Criar listener para `orders.created` com group id `payment-service`.
- [ ] Converter payload JSON para `OrderCreatedEvent`.
- [ ] Chamar `PaymentService.handleOrderCreated`.
- [ ] Criar o arquivo `payment-service/src/main/java/com/example/orderprocessing/payment/messaging/kafka/PaymentKafkaEventPublisher.java`.
- [ ] Fazer `PaymentKafkaEventPublisher` implementar `PaymentEventPublisher`.
- [ ] Publicar `PaymentProcessedEvent` em `payments.processed`.

Arquivos esperados:

```text
payment-service/
  src/main/java/com/example/orderprocessing/payment/config/kafka/
    KafkaConsumerConfig.java
    KafkaProducerConfig.java
    KafkaTopicConfig.java
    KafkaTopicProperties.java
  src/main/java/com/example/orderprocessing/payment/messaging/kafka/
    OrderCreatedKafkaListener.java
    PaymentKafkaEventPublisher.java
```

O que Kafka esta fazendo: desacoplando `order-service` de `payment-service`. O pedido nao chama pagamento por HTTP; ele publica um fato e segue.

### 2.5 Consumer no notification-service

- [ ] Adicionar dependencia Spring Kafka em `notification-service/pom.xml`.
- [ ] Criar o arquivo `notification-service/src/main/java/com/example/orderprocessing/notification/config/kafka/KafkaTopicProperties.java`.
- [ ] Mapear `app.kafka.topics.orders-created=orders.created`.
- [ ] Mapear `app.kafka.topics.payments-processed=payments.processed`.
- [ ] Criar o arquivo `notification-service/src/main/java/com/example/orderprocessing/notification/config/kafka/KafkaConsumerConfig.java`.
- [ ] Criar consumer para key `String` e value JSON.
- [ ] Criar o arquivo `notification-service/src/main/java/com/example/orderprocessing/notification/messaging/kafka/OrderCreatedKafkaListener.java`.
- [ ] Consumir `orders.created` com group id `notification-service`.
- [ ] Chamar `NotificationService.handleOrderCreated`.
- [ ] Criar o arquivo `notification-service/src/main/java/com/example/orderprocessing/notification/messaging/kafka/PaymentProcessedKafkaListener.java`.
- [ ] Consumir `payments.processed` com group id `notification-service`.
- [ ] Chamar `NotificationService.handlePaymentProcessed`.

Arquivos esperados:

```text
notification-service/
  src/main/java/com/example/orderprocessing/notification/config/kafka/
    KafkaConsumerConfig.java
    KafkaTopicProperties.java
  src/main/java/com/example/orderprocessing/notification/messaging/kafka/
    OrderCreatedKafkaListener.java
    PaymentProcessedKafkaListener.java
```

Por que fazer: um mesmo evento pode ter mais de um consumidor independente. Isso e uma das utilizacoes mais comuns de Kafka em MS.

## 3. Contrato de Evento

### 3.1 Eventos JSON tipados

- [ ] Padronizar todos os eventos com:
  - `eventId`
  - aggregate id, como `orderId`
  - dados relevantes do evento
  - `occurredAt`
- [ ] Evitar publicar entity JPA diretamente.

Por que fazer: evento e contrato publico entre servicos. Entity e detalhe interno do banco.

### 3.2 Headers uteis

- [ ] Enviar `eventType`.
- [ ] Enviar `correlationId`.
- [ ] Enviar `eventVersion`.

Como funciona: headers ajudam rastreabilidade, roteamento e evolucao sem depender somente do payload.

### 3.3 Versionamento de eventos

- [ ] Criar `OrderCreatedEventV1`.
- [ ] Documentar campos obrigatorios.
- [ ] Garantir compatibilidade ao adicionar campos novos.

Boa pratica: consumidores devem ignorar campos desconhecidos. Evite remover ou renomear campos sem uma estrategia de migracao.

## 4. Consumer Groups e Paralelismo

### 4.1 Group ids por responsabilidade

- [ ] Usar group id `payment-service`.
- [ ] Usar group id `notification-service`.
- [ ] Entender que consumidores com group ids diferentes recebem copias independentes do mesmo evento.

Como funciona: Kafka entrega cada mensagem uma vez por consumer group, nao uma vez globalmente.

### 4.2 Particoes e escala horizontal

- [ ] Rodar mais de uma instancia do mesmo consumer group.
- [ ] Observar distribuicao por particao.
- [ ] Testar que a ordem e garantida apenas dentro da mesma particao.

Por que fazer: particionamento e o mecanismo principal de paralelismo no Kafka.

### 4.3 Offset e replay

- [ ] Ver offsets do consumer group via CLI.
- [ ] Resetar offset em ambiente local.
- [ ] Reprocessar eventos desde o inicio.

Uso real: replay e util para reconstruir projecoes, corrigir bugs e reprocessar historico controladamente.

## 5. Erros, Retry e Dead Letter Topic

### 5.1 Tratamento de erro no consumer

- [ ] Simular erro no `payment-service`.
- [ ] Configurar retry com backoff.
- [ ] Evitar loop infinito silencioso.

Por que fazer: consumer falhando sem politica clara trava processamento ou perde rastreabilidade.

### 5.2 Dead Letter Topic

- [ ] Criar `orders.created.dlt` em `payment-service/src/main/java/com/example/orderprocessing/payment/config/kafka/KafkaTopicConfig.java`.
- [ ] Enviar mensagem para DLT apos retries esgotados.
- [ ] Logar causa do erro.

Como funciona: DLT guarda mensagens que nao puderam ser processadas para analise ou reprocessamento manual.

### 5.3 Erros recuperaveis vs nao recuperaveis

- [ ] Tratar erro temporario como retry.
- [ ] Tratar payload invalido como DLT direto.
- [ ] Documentar exemplos no codigo.

Boa pratica: nem todo erro merece retry. JSON invalido, schema incompativel ou campo obrigatorio ausente normalmente nao melhora tentando de novo.

## 6. Idempotencia e Duplicidade

### 6.1 Idempotencia no producer

- [ ] Configurar producer com idempotencia.
- [ ] Usar `acks=all`.
- [ ] Entender impacto de retries no producer.

Por que fazer: producer pode reenviar mensagem em falhas temporarias. Idempotencia reduz duplicidade gerada pelo producer.

### 6.2 Idempotencia no consumer

- [ ] Criar tabela de eventos processados.
- [ ] Salvar `eventId` antes/depois da regra de negocio com criterio claro.
- [ ] Ignorar evento ja processado.

Uso real: Kafka oferece entrega pelo menos uma vez na pratica comum. Seu consumer precisa tolerar duplicidade.

### 6.3 Chaves naturais

- [ ] Impedir pagamento duplicado para o mesmo `orderId`.
- [ ] Criar constraint unica quando fizer sentido.
- [ ] Testar reprocessamento do mesmo evento.

Boa pratica: idempotencia nao e so Kafka config; normalmente envolve banco e regra de negocio.

## 7. Transacao Local e Outbox

### 7.1 Problema do dual write

- [ ] Entender o risco de salvar pedido no banco e falhar antes de publicar no Kafka.
- [ ] Entender o risco inverso: publicar no Kafka e falhar ao commitar banco.

Por que fazer: esse e um dos problemas mais importantes em sistemas event-driven.

### 7.2 Outbox pattern

- [ ] Criar tabela `outbox_events` no `order-service`.
- [ ] Salvar pedido e evento outbox na mesma transacao.
- [ ] Criar publisher assicrono que le outbox e publica no Kafka.
- [ ] Marcar evento como publicado.

Como funciona: o banco vira a fonte confiavel da intencao de publicar. Kafka recebe depois, sem perder consistencia local.

### 7.3 Reprocessamento da outbox

- [ ] Implementar retentativa para eventos nao publicados.
- [ ] Evitar publicar duplicado sem controle.
- [ ] Registrar tentativas e ultimo erro.

Boa pratica: outbox nao elimina duplicidade, mas evita perda de evento apos commit no banco.

## 8. Observabilidade

### 8.1 Logs com contexto

- [ ] Logar `eventId`, `orderId`, `topic`, `partition` e `offset`.
- [ ] Usar `correlationId` nos logs dos tres servicos.

Por que fazer: sem contexto, debugar fluxo assincrono vira tentativa e erro.

### 8.2 Health e metricas

- [ ] Adicionar Actuator.
- [ ] Expor health dos servicos.
- [ ] Observar lag de consumer group.

Uso real: lag mostra se o consumer esta atrasado em relacao ao producer.

### 8.3 Testes de integracao

- [ ] Usar Testcontainers para Kafka.
- [ ] Testar producer publicando evento.
- [ ] Testar consumer processando evento.
- [ ] Testar retry e DLT.

Boa pratica: mock de Kafka testa pouco. Para fluxo critico, use broker real em teste de integracao.

## 9. Hardening Final

### 9.1 Configuracao externa

- [ ] Garantir que todos os nomes de topicos estejam em `application.yaml`.
- [ ] Garantir que todos os group ids estejam em `application.yaml`.
- [ ] Separar profiles `local` e `test`.

Por que fazer: topico e group id mudam por ambiente. Mesmo usando `@Bean NewTopic`, o nome do topic deve vir de configuracao, nao de string espalhada pelo codigo.

### 9.2 Payload pequeno e objetivo

- [ ] Remover dados desnecessarios dos eventos.
- [ ] Evitar mandar snapshots gigantes sem necessidade.
- [ ] Garantir que consumidores tenham os dados minimos para trabalhar.

Boa pratica: evento deve carregar o que o consumidor precisa para reagir sem acoplamento HTTP desnecessario, mas sem virar dump do banco.

### 9.3 Documentacao operacional

- [ ] Documentar topicos.
- [ ] Documentar producers e consumers.
- [ ] Documentar keys.
- [ ] Documentar DLTs.
- [ ] Documentar como resetar offset localmente.

Resultado esperado: ao final, o projeto deve demonstrar as praticas mais usadas em Kafka para microservices sem virar uma plataforma grande demais para estudo.
