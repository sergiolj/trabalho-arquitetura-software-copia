package br.edu.ucsal.ms_vendas.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import br.edu.ucsal.ms_vendas.util.StatusVendaEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Venda registrada pelo orquestrador.
 *
 * Guarda apenas os IDs de cliente e produto (que pertencem a outros microsserviços)
 * e uma "foto" dos dados no momento da venda (nome do cliente, nome e preço do produto).
 * Assim, se o preço do produto mudar depois, a venda antiga não muda junto.
 */
@Entity
@Table(name = "vendas")
@NoArgsConstructor
@Getter
public class Venda {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "cliente_id", nullable = false)
	private Long clienteId;

	@Column(name = "cliente_nome", nullable = false, length = 150)
	private String clienteNome;

	@Column(name = "cliente_documento", length = 18)
	private String clienteDocumento;

	@Column(name = "data_venda", nullable = false)
	private Instant dataVenda;

	@Column(name = "data_cancelamento")
	private Instant dataCancelamento;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusVendaEnum status;

	@Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal valorTotal;

	@OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ItemVenda> itens = new ArrayList<>();

	public Venda(Long clienteId, String clienteNome, String clienteDocumento) {
		this.clienteId = clienteId;
		this.clienteNome = clienteNome;
		this.clienteDocumento = clienteDocumento;
		this.dataVenda = Instant.now();
		this.status = StatusVendaEnum.CONCLUIDA;
		this.valorTotal = BigDecimal.ZERO;
	}

	public void adicionarItem(ItemVenda item) {
		item.setVenda(this);
		this.itens.add(item);
		this.valorTotal = this.valorTotal.add(item.getSubtotal());
	}

	public void cancelar() {
		if (this.status == StatusVendaEnum.CANCELADA) {
			throw new IllegalStateException("A venda " + id + " já está cancelada");
		}
		this.status = StatusVendaEnum.CANCELADA;
		this.dataCancelamento = Instant.now();
	}

	public boolean isCancelada() {
		return this.status == StatusVendaEnum.CANCELADA;
	}
}
