# 🏢 ArremateAI - Vendor Service

![CI](https://github.com/Quintanilha09/arremateai-vendor/actions/workflows/ci.yml/badge.svg)

Microsserviço responsável pelo cadastro de vendedores Pessoa Jurídica (PJ), validação de CNPJ, workflow de documentos e processo de aprovação administrativa.

## 📋 Descrição

O Vendor Service gerencia todo o ciclo de vida de vendedores PJ no ArremateAI:

- **Cadastro de vendedores PJ** com validação de CNPJ
- **Integração com ReceitaWS** para dados empresariais
- **Workflow de documentos** (CNPJ, contrato social, documentos pessoais)
- **Processo de aprovação** em múltiplas etapas
- **Validação de email corporativo**
- **Histórico de status** (auditoria completa)
- **Gestão administrativa** (aprovar/rejeitar/suspender)

## 🛠️ Tecnologias

- **Java 17** (LTS)
- **Spring Boot 3.2.2**
- **Spring Data JPA** - Persistência
- **PostgreSQL 16** - Banco de dados
- **WebFlux** - Cliente HTTP reativo
- **OpenFeign** - Cliente HTTP declarativo (ReceitaWS)
- **Spring Mail** - Notificações por email
- **Flyway** - Migrations
- **Validation API** - Validação de dados

## 🏗️ Arquitetura

```
┌──────────────────┐
│  Gateway :8080   │
└────────┬─────────┘
         │
         ▼
┌─────────────────────────────────┐
│     Vendor Service              │
│       (Port 8083)               │
├─────────────────────────────────┤
│ Controllers                     │
│  ├─ VendedorController          │
│  └─ AdminVendedorController     │
├─────────────────────────────────┤
│ Services                        │
│  ├─ VendedorService             │
│  ├─ CnpjValidationService       │
│  ├─ DocumentStorageService      │
│  └─ AdminVendedorService        │
├─────────────────────────────────┤
│ Validators                      │
│  └─ EmailCorporativoValidator   │
└────────┬──────────┬─────────────┘
         │          │
         ▼          ▼
    PostgreSQL   ReceitaWS API
     (5435)    (externa)
```

## 📦 Estrutura do Projeto

```
src/main/java/com/arremateai/vendor/
├── VendorApplication.java
├── controller/
│   ├── VendedorController.java           # Endpoints vendedor
│   └── AdminVendedorController.java      # Endpoints admin
├── domain/
│   ├── DocumentoVendedor.java            # Entidade documentos
│   ├── HistoricoStatusVendedor.java      # Auditoria de status
│   ├── Usuario.java                      # Dados do vendedor
│   ├── StatusVendedor.java               # Enum status
│   ├── TipoDocumento.java                # Enum tipos de documento
│   └── StatusDocumento.java              # Enum status documento
├── dto/
│   ├── CadastroVendedorRequest.java
│   ├── DocumentoVendedorResponse.java
│   ├── AprovarVendedorRequest.java
│   ├── RejeitarVendedorRequest.java
│   ├── CnpjResponseDTO.java              # Resposta ReceitaWS
│   └── HistoricoResponse.java
├── repository/
│   ├── DocumentoVendedorRepository.java
│   ├── HistoricoStatusVendedorRepository.java
│   └── UsuarioRepository.java
├── service/
│   ├── VendedorService.java              # Lógica principal
│   ├── CnpjValidationService.java        # Validação ReceitaWS
│   ├── DocumentStorageService.java       # Upload de documentos
│   ├── AdminVendedorService.java         # Aprovação/rejeição
│   ├── EmailService.java                 # Notificações
│   └── VerificacaoService.java           # Validações
├── validator/
│   └── EmailCorporativoValidator.java    # Valida email corporativo
└── exception/
    ├── BusinessException.java
    └── GlobalExceptionHandler.java
```

## 🚀 Endpoints Principais

### Cadastro de Vendedor

#### POST `/api/vendedores/cadastro`
Inicia cadastro de vendedor PJ (Etapa 1: Dados básicos).

**Request:**
```json
{
  "cnpj": "12345678000190",
  "razaoSocial": "Imobiliária XYZ Ltda",
  "nomeFantasia": "Imobiliária XYZ",
  "email": "contato@imobiliariaxyz.com.br",
  "telefone": "+5511999999999",
  "responsavel": {
    "nome": "João Silva",
    "cpf": "12345678900",
    "email": "joao@imobiliariaxyz.com.br",
    "cargo": "Diretor"
  },
  "endereco": {
    "cep": "01310-100",
    "rua": "Av Paulista",
    "numero": "1000",
    "complemento": "Sala 1001",
    "bairro": "Bela Vista",
    "cidade": "São Paulo",
    "estado": "SP"
  }
}
```

**Response 201:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "cnpj": "12345678000190",
  "razaoSocial": "Imobiliária XYZ Ltda",
  "email": "contato@imobiliariaxyz.com.br",
  "statusVendedor": "PENDENTE_DOCUMENTOS",
  "proximoPasso": "Enviar documentos obrigatórios",
  "documentosPendentes": [
    "CARTAO_CNPJ",
    "CONTRATO_SOCIAL",
    "COMPROVANTE_ENDERECO",
    "DOCUMENTO_RESPONSAVEL"
  ]
}
```

### Upload de Documentos

#### POST `/api/vendedores/{vendedorId}/documentos`
Upload de documento (Etapa 2).

**Headers:**
```
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

**Request (Form Data):**
```
tipoDocumento: CARTAO_CNPJ
arquivo: [binary file]
```

**Response 200:**
```json
{
  "id": "doc-uuid",
  "tipoDocumento": "CARTAO_CNPJ",
  "status": "PENDENTE_APROVACAO",
  "nomeArquivo": "cartao_cnpj.pdf",
  "url": "https://storage.arremateai.com/documentos/vendedor-uuid/cartao_cnpj.pdf",
  "uploadedAt": "2026-03-27T10:30:00Z"
}
```

#### GET `/api/vendedores/{vendedorId}/documentos`
Lista documentos do vendedor.

**Response 200:**
```json
{
  "vendedorId": "550e8400-e29b-41d4-a716-446655440000",
  "documentos": [
    {
      "id": "doc-001",
      "tipoDocumento": "CARTAO_CNPJ",
      "status": "APROVADO",
      "uploadedAt": "2026-03-27T10:30:00Z"
    },
    {
      "id": "doc-002",
      "tipoDocumento": "CONTRATO_SOCIAL",
      "status": "PENDENTE_APROVACAO",
      "uploadedAt": "2026-03-27T11:00:00Z"
    }
  ],
  "documentosPendentes": [
    "COMPROVANTE_ENDERECO",
    "DOCUMENTO_RESPONSAVEL"
  ],
  "progresso": 50
}
```

### Validação de CNPJ

#### GET `/api/vendedores/cnpj/{cnpj}`
Valida CNPJ na Receita Federal (ReceitaWS).

**Response 200:**
```json
{
  "cnpj": "12345678000190",
  "razaoSocial": "Imobiliária XYZ Ltda",
  "nomeFantasia": "Imobiliária XYZ",
  "situacao": "ATIVA",
  "dataSituacao": "2020-01-15",
  "naturezaJuridica": "Sociedade Empresária Limitada",
  "capitalSocial": 100000.00,
  "atividades": [
    {
      "codigo": "6821-8/01",
      "descricao": "Corretagem na compra e venda de imóveis",
      "principal": true
    }
  ],
  "endereco": {
    "logradouro": "Av Paulista",
    "numero": "1000",
    "complemento": "Sala 1001",
    "bairro": "Bela Vista",
    "municipio": "São Paulo",
    "uf": "SP",
    "cep": "01310-100"
  },
  "valido": true
}
```

### Administração

#### GET `/api/admin/vendedores/pendentes`
Lista vendedores pendentes de aprovação (apenas ADMIN).

**Headers:**
```
Authorization: Bearer {admin_token}
```

**Response 200:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "razaoSocial": "Imobiliária XYZ Ltda",
    "cnpj": "12345678000190",
    "email": "contato@imobiliariaxyz.com.br",
    "statusVendedor": "PENDENTE_APROVACAO",
    "documentosEnviados": 4,
    "documentosAprovados": 3,
    "dataCadastro": "2026-03-20T10:00:00Z"
  }
]
```

#### POST `/api/admin/vendedores/{vendedorId}/aprovar`
Aprovar vendedor (apenas ADMIN).

**Request:**
```json
{
  "observacoes": "Documentação completa e validada",
  "aprovarTodosDocumentos": true
}
```

**Response 200:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "statusVendedor": "APROVADO",
  "aprovadoEm": "2026-03-27T10:30:00Z",
  "aprovadoPor": "admin@arremateai.com",
  "mensagem": "Vendedor aprovado com sucesso. Email de confirmação enviado."
}
```

#### POST `/api/admin/vendedores/{vendedorId}/rejeitar`
Rejeitar vendedor (apenas ADMIN).

**Request:**
```json
{
  "motivo": "Documentos com inconsistências",
  "detalhes": "Contrato social não corresponde ao CNPJ informado",
  "documentosRejeitados": [
    "CONTRATO_SOCIAL"
  ]
}
```

#### POST `/api/admin/vendedores/{vendedorId}/suspender`
Suspender vendedor ativo (apenas ADMIN).

**Request:**
```json
{
  "motivo": "Violação das políticas de uso",
  "tempoSuspensao": 30
}
```

### Histórico

#### GET `/api/vendedores/{vendedorId}/historico`
Histórico completo de mudanças de status.

**Response 200:**
```json
[
  {
    "id": "hist-001",
    "statusAnterior": null,
    "statusNovo": "PENDENTE_DOCUMENTOS",
    "motivoMudanca": "Cadastro inicial",
    "realizadoPor": "vendedor@example.com",
    "dataHora": "2026-03-20T10:00:00Z"
  },
  {
    "id": "hist-002",
    "statusAnterior": "PENDENTE_DOCUMENTOS",
    "statusNovo": "PENDENTE_APROVACAO",
    "motivoMudanca": "Todos documentos enviados",
    "realizadoPor": "vendedor@example.com",
    "dataHora": "2026-03-22T15:30:00Z"
  },
  {
    "id": "hist-003",
    "statusAnterior": "PENDENTE_APROVACAO",
    "statusNovo": "APROVADO",
    "motivoMudanca": "Documentação completa e validada",
    "realizadoPor": "admin@arremateai.com",
    "dataHora": "2026-03-27T10:30:00Z"
  }
]
```

## 🔄 Workflow de Status

```
┌──────────────────────┐
│  CADASTRO INICIAL    │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ PENDENTE_DOCUMENTOS  │ ← Vendedor envia documentos
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ PENDENTE_APROVACAO   │ ← Admin revisa
└──────────┬───────────┘
           │
      ┌────┴────┐
      ▼         ▼
┌─────────┐  ┌──────────┐
│APROVADO │  │REJEITADO │
└────┬────┘  └────┬─────┘
     │            │
     │            └──→ Pode reenviar documentos
     │
     ▼
┌──────────┐
│SUSPENSO  │ ← Admin pode suspender vendedor ativo
└──────────┘
```

## 📋 Tipos de Documentos Obrigatórios

| Tipo | Descrição | Formato Aceito |
|------|-----------|----------------|
| `CARTAO_CNPJ` | Cartão CNPJ atualizado | PDF |
| `CONTRATO_SOCIAL` | Contrato social ou alteração consolidada | PDF |
| `COMPROVANTE_ENDERECO` | Conta de água/luz empresa (máx 3 meses) | PDF, JPG, PNG |
| `DOCUMENTO_RESPONSAVEL` | RG/CNH do responsável legal | PDF, JPG, PNG |
| `PROCURACAO` | Caso representante legal (opcional) | PDF |

## ⚙️ Variáveis de Ambiente

```bash
# Server
SERVER_PORT=8083

# Database
DB_HOST=localhost
DB_PORT=5435
DB_NAME=vendor_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# ReceitaWS API
RECEITA_WS_URL=https://www.receitaws.com.br/v1
RECEITA_WS_TIMEOUT=5000
RECEITA_WS_RETRY_ATTEMPTS=3

# Document Storage
STORAGE_TYPE=S3
AWS_S3_BUCKET=arremateai-vendor-documents
AWS_S3_REGION=us-east-1
STORAGE_BASE_URL=https://storage.arremateai.com

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@arremateai.com
MAIL_PASSWORD=senha_app

# Validation
EMAIL_CORPORATIVO_REQUIRED=true
CNPJ_VALIDATION_ENABLED=true
```

## 🏃 Como Executar

```bash
# Clone o repositório
git clone https://github.com/Quintanilha09/arremateai-vendor.git
cd arremateai-vendor

# Suba o banco de dados
docker-compose up -d postgres

# Execute as migrations
./mvnw flyway:migrate

# Execute a aplicação
./mvnw spring-boot:run
```

## 📊 Banco de Dados

### `documento_vendedor`
```sql
CREATE TABLE documento_vendedor (
    id UUID PRIMARY KEY,
    vendedor_id UUID NOT NULL,
    tipo_documento VARCHAR(50) NOT NULL,
    status_documento VARCHAR(50) NOT NULL,
    nome_arquivo VARCHAR(255),
    url VARCHAR(500),
    observacoes TEXT,
    uploaded_at TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_documento_vendedor_id ON documento_vendedor(vendedor_id);
CREATE INDEX idx_documento_status ON documento_vendedor(status_documento);
```

### `historico_status_vendedor`
```sql
CREATE TABLE historico_status_vendedor (
    id UUID PRIMARY KEY,
    vendedor_id UUID NOT NULL,
    status_anterior VARCHAR(50),
    status_novo VARCHAR(50) NOT NULL,
    motivo_mudanca TEXT,
    observacoes TEXT,
    realizado_por UUID,
    data_hora TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_historico_vendedor ON historico_status_vendedor(vendedor_id);
```

## 🧪 Testes

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify

# Coverage
./mvnw jacoco:report
```

## 🔧 Integração com ReceitaWS

O serviço utiliza a API pública **ReceitaWS** para validação de CNPJ:

```java
@FeignClient(name = "receitaws", url = "${receita-ws.url}")
public interface ReceitaWSClient {
    
    @GetMapping("/cnpj/{cnpj}")
    CnpjResponseDTO consultarCnpj(@PathVariable String cnpj);
}
```

**Observações:**
- API gratuita com rate limit
- Fallback para cache se API indisponível
- Retry automático (3 tentativas)

## 📄 Licença

Proprietary - © 2026 ArremateAI
