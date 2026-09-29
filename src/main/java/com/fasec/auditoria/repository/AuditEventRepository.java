package com.fasec.auditoria.repository;

import com.fasec.auditoria.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findAll(Pageable pageable);
    Page<AuditEvent> findByEntidadeAndIdEntidade(String entidade, String idEntidade, Pageable pageable);

    List<AuditEvent> findByEntidadeAndIdEntidade(String entidade, String idEntidade);
    List<AuditEvent> findByAutor(String autor);

    // Consulta otimizada: busca no banco apenas eventos de uma entidade anteriores a um marco temporal
    List<AuditEvent> findByEntidadeAndDataHoraEventoBefore(String entidade, LocalDateTime dataLimite);
}