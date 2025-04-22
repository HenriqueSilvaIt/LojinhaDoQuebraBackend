package com.devsuperior.dscommerce.repositories;

import com.devsuperior.dscommerce.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT DISTINCT obj FROM Order obj JOIN FETCH obj.client JOIN FETCH obj.payment LEFT JOIN FETCH obj.items WHERE ...")
    Page<Order> findAllWithItems(Pageable pageable);
}