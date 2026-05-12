package com.microservice.order.dto;

import com.microservice.order.model.OrderStatus;
import java.math.BigDecimal;

public record OrderResponse(
		Long id,
		Long productId,
		String productName,
		Integer quantity,
		BigDecimal unitPrice,
		BigDecimal totalAmount,
		OrderStatus status) {
}
