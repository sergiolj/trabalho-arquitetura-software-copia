package br.edu.ucsal.ms_vendas.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Cópia do contrato de resposta do ms-clientes (GET /api/clientes/{id}).
 * Não é a classe do outro serviço: cada microsserviço tem a sua própria versão do contrato.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClienteDTO(Long id, String nome, String email, String tipo, String idReceitaFederal) {
}
