package br.edu.ucsal.ms_vendas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ucsal.ms_vendas.client.ClienteClient;
import br.edu.ucsal.ms_vendas.client.EstoqueClient;
import br.edu.ucsal.ms_vendas.client.ProdutoClient;
import br.edu.ucsal.ms_vendas.client.dto.ClienteDTO;
import br.edu.ucsal.ms_vendas.client.dto.ProdutoDTO;
import br.edu.ucsal.ms_vendas.dto.request.ItemVendaRequest;
import br.edu.ucsal.ms_vendas.dto.request.VendaRequest;
import br.edu.ucsal.ms_vendas.exception.EstoqueInsuficienteException;
import br.edu.ucsal.ms_vendas.exception.RecursoExternoNaoEncontradoException;
import br.edu.ucsal.ms_vendas.exception.ServicoIndisponivelException;
import br.edu.ucsal.ms_vendas.model.ItemVenda;
import br.edu.ucsal.ms_vendas.model.Venda;
import br.edu.ucsal.ms_vendas.repository.VendaRepository;
import br.edu.ucsal.ms_vendas.util.StatusVendaEnum;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

	@Mock VendaRepository repository;
	@Mock ClienteClient clienteClient;
	@Mock ProdutoClient produtoClient;
	@Mock EstoqueClient estoqueClient;

	@InjectMocks VendaService service;

	private static final ClienteDTO TONY = new ClienteDTO(1L, "Tony Stark", "tony@stark.com", "PESSOA_FISICA", "22211133345");
	private static final ProdutoDTO ARMADURA = new ProdutoDTO(1L, "Armadura Mark V", "", new BigDecimal("100.00"));
	private static final ProdutoDTO ESCUDO = new ProdutoDTO(2L, "Escudo", "", new BigDecimal("25.50"));

	private final VendaRequest vendaDoisItens = new VendaRequest(1L, List.of(
			new ItemVendaRequest(1L, 2),
			new ItemVendaRequest(2L, 3)));

	private void clienteEProdutosExistem() {
		when(clienteClient.buscarPorId(1L)).thenReturn(TONY);
		when(produtoClient.buscarPorId(1L)).thenReturn(ARMADURA);
		when(produtoClient.buscarPorId(2L)).thenReturn(ESCUDO);
	}

	@Test
	void deveRegistrarVendaChamandoOsServicosNaOrdemCerta() {
		clienteEProdutosExistem();
		when(repository.saveAndFlush(any(Venda.class))).thenAnswer(inv -> inv.getArgument(0));

		Venda venda = service.registrarVenda(vendaDoisItens);

		assertThat(venda.getClienteNome()).isEqualTo("Tony Stark");
		assertThat(venda.getStatus()).isEqualTo(StatusVendaEnum.CONCLUIDA);
		assertThat(venda.getItens()).hasSize(2);
		// 2 x 100.00 + 3 x 25.50 = 276.50 (preço copiado do ms-produtos no momento da venda)
		assertThat(venda.getValorTotal()).isEqualByComparingTo("276.50");

		InOrder ordem = inOrder(clienteClient, produtoClient, estoqueClient, repository);
		ordem.verify(clienteClient).buscarPorId(1L);
		ordem.verify(produtoClient).buscarPorId(1L);
		ordem.verify(produtoClient).buscarPorId(2L);
		ordem.verify(estoqueClient).registrarSaida(1L, 2);
		ordem.verify(estoqueClient).registrarSaida(2L, 3);
		ordem.verify(repository).saveAndFlush(any(Venda.class));
	}

	@Test
	void naoDeveMexerNoEstoqueQuandoClienteNaoExiste() {
		when(clienteClient.buscarPorId(1L))
				.thenThrow(new RecursoExternoNaoEncontradoException("Cliente 1 não encontrado"));

		assertThatThrownBy(() -> service.registrarVenda(vendaDoisItens))
				.isInstanceOf(RecursoExternoNaoEncontradoException.class);

		verifyNoInteractions(produtoClient, estoqueClient);
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void deveEstornarPrimeiroItemQuandoSegundoNaoTemEstoque() {
		clienteEProdutosExistem();
		when(estoqueClient.registrarSaida(1L, 2)).thenReturn(null); // item 1 tem estoque
		doThrow(new EstoqueInsuficienteException("Estoque insuficiente para o produto 2"))
				.when(estoqueClient).registrarSaida(2L, 3);

		assertThatThrownBy(() -> service.registrarVenda(vendaDoisItens))
				.isInstanceOf(EstoqueInsuficienteException.class);

		verify(estoqueClient).registrarEntrada(1L, 2);      // compensação do item 1
		verify(estoqueClient, never()).registrarEntrada(2L, 3); // item 2 nunca foi baixado
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void deveEstornarTodasAsBaixasQuandoFalharAoSalvarAVenda() {
		clienteEProdutosExistem();
		when(repository.saveAndFlush(any(Venda.class))).thenThrow(new RuntimeException("falha no banco"));

		assertThatThrownBy(() -> service.registrarVenda(vendaDoisItens))
				.hasMessage("falha no banco");

		verify(estoqueClient).registrarEntrada(1L, 2);
		verify(estoqueClient).registrarEntrada(2L, 3);
	}

	@Test
	void deveRepassarErroOriginalMesmoSeOEstornoFalhar() {
		clienteEProdutosExistem();
		when(estoqueClient.registrarSaida(1L, 2)).thenReturn(null); // item 1 tem estoque
		doThrow(new EstoqueInsuficienteException("sem estoque")).when(estoqueClient).registrarSaida(2L, 3);
		doThrow(new ServicoIndisponivelException("ms-estoque caiu")).when(estoqueClient).registrarEntrada(1L, 2);

		assertThatThrownBy(() -> service.registrarVenda(vendaDoisItens))
				.isInstanceOf(EstoqueInsuficienteException.class);
	}

	@Test
	void deveRecusarProdutoRepetidoAntesDeChamarOutrosServicos() {
		VendaRequest repetido = new VendaRequest(1L, List.of(
				new ItemVendaRequest(1L, 1),
				new ItemVendaRequest(1L, 2)));

		assertThatThrownBy(() -> service.registrarVenda(repetido))
				.isInstanceOf(IllegalArgumentException.class);

		verifyNoInteractions(clienteClient, produtoClient, estoqueClient);
	}

	@Test
	void cancelarDeveDevolverItensAoEstoque() {
		Venda venda = vendaConcluida();
		when(repository.findById(10L)).thenReturn(Optional.of(venda));
		when(repository.saveAndFlush(venda)).thenReturn(venda);

		Venda cancelada = service.cancelarVenda(10L);

		assertThat(cancelada.getStatus()).isEqualTo(StatusVendaEnum.CANCELADA);
		assertThat(cancelada.getDataCancelamento()).isNotNull();
		verify(estoqueClient).registrarEntrada(1L, 2);
	}

	@Test
	void cancelarVendaJaCanceladaDeveFalharSemMexerNoEstoque() {
		Venda venda = vendaConcluida();
		venda.cancelar();
		when(repository.findById(10L)).thenReturn(Optional.of(venda));

		assertThatThrownBy(() -> service.cancelarVenda(10L))
				.isInstanceOf(IllegalStateException.class);

		verifyNoInteractions(estoqueClient);
	}

	@Test
	void cancelamentoNaoAconteceSeEstoqueEstiverFora() {
		Venda venda = vendaConcluida();
		when(repository.findById(10L)).thenReturn(Optional.of(venda));
		doThrow(new ServicoIndisponivelException("ms-estoque caiu")).when(estoqueClient).registrarEntrada(1L, 2);

		assertThatThrownBy(() -> service.cancelarVenda(10L))
				.isInstanceOf(ServicoIndisponivelException.class);

		assertThat(venda.getStatus()).isEqualTo(StatusVendaEnum.CONCLUIDA);
		verify(repository, never()).saveAndFlush(any());
	}

	private Venda vendaConcluida() {
		Venda venda = new Venda(1L, "Tony Stark", "22211133345");
		venda.adicionarItem(new ItemVenda(1L, "Armadura Mark V", 2, new BigDecimal("100.00")));
		return venda;
	}
}
