# ☕ Facoffee - Serviço de Finance

Este repositório contém o código-fonte, suíte de testes e documentação do **Serviço de Finance, componente integrante do ecossistema de microsserviços **Facoffee**.

O ecossistema é projetado sob os pilares de:

- Arquitetura Orientada a Eventos (EDA)
- Descentralização de Dados
- Segurança Centralizada via API Gateway
- Controle de Identidade Federado utilizando Keycloak (OAuth2/OIDC)

---

# 🏛️ Visão Geral da Arquitetura

O ecossistema Facoffee opera através de um fluxo distribuído e blindado por segurança em camadas:

## 1. API Gateway (Nginx)

Funciona como ponto único de entrada (**porta 8000**), gerenciando:

- Regras de CORS
- Roteamento inteligente de tráfego
- Validação de sessão utilizando a diretiva `auth_request`
- Integração com o endpoint `/userinfo` do Keycloak

## 2. Provedor de Identidade (Keycloak)

Responsável por:

- Gerenciamento de usuários
- Políticas de acesso
- Emissão de JWTs assinados com RS256

## 3. Mensageria (Apache Kafka)

Responsável pela comunicação assíncrona entre os microsserviços, propagando eventos de domínio sempre que ocorrem alterações relevantes de estado.

## 4. Microsserviços (Spring Boot)

### Finance Service
- Porta: `3003`

Cada serviço:

- Possui banco de dados isolado
- Implementa filtros locais de segurança (`JwtAuthenticationFilter`)
- Realiza autorizações granulares com `@PreAuthorize`

---

# 🛠️ Tecnologias Utilizadas

## Linguagem

- Java 21

## Framework

- Spring Boot 3.x

### Dependências Spring

- Spring Security (OAuth2 Resource Server)
- Spring Data JPA
- Spring for Apache Kafka

## Banco de Dados

- PostgreSQL

## Mensageria

- Apache Kafka

## Infraestrutura

- Docker
- Docker Compose

## Segurança

- JJWT (Java JWT)

---

# 🚀 Como Executar o Ecossistema

## 1. Pré-requisitos

Certifique-se de possuir:

- Git
- JDK instalado e configurado
- Docker
- Docker Compose

---

## 2. Subir a Infraestrutura

Na raiz do projeto principal:

```bash
docker compose up -d
```

Esse comando iniciará:

- Keycloak
- Kafka
- Nginx
- Bancos de Dados
- Demais serviços auxiliares

---

## 3. Executar o Serviço Participation

### Maven

```bash
./mvnw spring-boot:run
```

### Gradle

```bash
./gradlew bootRun
```

Após a inicialização, o serviço estará integrado ao ecossistema.

---

# 🔐 Guia de Autenticação e Autorização

O API Gateway utiliza validação baseada em OpenID Connect.

Para acessar qualquer endpoint protegido através da porta pública `8000`, é necessário gerar um token válido.

---

## Geração do Token de Usuário (Client Público)

Fluxo utilizado para simular um usuário autenticado.

```bash
curl -X POST "http://localhost:8080/realms/facoffee/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=facoffee-public" \
  -d "username=facoffee@facom.ufms.br" \
  -d "password=facoffee" \
  -d "scope=openid roles"
```

### Observação

A inclusão explícita dos escopos:

```text
openid roles
```

é obrigatória.

Motivos:

- `openid` permite a validação no endpoint `/userinfo`
- `roles` força a inclusão da claim:

```json
realm_access.roles
```

possibilitando a autorização correta dentro da aplicação Spring Boot.

---

## Geração de Token de Serviço (Client Privado)

Fluxo destinado à comunicação máquina-para-máquina (M2M).

```bash
curl -X POST "http://localhost:8080/realms/facoffee/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=facoffee-private" \
  -d "client_secret=facoffee-private-secret"
```

---

# 📡 Exemplos de Uso da API

Todas as chamadas devem ser realizadas através do Gateway:

```text
http://localhost:8000
```

e devem incluir:

```http
Authorization: Bearer <ACCESS_TOKEN>
```

---

## Criar Participação

### Endpoint

```http
POST /api/participation
```

### URL

```text
http://localhost:8000/api/participation
```

### Headers

```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

### Body

```json
{
  "userId": "3d50189c-9b67-41d4-af36-7da5226a1bf5",
  "eventId": 101,
  "status": "CONFIRMED"
}
```

---

## Listar Participações

### Endpoint

```http
GET /api/participation
```

### URL

```text
http://localhost:8000/api/participation
```

### Headers

```http
Authorization: Bearer <ACCESS_TOKEN>
```

---

# 🧪 Testes Automatizados

O microsserviço possui cobertura completa de testes para validar:

- Regras de negócio
- Persistência de dados
- Segurança das rotas
- Controle de acesso baseado em roles

Como as chaves RSA utilizadas pelo Keycloak mudam dinamicamente em tempo de execução, os testes utilizam **JWT Mocking**, eliminando a necessidade de executar o Keycloak durante os testes.

---

## Executar Testes

### Maven

```bash
./mvnw test
```

### Gradle

```bash
./gradlew test
```

---

# 🔒 Exemplo de Teste de Segurança

Utilizando MockMvc com JWT Mock:

```java
@Test
@DisplayName("Deve permitir acesso à rota de criação quando o usuário for MANAGER")
void deveCriarParticipacaoComSucesso() throws Exception {

    mockMvc.perform(post("/api/participation")
            .with(SecurityMockMvcConfigurers.mockJwt()
                    .jwt(jwt -> jwt.claim(
                        "preferred_username",
                        "facoffee@facom.ufms.br"
                    ))
                    .authorities(
                        new SimpleGrantedAuthority("ROLE_MANAGER")
                    ))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "userId":"123",
                    "eventId":1,
                    "status":"CONFIRMED"
                }
            """))
            .andExpect(status().isCreated());
}
```

---

# 📌 Características do Serviço

- Arquitetura baseada em eventos (EDA)
- Segurança centralizada via API Gateway
- Autenticação federada com Keycloak
- Comunicação assíncrona via Kafka
- Banco de dados isolado por serviço
- Controle granular de permissões com Spring Security
- Testes automatizados independentes do Keycloak
- Integração completa com Docker Compose

---
