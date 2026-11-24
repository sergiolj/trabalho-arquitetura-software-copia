package br.edu.ucsal.ms_vendas.exception;

/** O ms-estoque recusou a baixa por falta de quantidade disponível. */
public class EstoqueInsuficienteException extends RuntimeException {

	public EstoqueInsuficienteException(String mensagem) {
		super(mensagem);
	}
}
