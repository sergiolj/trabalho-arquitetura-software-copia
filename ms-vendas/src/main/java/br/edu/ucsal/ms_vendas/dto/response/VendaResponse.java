package br.edu.ucsal.ms_vendas.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.edu.ucsal.ms_vendas.model.Venda;

public record VendaResponse(
		Long id,
		Long clienteId,
		String clienteNome,
		String clienteDocumento,
		Instant dataVenda,
		String status,
		Instant dataCancelamento,
		BigDecimal valorTotal,
		List<ItemVendaResponse> itens
		) {

	public static VendaResponse converterParaDTO(Venda venda) {
		return new VendaResponse(
				venda.getId(),
				venda.getClienteId(),
				venda.getClienteNome(),
				venda.getClienteDocumento(),
				venda.getDataVenda(),
				venda.getStatus().name(),
				venda.getDataCancelamento(),
				venda.getValorTotal(),
				venda.getItens().stream().map(ItemVendaResponse::converterParaDTO).toList());
	}
}
