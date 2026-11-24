package br.edu.ucsal.ms_vendas.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "itens_venda")
@NoArgsConstructor
@Getter
public class ItemVenda {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Setter(AccessLevel.PACKAGE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venda_id", nullable = false)
	private Venda venda;

	@Column(name = "produto_id", nullable = false)
	private Long produtoId;

	@Column(name = "produto_nome", nullable = false)
	private String produtoNome;

	@Column(nullable = false)
	private Integer quantidade;

	@Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
	private BigDecimal precoUnitario;

	public ItemVenda(Long produtoId, String produtoNome, Integer quantidade, BigDecimal precoUnitario) {
		this.produtoId = produtoId;
		this.produtoNome = produtoNome;
		this.quantidade = quantidade;
		this.precoUnitario = precoUnitario;
	}

	public BigDecimal getSubtotal() {
		return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
	}
}
