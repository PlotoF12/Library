package com.example.library.module.reservation.repository;

import com.example.library.module.reservation.entity.ViolationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 爽约记录 JPA Repository */
@Repository
public interface ViolationRecordRepository extends JpaRepository<ViolationRecord, Long> {

    List<ViolationRecord> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    long countByStudentIdAndPenaltyEndAfter(Long studentId, java.time.LocalDate now);
}
