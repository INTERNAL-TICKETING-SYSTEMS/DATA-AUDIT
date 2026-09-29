package com.fasec.auditoria.repository;

import com.fasec.auditoria.model.DataViolation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataViolationRepository extends JpaRepository<DataViolation, Long> {
    Page<DataViolation> findAll(Pageable pageable);
    Page<DataViolation> findBySeveridade(String severidade, Pageable pageable);
    List<DataViolation> findByEntidadeAfetadaAndIdEntidadeAfetada(String entidadeAfetada, String idEntidadeAfetada);
}