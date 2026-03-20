package com.springbootplayground.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.springbootplayground.product.entity.Product;
import com.springbootplayground.product.exception.ProductNotFoundException;
import com.springbootplayground.product.exception.ProductSkuAlreadyExistsException;
import com.springbootplayground.product.repository.ProductRepository;
import com.springbootplayground.product.web.dto.ProductRequest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repository);
    }

    @Test
    void getByIdThrowsBusinessExceptionWhenProductDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class, () -> service.getById(99L));

        assertEquals("Product not found: 99", exception.getMessage());
    }

    @Test
    void createThrowsBusinessExceptionWhenSkuAlreadyExists() {
        ProductRequest request = request("DUP-001");
        when(repository.existsBySku("DUP-001")).thenReturn(true);

        ProductSkuAlreadyExistsException exception =
                assertThrows(ProductSkuAlreadyExistsException.class, () -> service.create(request));

        assertEquals("SKU already exists: DUP-001", exception.getMessage());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(Product.class));
    }

    private ProductRequest request(String sku) {
        return new ProductRequest(
                "Starter Backpack",
                "Durable backpack for everyday use.",
                new BigDecimal("59.99"),
                sku,
                25,
                true
        );
    }
}
