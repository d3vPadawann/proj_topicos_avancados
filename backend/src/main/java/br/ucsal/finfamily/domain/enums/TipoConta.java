package br.ucsal.finfamily.domain.enums;

/**
 * Enumeracao TipoConta conforme Diagrama de Classes (Figura 5) e OE1.
 * Suporta os 4 tipos exigidos: CORRENTE, POUPANCA, CARTAO_CREDITO e CORRETORA.
 */
public enum TipoConta {
    CORRENTE("Conta Corrente"),
    POUPANCA("Conta Poupança"),
    CARTAO_CREDITO("Cartão de Crédito"),
    CORRETORA("Conta Corretora");

    private final String descricao;

    TipoConta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
