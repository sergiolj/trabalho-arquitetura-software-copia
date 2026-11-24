package br.edu.ucsal.ms_vendas.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import br.edu.ucsal.ms_vendas.client.dto.EstoqueDTO;
import br.edu.ucsal.ms_vendas.client.dto.MovimentacaoEstoqueDTO;
import br.edu.ucsal.ms_vendas.exception.EstoqueInsuficienteException;
import br.edu.ucsal.ms_vendas.exception.ServicoIndisponivelException;
import lombok.extern.slf4j.Slf4j;

/** Comunicação HTTP com o ms-estoque (porta 8081). */
@Slf4j
@Component
public class EstoqueClient {

	private final RestClient restClient;

	public EstoqueClient(@Qualifier("estoqueRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	/** Baixa no estoque. O ms-estoque responde 409 se não houver quantidade suficiente. */
	public EstoqueDTO registrarSaida(Long produtoId, Integer quantidade) {
		log.info("[ms-vendas -> ms-estoque] POST /estoque/saidas produto={} quantidade={}", produtoId, quantidade);
		try {
			return restClient.post()
					.uri("/estoque/saidas")
					.body(new MovimentacaoEstoqueDTO(produtoId, quantidade))
					.retrieve()
					.body(EstoqueDTO.class);

		} catch (HttpClientErrorException.Conflict e) {
			throw new EstoqueInsuficienteException(
					ErroRemoto.mensagem(e, "Estoque insuficiente para o produto " + produtoId));

		} catch (HttpClientErrorException.NotFound e) {
			throw new EstoqueInsuficienteException(
					"O produto " + produtoId + " não possui estoque cadastrado");

		} catch (RestClientResponseException e) {
			throw new ServicoIndisponivelException(
					"ms-estoque respondeu com erro " + e.getStatusCode().value() + ": "
							+ ErroRemoto.mensagem(e, "sem detalhes"));

		} catch (ResourceAccessException e) {
			throw new ServicoIndisponivelException("ms-estoque está indisponível (sem resposta)");
		}
	}

	/** Devolve quantidade ao estoque. Usado no estorno (compensação) e no cancelamento da venda. */
	public EstoqueDTO registrarEntrada(Long produtoId, Integer quantidade) {
		log.info("[ms-vendas -> ms-estoque] POST /estoque/entradas produto={} quantidade={}", produtoId, quantidade);
		try {
			return restClient.post()
					.uri("/estoque/entradas")
					.body(new MovimentacaoEstoqueDTO(produtoId, quantidade))
					.retrieve()
					.body(EstoqueDTO.class);

		} catch (RestClientResponseException e) {
			throw new ServicoIndisponivelException(
					"ms-estoque respondeu com erro " + e.getStatusCode().value() + ": "
							+ ErroRemoto.mensagem(e, "sem detalhes"));

		} catch (ResourceAccessException e) {
			throw new ServicoIndisponivelException("ms-estoque está indisponível (sem resposta)");
		}
	}
}
