package com.microservice.order.client;

import com.microservice.order.dto.ProductResponse;
import com.microservice.order.dto.ReserveProductRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductClient {

	@PostMapping("/api/products/{productId}/reserve")
	ProductResponse reserveProduct(
			@PathVariable Long productId,
			@RequestBody ReserveProductRequest request);
}
