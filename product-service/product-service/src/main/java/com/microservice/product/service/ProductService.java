package com.microservice.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.product.dto.ProductRequest;
import com.microservice.product.dto.ProductResponse;
import com.microservice.product.exception.InsufficientStockException;
import com.microservice.product.exception.ResourceNotFoundException;
import com.microservice.product.model.Product;
import com.microservice.product.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ProductService {

	private final ProductRepository productRepository;
	private final ObjectMapper objectMapper;

	public ProductService(ProductRepository productRepository, ObjectMapper objectMapper) {
		this.productRepository = productRepository;
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	void loadDemoProducts() {
		if (productRepository.count() > 0) {
			return;
		}

		create(new ProductRequest("Laptop", "Demo product for learning microservices", BigDecimal.valueOf(55000), 10));
		create(new ProductRequest("Phone", "Second demo product", BigDecimal.valueOf(25000), 20));
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> findAll() {
		return productRepository.findAll(Sort.by("id"))
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ProductResponse findById(Long id) {
		return toResponse(findProduct(id));
	}

	@Transactional
	public ProductResponse reserve(Long id, Integer requestedQuantity) {
		Product product = productRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
		log.info("FLOW-3B product-service checking stock. productId={}, availableStock={}, requestedQuantity={}",
				id, product.getQuantity(), requestedQuantity);
		if (product.getQuantity() < requestedQuantity) {
			log.warn("FLOW-ERROR insufficient stock in product-service. productId={}, availableStock={}, requestedQuantity={}",
					id, product.getQuantity(), requestedQuantity);
			throw new InsufficientStockException("Only " + product.getQuantity()
					+ " items available for product id: " + id);
		}

		product.setQuantity(product.getQuantity() - requestedQuantity);
		log.info("FLOW-3C product-service reserved stock. productId={}, remainingStock={}",
				id, product.getQuantity());

		return toResponse(product);
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		Product product = objectMapper.convertValue(request, Product.class);

		return toResponse(productRepository.save(product));
	}

	@Transactional
	public ProductResponse update(Long id, ProductRequest request) {
		findProduct(id);
		Product product = objectMapper.convertValue(request, Product.class);
		product.setId(id);

		return toResponse(productRepository.save(product));
	}

	@Transactional
	public void delete(Long id) {
		Product product = findProduct(id);
		productRepository.delete(product);
	}

	private Product findProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
	}

	private ProductResponse toResponse(Product product) {
		return objectMapper.convertValue(product, ProductResponse.class);
	}
}
