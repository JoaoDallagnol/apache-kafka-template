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

- [x] Criar um `docker-compose.yaml` na raiz ou em `infra/`.
- [x] Subir Kafka local em `localhost:9092`.
- [x] Validar com `kafka-topics.sh --list`.

O que praticar: bootstrap server, broker local, CLI do Kafka e diferenca entre acessar Kafka de fora do container e de dentro do container.

### 2.2 Criar topicos iniciais

- [ ] Criar o topic `orders.created`.
- [ ] Criar o topic `payments.processed`.
- [ ] Definir inicialmente 3 particoes para cada topic.

Por que fazer: em projeto real, topico tem nome de evento/fato de negocio. `orders.created` representa algo que ja aconteceu, nao um comando para outro servico.

### 2.3 Producer no order-service

- [ ] Adicionar dependencia Spring Kafka no `order-service`.
- [ ] Criar configuracao de producer.
- [ ] Implementar `OrderEventPublisher` com Kafka.
- [ ] Publicar `OrderCreatedEvent` no topic `orders.created`.
- [ ] Usar `orderId` como message key.

Como funciona: a key define a particao. Usar `orderId` garante que eventos do mesmo pedido tendem a manter ordem dentro da mesma particao.

### 2.4 Consumer no payment-service

- [ ] Adicionar dependencia Spring Kafka no `payment-service`.
- [ ] Criar listener para `orders.created`.
- [ ] Converter payload JSON para `OrderCreatedEvent`.
- [ ] Chamar `PaymentService.handleOrderCreated`.
- [ ] Publicar `PaymentProcessedEvent` em `payments.processed`.

O que Kafka esta fazendo: desacoplando `order-service` de `payment-service`. O pedido nao chama pagamento por HTTP; ele publica um fato e segue.

### 2.5 Consumer no notification-service

- [ ] Adicionar dependencia Spring Kafka no `notification-service`.
- [ ] Consumir `orders.created` para criar notificacao de pedido recebido.
- [ ] Consumir `payments.processed` para criar notificacao de pagamento aprovado/rejeitado.

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

- [ ] Criar `orders.created.dlt`.
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

- [ ] Mover nomes de topicos para `application.yaml`.
- [ ] Mover group ids para config.
- [ ] Separar profiles `local` e `test`.

Por que fazer: topico e group id mudam por ambiente. Hardcode e aceitavel no inicio, mas ruim como padrao.

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
