package br.edu.ucsal.ms_vendas.client;

import java.util.Map;

import org.springframework.web.client.RestClientResponseException;

/**
 * Lê o campo "mensagem" do corpo de erro devolvido por outro microsserviço.
 * Clientes, produtos e estoque usam esse mesmo nome de campo nas respostas de erro.
 */
final class ErroRemoto {

	private ErroRemoto() {
	}

	static String mensagem(RestClientResponseException e, String padrao) {
		try {
			Map<?, ?> corpo = e.getResponseBodyAs(Map.class);
			if (corpo != null && corpo.get("mensagem") instanceof String mensagem && !mensagem.isBlank()) {
				return mensagem;
			}
		} catch (RuntimeException ignorada) {
			// corpo vazio ou fora do formato esperado: usa a mensagem padrão
		}
		return padrao;
	}
}
