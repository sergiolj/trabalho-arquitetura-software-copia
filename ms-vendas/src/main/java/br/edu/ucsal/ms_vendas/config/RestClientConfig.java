package br.edu.ucsal.ms_vendas.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cria um RestClient para cada microsserviço que o ms-vendas consome.
 *
 * As URLs ficam no application.properties (servicos.*.url), então nenhuma porta
 * fica fixa no código. Os timeouts evitam que uma venda fique "pendurada"
 * esperando um serviço que travou.
 */
@Configuration
public class RestClientConfig {

	@Value("${servicos.timeout.conexao}")
	private Duration timeoutConexao;

	@Value("${servicos.timeout.leitura}")
	private Duration timeoutLeitura;

	@Bean
	RestClient clientesRestClient(@Value("${servicos.clientes.url}") String url) {
		return criarRestClient(url);
	}

	@Bean
	RestClient produtosRestClient(@Value("${servicos.produtos.url}") String url) {
		return criarRestClient(url);
	}

	@Bean
	RestClient estoqueRestClient(@Value("${servicos.estoque.url}") String url) {
		return criarRestClient(url);
	}

	private RestClient criarRestClient(String baseUrl) {
		SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(timeoutConexao);
		fabrica.setReadTimeout(timeoutLeitura);

		return RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(fabrica)
				.build();
	}
}
