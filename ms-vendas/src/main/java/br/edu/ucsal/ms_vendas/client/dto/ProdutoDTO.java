package br.edu.ucsal.ms_vendas.client.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Cópia do contrato de resposta do ms-produtos (GET /api/produtos/{id}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProdutoDTO(Long id, String nome, String descricao, BigDecimal preco) {
}
