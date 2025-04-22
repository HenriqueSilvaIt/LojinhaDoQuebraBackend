package com.devsuperior.dscommerce.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


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
    public List<OrderDTO> findAll() {
        List<Order> result = repository.findAllWithItems();
        return result.stream().map(x -> new OrderDTO(x)).collect(Collectors.toList());
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
