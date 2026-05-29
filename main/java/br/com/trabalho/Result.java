package br.com.trabalho;

public class Result {
    String arquivo;
    String metodo;
    int ocorrencias;
    long tempoMs;

    public Result(String arquivo, String metodo, int ocorrencias, long tempoMs) {
        this.arquivo = arquivo;
        this.metodo = metodo;
        this.ocorrencias = ocorrencias;
        this.tempoMs = tempoMs;
    }
}
