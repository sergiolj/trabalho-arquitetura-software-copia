package br.edu.ucsal.ms_vendas.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import br.edu.ucsal.ms_vendas.client.dto.ProdutoDTO;
import br.edu.ucsal.ms_vendas.exception.RecursoExternoNaoEncontradoException;
import br.edu.ucsal.ms_vendas.exception.ServicoIndisponivelException;
import lombok.extern.slf4j.Slf4j;

/** Comunicação HTTP com o ms-produtos (porta 8082). */
@Slf4j
@Component
public class ProdutoClient {

	private final RestClient restClient;

	public ProdutoClient(@Qualifier("produtosRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public ProdutoDTO buscarPorId(Long produtoId) {
		log.info("[ms-vendas -> ms-produtos] GET /api/produtos/{}", produtoId);
		try {
			return restClient.get()
					.uri("/api/produtos/{id}", produtoId)
					.retrieve()
					.body(ProdutoDTO.class);

		} catch (HttpClientErrorException.NotFound e) {
			throw new RecursoExternoNaoEncontradoException(
					"Produto " + produtoId + " não encontrado no ms-produtos");

		} catch (RestClientResponseException e) {
			throw new ServicoIndisponivelException(
					"ms-produtos respondeu com erro " + e.getStatusCode().value() + ": "
							+ ErroRemoto.mensagem(e, "sem detalhes"));

		} catch (ResourceAccessException e) {
			throw new ServicoIndisponivelException("ms-produtos está indisponível (sem resposta)");
		}
	}
}
