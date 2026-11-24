package br.edu.ucsal.ms_vendas.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import br.edu.ucsal.ms_vendas.client.dto.EstoqueDTO;
import br.edu.ucsal.ms_vendas.exception.EstoqueInsuficienteException;
import br.edu.ucsal.ms_vendas.exception.ServicoIndisponivelException;

/**
 * Testa a tradução das respostas HTTP do ms-estoque em exceções do ms-vendas,
 * sem precisar do ms-estoque rodando: o MockRestServiceServer simula as respostas.
 */
class EstoqueClientTest {

	private MockRestServiceServer servidor;
	private EstoqueClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://estoque");
		servidor = MockRestServiceServer.bindTo(builder).build();
		client = new EstoqueClient(builder.build());
	}

	@Test
	void baixaComSucessoEnviaProdutoEQuantidade() {
		servidor.expect(requestTo("http://estoque/estoque/saidas"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json("{\"produtoId\":1,\"quantidade\":2}"))
				.andRespond(withSuccess("{\"id\":1,\"produtoId\":1,\"quantidade\":48}", MediaType.APPLICATION_JSON));

		EstoqueDTO estoque = client.registrarSaida(1L, 2);

		assertThat(estoque.quantidade()).isEqualTo(48);
		servidor.verify();
	}

	@Test
	void conflitoNoEstoqueViraEstoqueInsuficienteComAMensagemDoOutroServico() {
		servidor.expect(requestTo("http://estoque/estoque/saidas"))
				.andRespond(withStatus(HttpStatus.CONFLICT)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"status\":409,\"mensagem\":\"Estoque insuficiente para o produto 3\"}"));

		assertThatThrownBy(() -> client.registrarSaida(3L, 10))
				.isInstanceOf(EstoqueInsuficienteException.class)
				.hasMessage("Estoque insuficiente para o produto 3");
	}

	@Test
	void produtoSemEstoqueCadastradoTambemEhEstoqueInsuficiente() {
		servidor.expect(requestTo("http://estoque/estoque/saidas"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> client.registrarSaida(4L, 1))
				.isInstanceOf(EstoqueInsuficienteException.class)
				.hasMessageContaining("não possui estoque cadastrado");
	}

	@Test
	void erroInternoNoEstoqueViraServicoIndisponivel() {
		servidor.expect(requestTo("http://estoque/estoque/saidas"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatThrownBy(() -> client.registrarSaida(1L, 1))
				.isInstanceOf(ServicoIndisponivelException.class);
	}

	@Test
	void estoqueForaDoArViraServicoIndisponivel() {
		servidor.expect(requestTo("http://estoque/estoque/entradas"))
				.andRespond(withException(new IOException("Connection refused")));

		assertThatThrownBy(() -> client.registrarEntrada(1L, 1))
				.isInstanceOf(ServicoIndisponivelException.class)
				.hasMessageContaining("indisponível");
	}
}
