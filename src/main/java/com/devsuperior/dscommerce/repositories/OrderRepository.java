package com.devsuperior.dscommerce.repositories;

import com.devsuperior.dscommerce.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items WHERE o IN :orders")
    List<Order> fetchOrdersWithItems(List<Order> orders);

    @Query(value = "SELECT DISTINCT o FROM Order o JOIN FETCH o.items",
            countQuery = "SELECT COUNT(DISTINCT o) FROM Order o")
    Page<Order> findAllWithItems(Pageable pageable);

    Page<Order> findByMoment(Instant moment, Pageable pageable);

    Page<Order> findByMomentBetween(Instant startMoment, Instant endMoment, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE DATE(o.moment) = DATE(:date)")
    Page<Order> findBySpecificDate(@Param("date") Instant date, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE YEAR(o.moment) = :year AND MONTH(o.moment) = :month")
    Page<Order> findByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month, Pageable pageable);

    @Query(value = "SELECT o FROM Order o WHERE YEARWEEK(o.moment, 1) = YEARWEEK(:date, 1)", nativeQuery = false)
    Page<Order> findByWeek(@Param("date") Instant date, Pageable pageable);

    Page<Order> findAll(Pageable pageable); // Você já tem isso, mas é bom manter para referência
}
