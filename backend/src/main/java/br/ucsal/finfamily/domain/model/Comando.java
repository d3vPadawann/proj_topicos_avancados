package br.ucsal.finfamily.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Classe Comando conforme Diagrama de Classes (Figura 5), RF-012 e RN-006.
 * Guarda o pedido interpretado pelo Assistente de IA (intencao, valor, descricao, data, conta, categoria e periodo).
 */
public class Comando {

    private String intencao; // "registrar" ou "consultar"
    private BigDecimal valor;
    private String descricao;
    private LocalDate data;
    private String conta;
    private Long contaId;
    private String categoria;
    private Long categoriaId;
    private String periodo; // "hoje", "semana_atual", "mes_atual", "mes_passado", "ano_atual", "todos"
    private String busca;
    private String tipo; // "DESPESA", "RECEITA", "TODOS"
    private Integer parcelas = 1;
    private Boolean recorrente = false;
    private String textoOriginal;
    private String provedorIA;
    private Long tempoRespostaMs;
    private Boolean requerConfirmacao = true;
    private String mensagemAssistente;

    public Comando() {
    }

    public String getIntencao() {
        return intencao;
    }

    public void setIntencao(String intencao) {
        this.intencao = intencao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getConta() {
        return conta;
    }

    public void setConta(String conta) {
        this.conta = conta;
    }

    public Long getContaId() {
        return contaId;
    }

    public void setContaId(Long contaId) {
        this.contaId = contaId;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public String getBusca() {
        return busca;
    }

    public void setBusca(String busca) {
        this.busca = busca;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }

    public Boolean getRecorrente() {
        return recorrente;
    }

    public void setRecorrente(Boolean recorrente) {
        this.recorrente = recorrente;
    }

    public String getTextoOriginal() {
        return textoOriginal;
    }

    public void setTextoOriginal(String textoOriginal) {
        this.textoOriginal = textoOriginal;
    }

    public String getProvedorIA() {
        return provedorIA;
    }

    public void setProvedorIA(String provedorIA) {
        this.provedorIA = provedorIA;
    }

    public Long getTempoRespostaMs() {
        return tempoRespostaMs;
    }

    public void setTempoRespostaMs(Long tempoRespostaMs) {
        this.tempoRespostaMs = tempoRespostaMs;
    }

    public Boolean getRequerConfirmacao() {
        return requerConfirmacao;
    }

    public void setRequerConfirmacao(Boolean requerConfirmacao) {
        this.requerConfirmacao = requerConfirmacao;
    }

    public String getMensagemAssistente() {
        return mensagemAssistente;
    }

    public void setMensagemAssistente(String mensagemAssistente) {
        this.mensagemAssistente = mensagemAssistente;
    }
}
