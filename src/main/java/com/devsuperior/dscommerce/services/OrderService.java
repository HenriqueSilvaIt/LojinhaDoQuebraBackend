package com.devsuperior.dscommerce.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.devsuperior.dscommerce.dto.CategoryDTO;
import com.devsuperior.dscommerce.dto.ProductMinDTO;
import com.devsuperior.dscommerce.entities.*;
import com.devsuperior.dscommerce.services.exceptions.DatabaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.devsuperior.dscommerce.dto.OrderDTO;
import com.devsuperior.dscommerce.dto.OrderItemDTO;
import com.devsuperior.dscommerce.repositories.OrderItemRepository;
import com.devsuperior.dscommerce.repositories.OrderRepository;
import com.devsuperior.dscommerce.repositories.ProductRepository;
import com.devsuperior.dscommerce.services.exceptions.ResourceNotFoundException;

@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;

    @Transactional(readOnly = true)
    public OrderDTO findById(Long id) {
        Order order = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não encontrado"));
        authService.validateSelfOrAdmin(order.getClient().getId());
        return new OrderDTO(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> findAll(Pageable pageable, String date, String month, String week) {
        Page<Order> page;

        if (date != null && !date.isEmpty()) {
            LocalDate localDate = LocalDate.parse(date);
            Instant instant = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            page = repository.findBySpecificDate(instant, pageable);
        } else if (month != null && !month.isEmpty()) {
            String[] parts = month.split("-");
            int year = Integer.parseInt(parts[0]);
            int monthNumber = Integer.parseInt(parts[1]);
            page = repository.findByYearAndMonth(year, monthNumber, pageable);
        } else if (week != null && !week.isEmpty()) {
            // A lógica para converter o formato de semana 'YYYY-Www' para um Instant representativo pode variar.
            // Uma abordagem é pegar o primeiro dia daquela semana.
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-'W'ww-E", Locale.ENGLISH);
                LocalDate localDate = LocalDate.parse(week + "-1", formatter); // '-1' representa a segunda-feira
                Instant instant = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                // Como a consulta no repository usa YEARWEEK, podemos passar qualquer data da semana
                page = repository.findByWeek(instant, pageable);
            } catch (Exception e) {
                System.err.println("Erro ao parsear a semana: " + week + " - " + e.getMessage());
                page = repository.findAll(pageable); // Em caso de erro, busca todos
            }
        } else {
            page = repository.findAll(pageable);
        }

        return page.map(OrderDTO::new);
    }


    @Transactional
	public OrderDTO insert(OrderDTO dto) {
		
    	Order order = new Order();
    	
    	order.setMoment(Instant.now());
    	order.setStatus(OrderStatus.WAITING_PAYMENT);
    	
    	User user = userService.authenticated();
    	order.setClient(user);
    	
    	for (OrderItemDTO itemDto : dto.getItems()) {
    		Product product = productRepository.getReferenceById(itemDto.getProductId());
    		OrderItem item = new OrderItem(order, product, itemDto.getQuantity(), product.getPrice());
    		order.getItems().add(item);
    	}
    	
    	repository.save(order);
    	orderItemRepository.saveAll(order.getItems());
    	
    	return new OrderDTO(order);
	}

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteOrderItem(Long orderId, Long productId) {
        try {
            System.out.println("Tentando excluir OrderItem com orderId: " + orderId + " e productId: " + productId);

            Order order = repository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
            Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));

            OrderItemPK orderItemPK = new OrderItemPK(order, product);

            System.out.println("OrderItemPK criado: orderId=" + orderItemPK.getOrder().getId() + ", productId=" + orderItemPK.getProduct().getId());

            // Verifique se o OrderItem existe antes de tentar excluí-lo
            if (orderItemRepository.existsById(orderItemPK)) {
                orderItemRepository.deleteById(orderItemPK);
                System.out.println("OrderItem excluído com sucesso.");
            } else {
                System.out.println("OrderItem não encontrado.");
                throw new ResourceNotFoundException("Item do pedido não encontrado");
            }

        } catch (EmptyResultDataAccessException e) {
            System.out.println("OrderItem não encontrado.");
            throw new ResourceNotFoundException("Item do pedido não encontrado");
        } catch (DataIntegrityViolationException e) {
            System.out.println("Falha de integridade referencial.");
            throw new DatabaseException("Falha de integridade referencial");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED) // Alterado para REQUIRED
    public void deleteOrder(Long id) {
        try {
            Order order = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Recurso não encontrado"));

            // Excluir OrderItems associados ao Order
            orderItemRepository.deleteAll(order.getItems());

            // Excluir o Order
            repository.delete(order);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException("Recurso não encontrado");
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Falha de integridade referencial");
        }
    }
}
