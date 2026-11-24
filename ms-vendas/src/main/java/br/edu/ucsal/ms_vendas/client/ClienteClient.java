package br.edu.ucsal.ms_vendas.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import br.edu.ucsal.ms_vendas.client.dto.ClienteDTO;
import br.edu.ucsal.ms_vendas.exception.RecursoExternoNaoEncontradoException;
import br.edu.ucsal.ms_vendas.exception.ServicoIndisponivelException;
import lombok.extern.slf4j.Slf4j;

/** Comunicação HTTP com o ms-clientes (porta 8084). */
@Slf4j
@Component
public class ClienteClient {

	private final RestClient restClient;

	public ClienteClient(@Qualifier("clientesRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public ClienteDTO buscarPorId(Long clienteId) {
		log.info("[ms-vendas -> ms-clientes] GET /api/clientes/{}", clienteId);
		try {
			return restClient.get()
					.uri("/api/clientes/{id}", clienteId)
					.retrieve()
					.body(ClienteDTO.class);

		} catch (HttpClientErrorException.NotFound e) {
			throw new RecursoExternoNaoEncontradoException(
					"Cliente " + clienteId + " não encontrado no ms-clientes");

		} catch (RestClientResponseException e) {
			throw new ServicoIndisponivelException(
					"ms-clientes respondeu com erro " + e.getStatusCode().value() + ": "
							+ ErroRemoto.mensagem(e, "sem detalhes"));

		} catch (ResourceAccessException e) {
			throw new ServicoIndisponivelException("ms-clientes está indisponível (sem resposta)");
		}
	}
}
