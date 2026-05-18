package com.microservice.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.order.client.ProductClient;
import com.microservice.order.dto.OrderRequest;
import com.microservice.order.dto.OrderResponse;
import com.microservice.order.dto.ProductResponse;
import com.microservice.order.dto.ReserveProductRequest;
import com.microservice.order.exception.OrderCreationException;
import com.microservice.order.exception.ResourceNotFoundException;
import com.microservice.order.model.CustomerOrder;
import com.microservice.order.model.OrderStatus;
import com.microservice.order.repository.OrderRepository;
import feign.FeignException;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

	private final OrderRepository orderRepository;
	private final ProductClient productClient;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public List<OrderResponse> findAll() {
		List<CustomerOrder> orders = orderRepository.findAll(Sort.by("id"));
		return orders.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public OrderResponse findById(Long id) {
		return toResponse(findOrder(id));
	}

	@Transactional
	public OrderResponse create(OrderRequest request) {
		log.info("FLOW-2 order-service business logic started. It will reserve product stock before saving order.");
		ProductResponse product = reserveProduct(request);
		BigDecimal totalAmount = product.price().multiply(BigDecimal.valueOf(request.quantity()));
		CustomerOrder order = CustomerOrder.builder()
				.productId(product.id())
				.productName(product.name())
				.quantity(request.quantity())
				.unitPrice(product.price())
				.totalAmount(totalAmount)
				.status(OrderStatus.CONFIRMED)
				.build();

		CustomerOrder savedOrder = orderRepository.save(order);
		log.info("FLOW-5 order-service saved order in its own database. orderId={}, productId={}, totalAmount={}",
				savedOrder.getId(), savedOrder.getProductId(), savedOrder.getTotalAmount());

		return toResponse(savedOrder);
	}

	@Transactional
	public OrderResponse update(Long id, OrderRequest request) {
		ProductResponse product = reserveProduct(request);
		BigDecimal totalAmount = product.price().multiply(BigDecimal.valueOf(request.quantity()));
		CustomerOrder order = findOrder(id);
		order.setProductId(product.id());
		order.setProductName(product.name());
		order.setQuantity(request.quantity());
		order.setUnitPrice(product.price());
		order.setTotalAmount(totalAmount);
		order.setStatus(OrderStatus.CONFIRMED);

		return toResponse(orderRepository.save(order));
	}

	private ProductResponse reserveProduct(OrderRequest request) {
		try {
			log.info("FLOW-3 ACTUAL SERVICE CALL starts here: order-service -> product-service using OpenFeign + Eureka. productId={}, quantity={}",
					request.productId(), request.quantity());
			ProductResponse product = productClient.reserveProduct(
					request.productId(),
					new ReserveProductRequest(request.quantity()));
			if (product == null) {
				throw new OrderCreationException("Product service returned empty response");
			}

			log.info("FLOW-4 order-service received response from product-service. productName={}, price={}, remainingStock={}",
					product.name(), product.price(), product.quantity());

			return product;
		}
		catch (FeignException.NotFound ex) {
			log.warn("FLOW-ERROR product-service returned 404 for productId={}", request.productId());
			throw new ResourceNotFoundException("Product not found with id: " + request.productId());
		}
		catch (FeignException.BadRequest ex) {
			log.warn("FLOW-ERROR product-service rejected stock reservation. productId={}, quantity={}",
					request.productId(), request.quantity());
			throw new OrderCreationException("Product does not have enough stock for this order");
		}
		catch (FeignException.ServiceUnavailable ex) {
			log.warn("FLOW-ERROR product-service is not available through Eureka");
			throw new OrderCreationException("Product service is not available in Eureka. Start eureka-server and product-service first.");
		}
		catch (FeignException ex) {
			log.warn("FLOW-ERROR product-service call failed. status={}", ex.status());
			throw new OrderCreationException("Product service call failed: " + ex.status());
		}
	}

	@Transactional
	public void delete(Long id) {
		orderRepository.delete(findOrder(id));
	}

	private CustomerOrder findOrder(Long id) {
		return orderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
	}

	private OrderResponse toResponse(CustomerOrder order) {
		return objectMapper.convertValue(order, OrderResponse.class);
	}
}
