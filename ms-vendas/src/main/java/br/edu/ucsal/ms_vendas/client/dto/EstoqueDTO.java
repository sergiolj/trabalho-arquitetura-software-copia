package br.edu.ucsal.ms_vendas.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Cópia do contrato de resposta do ms-estoque (POST /estoque/saidas e /estoque/entradas). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EstoqueDTO(Long id, Long produtoId, Integer quantidade) {
}
