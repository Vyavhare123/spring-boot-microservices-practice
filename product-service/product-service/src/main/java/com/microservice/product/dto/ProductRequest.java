package com.microservice.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductRequest(
		@NotBlank(message = "Product name is required")
		String name,

		String description,

		@NotNull(message = "Price is required")
		@DecimalMin(value = "0.01", message = "Price must be greater than 0")
		BigDecimal price,

		@NotNull(message = "Quantity is required")
		@Min(value = 0, message = "Quantity cannot be negative")
		Integer quantity) {
}
