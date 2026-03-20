package com.springbootplayground.product.service;

import com.springbootplayground.product.entity.Product;
import com.springbootplayground.product.exception.ProductNotFoundException;
import com.springbootplayground.product.exception.ProductSkuAlreadyExistsException;
import com.springbootplayground.product.repository.ProductRepository;
import com.springbootplayground.product.web.dto.ProductRequest;
import java.util.List;
import org.springframework.stereotype.Service;

import com.springbootplayground.product.event.KafkaProducerService;
import com.springbootplayground.product.event.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final KafkaProducerService kafkaProducerService;

    public List<Product> getAll() {
        return repository.findAll();
    }

    public Product getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product create(ProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new ProductSkuAlreadyExistsException(request.sku());
        }
        Product product = new Product();
        applyRequest(product, request);
        Product createdProduct = repository.save(product);

        // Publish ProductCreatedEvent to Kafka
        ProductCreatedEvent event = new ProductCreatedEvent(
                createdProduct.getId(),
                createdProduct.getName(),
                createdProduct.getDescription(),
                createdProduct.getPrice(),
                createdProduct.getSku(),
                createdProduct.getQuantity(),
                createdProduct.getActive(),
                createdProduct.getCreatedAt()
        );
        kafkaProducerService.sendProductCreatedEvent(event);

        return createdProduct;
    }

    public Product update(Long id, ProductRequest request) {
        Product product = getById(id);
        if (!product.getSku().equals(request.sku()) && repository.existsBySku(request.sku())) {
            throw new ProductSkuAlreadyExistsException(request.sku());
        }
        applyRequest(product, request);
        return repository.save(product);
    }

    public void delete(Long id) {
        Product product = getById(id);
        repository.delete(product);
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setSku(request.sku());
        product.setQuantity(request.quantity());
        product.setActive(request.active());
    }
}
