package br.edu.ucsal.ms_vendas.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class CapturadorExcecoes {

	// 404: venda não existe neste serviço
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<RespostaErroPadrao> entidadeNaoEncontrada(EntityNotFoundException e, HttpServletRequest request) {
		return responder(HttpStatus.NOT_FOUND, "Entidade não encontrada.", e.getMessage(), request);
	}

	// 422: a requisição é válida, mas aponta para cliente/produto que não existe em outro serviço
	@ExceptionHandler(RecursoExternoNaoEncontradoException.class)
	public ResponseEntity<RespostaErroPadrao> recursoExternoNaoEncontrado(RecursoExternoNaoEncontradoException e, HttpServletRequest request) {
		return responder(HttpStatus.UNPROCESSABLE_CONTENT, "Referência inválida.", e.getMessage(), request);
	}

	// 409: o ms-estoque recusou a baixa
	@ExceptionHandler(EstoqueInsuficienteException.class)
	public ResponseEntity<RespostaErroPadrao> estoqueInsuficiente(EstoqueInsuficienteException e, HttpServletRequest request) {
		return responder(HttpStatus.CONFLICT, "Estoque insuficiente.", e.getMessage(), request);
	}

	// 409: operação incompatível com o estado atual (ex.: cancelar venda já cancelada)
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<RespostaErroPadrao> estadoInvalido(IllegalStateException e, HttpServletRequest request) {
		return responder(HttpStatus.CONFLICT, "Operação não permitida.", e.getMessage(), request);
	}

	// 503: outro microsserviço fora do ar, lento ou com erro interno
	@ExceptionHandler(ServicoIndisponivelException.class)
	public ResponseEntity<RespostaErroPadrao> servicoIndisponivel(ServicoIndisponivelException e, HttpServletRequest request) {
		return responder(HttpStatus.SERVICE_UNAVAILABLE, "Falha de comunicação com outro serviço.", e.getMessage(), request);
	}

	// 400: falhas do @Valid
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<RespostaErroPadrao> errosValidacao(MethodArgumentNotValidException e, HttpServletRequest request) {
		String detalhes = e.getBindingResult().getFieldErrors().stream()
				.map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return responder(HttpStatus.BAD_REQUEST, "Erro nos dados da requisição.", detalhes, request);
	}

	// 400: JSON malformado ou ausente
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<RespostaErroPadrao> jsonInvalido(HttpMessageNotReadableException e, HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.",
				"Verifique a estrutura e os tipos dos dados enviados no JSON.", request);
	}

	// 400: /api/vendas/abc
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<RespostaErroPadrao> parametroInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST, "Erro nos dados da requisição.",
				"O parâmetro '" + e.getName() + "' recebeu um valor inválido: " + e.getValue(), request);
	}

	// 400: regra de negócio violada nos dados enviados
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<RespostaErroPadrao> argumentosInvalidos(IllegalArgumentException e, HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST, "Erro nos dados da requisição.", e.getMessage(), request);
	}

	// 500: qualquer outro erro, sem expor detalhes internos
	@ExceptionHandler(Exception.class)
	public ResponseEntity<RespostaErroPadrao> erroInesperado(Exception e, HttpServletRequest request) {
		log.error("Erro inesperado em {}", request.getRequestURI(), e);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno.",
				"Ocorreu um erro inesperado. Tente novamente mais tarde.", request);
	}

	private ResponseEntity<RespostaErroPadrao> responder(HttpStatus status, String msgErroPadrao,
			String msgErroEspecifica, HttpServletRequest request) {
		RespostaErroPadrao erro = new RespostaErroPadrao(
				Instant.now(),
				status.value(),
				msgErroPadrao,
				msgErroEspecifica,
				request.getRequestURI());
		return ResponseEntity.status(status).body(erro);
	}
}
