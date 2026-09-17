package com.example.library.module.reservation.repository;

import com.example.library.module.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** 预约 JPA Repository（基础CRUD，复杂查询走MyBatis） */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByStudentIdAndReserveDate(Long studentId, LocalDate date);

    List<Reservation> findByStudentIdAndStatus(Long studentId, Integer status);

    List<Reservation> findByRoomIdAndReserveDate(Long roomId, LocalDate date);

    List<Reservation> findByStatusAndReserveDate(Integer status, LocalDate date);

    List<Reservation> findBySignToken(String signToken);

    boolean existsByStudentIdAndReserveDateAndStatusIn(
            Long studentId, LocalDate date, List<Integer> statuses);
}
