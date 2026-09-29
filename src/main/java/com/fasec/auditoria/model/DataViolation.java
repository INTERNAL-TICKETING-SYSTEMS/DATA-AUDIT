package com.fasec.auditoria.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_data_violation", schema = "audit_schema")
public class DataViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_regra", nullable = false, length = 50)
    private String codigoRegra;

    @Column(nullable = false, length = 20)
    private String severidade;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "entidade_afetada", nullable = false, length = 50)
    private String entidadeAfetada;

    @Column(name = "id_entidade_afetada", length = 100)
    private String idEntidadeAfetada;

    @Column(name = "data_identificacao", nullable = false)
    private LocalDateTime dataIdentificacao = LocalDateTime.now();

    @Column(name = "status_resolucao", nullable = false, length = 30)
    private String statusResolucao = "PENDENTE";

    public DataViolation() {}

    public DataViolation(String codigoRegra, String severidade, String descricao, 
                         String entidadeAfetada, String idEntidadeAfetada) {
        this.codigoRegra = codigoRegra;
        this.severidade = severidade;
        this.descricao = descricao;
        this.entidadeAfetada = entidadeAfetada;
        this.idEntidadeAfetada = idEntidadeAfetada;
        this.dataIdentificacao = LocalDateTime.now();
        this.statusResolucao = "PENDENTE";
    }

    public Long getId() { return id; }
    public String getCodigoRegra() { return codigoRegra; }
    public String getSeveridade() { return severidade; }
    public String getDescricao() { return descricao; }
    public String getEntidadeAfetada() { return entidadeAfetada; }
    public String getIdEntidadeAfetada() { return idEntidadeAfetada; }
    public LocalDateTime getDataIdentificacao() { return dataIdentificacao; }
    public String getStatusResolucao() { return statusResolucao; }

    public void setStatusResolucao(String statusResolucao) {
        this.statusResolucao = statusResolucao;
    }
}