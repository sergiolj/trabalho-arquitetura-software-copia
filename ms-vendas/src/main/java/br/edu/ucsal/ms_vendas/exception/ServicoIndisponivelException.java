package br.edu.ucsal.ms_vendas.exception;

/** Outro microsserviço está fora do ar, não respondeu a tempo ou devolveu um erro inesperado. */
public class ServicoIndisponivelException extends RuntimeException {

	public ServicoIndisponivelException(String mensagem) {
		super(mensagem);
	}
}
