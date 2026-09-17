package com.example.library.module.seat.repository;

import com.example.library.module.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 座位 JPA Repository */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByRoomIdAndStatus(Long roomId, Integer status);
    List<Seat> findByRoomId(Long roomId);
    boolean existsByRoomIdAndSeatCode(Long roomId, String seatCode);
    long countByRoomIdAndStatus(Long roomId, Integer status);
}
