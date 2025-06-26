package com.devsuperior.dscommerce.repositories;

import com.devsuperior.dscommerce.dto.HistoryDTO;
import com.devsuperior.dscommerce.dto.OrderDTO;
import com.devsuperior.dscommerce.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {


    @Query("SELECT new com.devsuperior.dscommerce.dto.HistoryDTO(" +
            "o.id, " +
            "p.name, " +
            "oi.quantity, " +
            "o.moment, " +
            "p.id,  " +
            "oi.quantity * oi.price) " +
            "FROM OrderItem oi " +
            "JOIN  oi.id.order o   " +
            "JOIN oi.id.product p   " +
            "WHERE o.moment >= :minDate AND o.moment < :maxDate " +
            "ORDER BY o.moment DESC " )
    Page<HistoryDTO> searchOrderByDate(Instant minDate, Instant maxDate, Pageable pageable);
/*
    @Query("SELECT o FROM Order o WHERE TRUNC('DAY', o.moment) = :date")
    Page<Order> findByMomentDate(@Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE YEAR(o.moment) = :year AND MONTH(o.moment) = :month")
    Page<Order> findByMomentMonth(@Param("year") int year, @Param("month") int month, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE YEAR(o.moment) = :year")
    Page<Order> findByMomentYear(@Param("year") int year, Pageable pageable);

    // Consulta para filtrar por semana (ISO 8601)
    @Query("SELECT o FROM Order o WHERE YEAR(o.moment) = :year AND WEEK(o.moment) = :week")
    Page<Order> findByMomentWeek(@Param("year") int year, @Param("week") int week, Pageable pageable);

//    Page<Order> findAll(Pageable pageable); */
}