package br.edu.ucsal.ms_vendas.client.dto;

/** Corpo enviado ao ms-estoque para registrar uma saída (baixa) ou entrada (estorno). */
public record MovimentacaoEstoqueDTO(Long produtoId, Integer quantidade) {
}
