package br.edu.ucsal.ms_vendas.dto.response;

import java.math.BigDecimal;

import br.edu.ucsal.ms_vendas.model.ItemVenda;

public record ItemVendaResponse(
		Long produtoId,
		String produtoNome,
		Integer quantidade,
		BigDecimal precoUnitario,
		BigDecimal subtotal
		) {

	public static ItemVendaResponse converterParaDTO(ItemVenda item) {
		return new ItemVendaResponse(
				item.getProdutoId(),
				item.getProdutoNome(),
				item.getQuantidade(),
				item.getPrecoUnitario(),
				item.getSubtotal());
	}
}
