package com.example.library.module.seat.repository;

import com.example.library.module.seat.entity.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 自习室 JPA Repository */
@Repository
public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {

    List<StudyRoom> findByStatus(Integer status);
    List<StudyRoom> findByFloorAndStatus(Integer floor, Integer status);
    boolean existsByRoomCode(String roomCode);
}
