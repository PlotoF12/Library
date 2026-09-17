package com.example.library.module.user.repository;

import com.example.library.module.user.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 学生 JPA Repository */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentNo(String studentNo);

    Optional<Student> findByPhone(String phone);

    boolean existsByStudentNo(String studentNo);

    boolean existsByPhone(String phone);
}
