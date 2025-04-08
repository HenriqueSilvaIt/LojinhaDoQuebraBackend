package com.devsuperior.dscommerce.repositories;

import com.devsuperior.dscommerce.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

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
            countQuery = "SELECT COUNT(DISTINCT o) FROM Order o") // Consulta de contagem explícita com DISTINCT
    Page<Order> findAllWithItems(Pageable pageable);
}
