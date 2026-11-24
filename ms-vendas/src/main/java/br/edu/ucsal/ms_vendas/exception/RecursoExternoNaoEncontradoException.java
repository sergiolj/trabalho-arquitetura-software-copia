package br.edu.ucsal.ms_vendas.exception;

/** O cliente ou produto informado na venda não existe no microsserviço dono desse dado. */
public class RecursoExternoNaoEncontradoException extends RuntimeException {

	public RecursoExternoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}
}
