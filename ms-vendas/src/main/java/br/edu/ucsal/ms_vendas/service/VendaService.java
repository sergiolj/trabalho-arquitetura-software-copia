package br.edu.ucsal.ms_vendas.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import br.edu.ucsal.ms_vendas.client.ClienteClient;
import br.edu.ucsal.ms_vendas.client.EstoqueClient;
import br.edu.ucsal.ms_vendas.client.ProdutoClient;
import br.edu.ucsal.ms_vendas.client.dto.ClienteDTO;
import br.edu.ucsal.ms_vendas.client.dto.ProdutoDTO;
import br.edu.ucsal.ms_vendas.dto.request.ItemVendaRequest;
import br.edu.ucsal.ms_vendas.dto.request.VendaRequest;
import br.edu.ucsal.ms_vendas.model.ItemVenda;
import br.edu.ucsal.ms_vendas.model.Venda;
import br.edu.ucsal.ms_vendas.repository.VendaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orquestrador da venda.
 *
 * Coordena, nesta ordem, as chamadas aos outros microsserviços:
 *   1. ms-clientes: o cliente existe?
 *   2. ms-produtos: os produtos existem? Qual o preço atual?
 *   3. ms-estoque:  dá baixa na quantidade de cada item.
 *   4. banco do ms-vendas: grava a venda.
 *
 * Cada serviço tem seu próprio banco, então não existe uma transação única entre eles.
 * Se algo falhar depois de o estoque ter sido baixado, o orquestrador desfaz as baixas
 * chamando o ms-estoque de novo (compensação, padrão Saga orquestrada).
 *
 * Os métodos de escrita não usam @Transactional de propósito: não é boa prática manter
 * uma transação de banco aberta enquanto se espera resposta HTTP de outro serviço.
 */
@Slf4j
@Service
@AllArgsConstructor
public class VendaService {

	private final VendaRepository repository;
	private final ClienteClient clienteClient;
	private final ProdutoClient produtoClient;
	private final EstoqueClient estoqueClient;

	public List<Venda> listarTodas() {
		return repository.findAll();
	}

	public List<Venda> listarPorCliente(Long clienteId) {
		return repository.findByClienteId(clienteId);
	}

	public Venda buscarPorId(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Venda não localizada com o id " + id));
	}

	public Venda registrarVenda(VendaRequest request) {
		validarProdutosRepetidos(request.itens());

		// 1. Consulta o cliente no ms-clientes (404 lá vira 422 aqui)
		ClienteDTO cliente = clienteClient.buscarPorId(request.clienteId());
		Venda venda = new Venda(cliente.id(), cliente.nome(), cliente.idReceitaFederal());

		// 2. Consulta cada produto no ms-produtos e guarda a "foto" do nome e do preço
		for (ItemVendaRequest itemRequest : request.itens()) {
			ProdutoDTO produto = produtoClient.buscarPorId(itemRequest.produtoId());
			venda.adicionarItem(new ItemVenda(produto.id(), produto.nome(), itemRequest.quantidade(), produto.preco()));
		}

		// 3 e 4. Baixa o estoque item a item e grava a venda.
		// A lista guarda o que já foi baixado, para saber o que estornar se algo falhar.
		List<ItemVenda> itensBaixados = new ArrayList<>();
		try {
			for (ItemVenda item : venda.getItens()) {
				estoqueClient.registrarSaida(item.getProdutoId(), item.getQuantidade());
				itensBaixados.add(item);
			}
			Venda salva = repository.saveAndFlush(venda);
			log.info("Venda {} registrada: cliente {}, total {}", salva.getId(), salva.getClienteId(), salva.getValorTotal());
			return salva;

		} catch (RuntimeException e) {
			log.warn("Falha ao registrar a venda ({}). Estornando {} baixa(s) de estoque.", e.getMessage(), itensBaixados.size());
			estornarBaixas(itensBaixados);
			throw e;
		}
	}

	public Venda cancelarVenda(Long id) {
		Venda venda = buscarPorId(id);
		if (venda.isCancelada()) {
			throw new IllegalStateException("A venda " + id + " já está cancelada");
		}

		// Devolve ao estoque cada item vendido. Se o ms-estoque falhar no meio,
		// refaz a baixa do que já tinha sido devolvido, e a venda continua CONCLUIDA.
		List<ItemVenda> itensDevolvidos = new ArrayList<>();
		try {
			for (ItemVenda item : venda.getItens()) {
				estoqueClient.registrarEntrada(item.getProdutoId(), item.getQuantidade());
				itensDevolvidos.add(item);
			}
			venda.cancelar();
			return repository.saveAndFlush(venda);

		} catch (RuntimeException e) {
			log.warn("Falha ao cancelar a venda {} ({}). Desfazendo {} devolução(ões).", id, e.getMessage(), itensDevolvidos.size());
			desfazerDevolucoes(itensDevolvidos);
			throw e;
		}
	}

	private void estornarBaixas(List<ItemVenda> itens) {
		for (ItemVenda item : itens) {
			try {
				estoqueClient.registrarEntrada(item.getProdutoId(), item.getQuantidade());
			} catch (RuntimeException falha) {
				// O estorno também falhou (ex.: ms-estoque caiu no meio do processo).
				// Fica registrado no log para correção manual.
				log.error("ESTORNO PENDENTE: produto {} quantidade {} ({})",
						item.getProdutoId(), item.getQuantidade(), falha.getMessage());
			}
		}
	}

	private void desfazerDevolucoes(List<ItemVenda> itens) {
		for (ItemVenda item : itens) {
			try {
				estoqueClient.registrarSaida(item.getProdutoId(), item.getQuantidade());
			} catch (RuntimeException falha) {
				log.error("AJUSTE PENDENTE: rebaixar produto {} quantidade {} ({})",
						item.getProdutoId(), item.getQuantidade(), falha.getMessage());
			}
		}
	}

	private void validarProdutosRepetidos(List<ItemVendaRequest> itens) {
		Set<Long> produtos = new HashSet<>();
		for (ItemVendaRequest item : itens) {
			if (!produtos.add(item.produtoId())) {
				throw new IllegalArgumentException(
						"O produto " + item.produtoId() + " foi informado mais de uma vez. Some as quantidades em um único item.");
			}
		}
	}
}
