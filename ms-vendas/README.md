# Microsserviço de Vendas

Serviço REST responsável por registrar, consultar e cancelar vendas. É uma aplicação Spring Boot independente, executada na porta `8083`.

É também o **orquestrador** do sistema: para registrar uma venda, ele consulta o `ms-clientes` e o `ms-produtos` e dá baixa no `ms-estoque`, sempre por HTTP com `RestClient`. Ele nunca acessa as classes ou o banco de outro serviço.

## Tecnologias

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- H2
- Bean Validation
- `RestClient` (comunicação entre serviços)
- Springdoc OpenAPI
- Maven

## Executar

O `ms-vendas` depende dos outros três serviços para registrar e cancelar vendas. Inicie-os antes, cada um em um terminal:

```powershell
cd ms-clientes;  .\mvnw.cmd spring-boot:run   # porta 8084
cd ms-produtos;  .\mvnw.cmd spring-boot:run   # porta 8082
cd ms-estoque;   .\mvnw.cmd spring-boot:run   # porta 8081
```

Depois, na raiz do repositório:

```powershell
cd ms-vendas
.\mvnw.cmd spring-boot:run
```

Também é possível usar Maven instalado:

```powershell
mvn spring-boot:run
```

Com o serviço iniciado:

- API base: `http://localhost:8083/api/vendas`
- Swagger UI: `http://localhost:8083/swagger-ui.html`
- Console H2: `http://localhost:8083/h2-console`

Para acessar o H2 Console, use a URL JDBC `jdbc:h2:mem:vendas`, usuário `sa` e senha em branco.

As URLs dos outros serviços ficam no `application.properties`:

```properties
servicos.clientes.url=http://localhost:8084
servicos.produtos.url=http://localhost:8082
servicos.estoque.url=http://localhost:8081
servicos.timeout.conexao=2s
servicos.timeout.leitura=5s
```

## Comunicação com os outros microsserviços

| Passo | Chamada | Para quê |
| --- | --- | --- |
| 1 | `GET ms-clientes /api/clientes/{id}` | Confirma que o cliente existe e copia nome e CPF/CNPJ para a venda. |
| 2 | `GET ms-produtos /api/produtos/{id}` (um por item) | Confirma que o produto existe e copia nome e preço atual. |
| 3 | `POST ms-estoque /estoque/saidas` (um por item) | Dá baixa na quantidade vendida. O `ms-estoque` responde `409` se faltar estoque. |
| 4 | Banco do `ms-vendas` | Grava a venda com os itens e o valor total. |

**Compensação (Saga orquestrada).** Cada serviço tem seu próprio banco, então não existe uma transação única entre eles. Se um item não tiver estoque ou a gravação da venda falhar, o `ms-vendas` devolve ao estoque os itens que já tinham sido baixados (`POST /estoque/entradas`). No cancelamento acontece o inverso: os itens voltam ao estoque e, se o `ms-estoque` falhar no meio, as devoluções feitas são desfeitas e a venda continua `CONCLUIDA`.

Cada chamada aparece no console do `ms-vendas`, o que ajuda a acompanhar a orquestração durante a demonstração:

```text
[ms-vendas -> ms-clientes] GET /api/clientes/2
[ms-vendas -> ms-produtos] GET /api/produtos/1
[ms-vendas -> ms-produtos] GET /api/produtos/3
[ms-vendas -> ms-estoque] POST /estoque/saidas produto=1 quantidade=4
[ms-vendas -> ms-estoque] POST /estoque/saidas produto=3 quantidade=10
Falha ao registrar a venda (Estoque insuficiente para o produto 3 ...). Estornando 1 baixa(s) de estoque.
[ms-vendas -> ms-estoque] POST /estoque/entradas produto=1 quantidade=4
```

## Modelo de dados e validação

Requisição para registrar uma venda:

| Campo | Tipo JSON | Regras |
| --- | --- | --- |
| `clienteId` | número | Obrigatório e positivo; precisa existir no `ms-clientes`. |
| `itens` | lista | Obrigatória, com pelo menos um item; o mesmo produto não pode aparecer duas vezes. |
| `itens[].produtoId` | número | Obrigatório e positivo; precisa existir no `ms-produtos`. |
| `itens[].quantidade` | número inteiro | Obrigatória e maior que zero; precisa haver saldo no `ms-estoque`. |

Uma venda registrada guarda, além dos IDs, uma cópia dos dados no momento da venda (nome do cliente, CPF/CNPJ, nome e preço de cada produto). Se o preço do produto mudar depois, a venda antiga não muda.

### Rotas implementadas

| Método | Rota | Resultado esperado |
| --- | --- | --- |
| `GET` | `/api/vendas` | Lista as vendas (`200 OK`). |
| `GET` | `/api/vendas?clienteId={id}` | Lista as vendas de um cliente (`200 OK`). |
| `GET` | `/api/vendas/{id}` | Consulta uma venda (`200 OK`, ou `404 Not Found`). |
| `POST` | `/api/vendas` | Registra uma venda orquestrando os outros serviços (`201 Created`, com cabeçalho `Location`). |
| `PATCH` | `/api/vendas/{id}/cancelamento` | Cancela a venda e devolve os itens ao estoque (`200 OK`). |

Venda não tem `PUT` nem `DELETE`: uma venda concluída não é editada nem apagada, ela é cancelada e continua no histórico com status `CANCELADA`.

### Exemplo de venda

Requisição:

```json
{
  "clienteId": 1,
  "itens": [
    { "produtoId": 1, "quantidade": 2 },
    { "produtoId": 2, "quantidade": 3 }
  ]
}
```

Resposta `201 Created`:

```json
{
  "id": 1,
  "clienteId": 1,
  "clienteNome": "Tony Stark",
  "clienteDocumento": "22211133345",
  "dataVenda": "2026-11-24T11:13:34.063Z",
  "status": "CONCLUIDA",
  "dataCancelamento": null,
  "valorTotal": 17075000.00,
  "itens": [
    { "produtoId": 1, "produtoNome": "Armadura Mark V", "quantidade": 2, "precoUnitario": 8500000.00, "subtotal": 17000000.00 },
    { "produtoId": 2, "produtoNome": "Escudo do Capitao America", "quantidade": 3, "precoUnitario": 25000.00, "subtotal": 75000.00 }
  ]
}
```

## Respostas de erro

Os erros tratados pela API usam o mesmo formato dos outros serviços:

```json
{
  "dataHora": "2026-11-24T11:13:34.313Z",
  "status": 422,
  "erro": "Referência inválida.",
  "mensagem": "Cliente 99 não encontrado no ms-clientes",
  "caminho": "/api/vendas"
}
```

| Código | Quando ocorre |
| --- | --- |
| `400 Bad Request` | Campos inválidos, lista de itens vazia, produto repetido ou JSON malformado. |
| `404 Not Found` | Venda não encontrada no `ms-vendas`. |
| `409 Conflict` | Estoque insuficiente, produto sem estoque cadastrado ou cancelamento de venda já cancelada. |
| `422 Unprocessable Content` | O cliente ou um produto informado não existe no serviço dono desse dado. |
| `503 Service Unavailable` | `ms-clientes`, `ms-produtos` ou `ms-estoque` fora do ar, sem resposta dentro do timeout ou com erro interno. |
| `500 Internal Server Error` | Erro inesperado; a resposta não expõe detalhes internos. |

## Testes automatizados

```powershell
.\mvnw.cmd test
```

- `VendaServiceTest`: testa o orquestrador com Mockito, sem nenhum outro serviço rodando (ordem das chamadas, estorno quando falta estoque, estorno quando a gravação falha, cancelamento).
- `EstoqueClientTest`: simula respostas do `ms-estoque` com `MockRestServiceServer` (sucesso, `409`, `404`, `500` e serviço fora do ar).

## Persistência e dados de demonstração

O serviço usa H2 em memória (`jdbc:h2:mem:vendas`). Os dados são perdidos quando o processo é encerrado. Não há carga de dados de demonstração: as vendas são criadas pelo `POST /api/vendas`, usando os clientes, produtos e estoques de demonstração dos outros serviços.

## Organização do código

- `controller`: endpoints REST e códigos HTTP.
- `service`: `VendaService`, o orquestrador da venda e do cancelamento.
- `client`: um client HTTP por serviço consumido (`ClienteClient`, `ProdutoClient`, `EstoqueClient`), que traduz as respostas HTTP em exceções do `ms-vendas`.
- `client.dto`: cópias dos contratos JSON dos outros serviços.
- `config`: `RestClientConfig`, com as URLs e os timeouts de cada serviço.
- `repository`: acesso aos dados via Spring Data JPA.
- `model`: entidades persistidas `Venda` e `ItemVenda`.
- `dto.request` e `dto.response`: contratos de entrada e saída da API.
- `exception`: exceções da orquestração, tratamento global e formato das respostas de erro.
- `util`: enum `StatusVendaEnum`.
