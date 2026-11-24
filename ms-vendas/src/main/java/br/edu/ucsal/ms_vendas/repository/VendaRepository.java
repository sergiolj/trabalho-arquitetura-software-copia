package br.edu.ucsal.ms_vendas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ucsal.ms_vendas.model.Venda;

public interface VendaRepository extends JpaRepository<Venda, Long> {

	// @EntityGraph carrega os itens junto com a venda em uma única consulta
	@Override
	@EntityGraph(attributePaths = "itens")
	List<Venda> findAll();

	@Override
	@EntityGraph(attributePaths = "itens")
	Optional<Venda> findById(Long id);

	@EntityGraph(attributePaths = "itens")
	List<Venda> findByClienteId(Long clienteId);
}
