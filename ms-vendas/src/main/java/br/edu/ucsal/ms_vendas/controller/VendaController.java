package br.edu.ucsal.ms_vendas.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.edu.ucsal.ms_vendas.dto.request.VendaRequest;
import br.edu.ucsal.ms_vendas.dto.response.VendaResponse;
import br.edu.ucsal.ms_vendas.model.Venda;
import br.edu.ucsal.ms_vendas.service.VendaService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("/api/vendas")
public class VendaController {

	private final VendaService service;

	@GetMapping
	public ResponseEntity<List<VendaResponse>> listar(@RequestParam(required = false) Long clienteId) {
		List<Venda> vendas = (clienteId == null)
				? service.listarTodas()
				: service.listarPorCliente(clienteId);

		List<VendaResponse> response = vendas.stream()
				.map(VendaResponse::converterParaDTO)
				.toList();

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<VendaResponse> buscarPorId(@PathVariable Long id) {
		Venda venda = service.buscarPorId(id);
		return ResponseEntity.ok(VendaResponse.converterParaDTO(venda));
	}

	@PostMapping
	public ResponseEntity<VendaResponse> registrarVenda(@Valid @RequestBody VendaRequest request) {
		Venda venda = service.registrarVenda(request);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(venda.getId())
				.toUri();

		return ResponseEntity.created(location).body(VendaResponse.converterParaDTO(venda));
	}

	// Venda não é apagada nem editada: é cancelada, e o estoque recebe os itens de volta.
	@PatchMapping("/{id}/cancelamento")
	public ResponseEntity<VendaResponse> cancelarVenda(@PathVariable Long id) {
		Venda venda = service.cancelarVenda(id);
		return ResponseEntity.ok(VendaResponse.converterParaDTO(venda));
	}
}
