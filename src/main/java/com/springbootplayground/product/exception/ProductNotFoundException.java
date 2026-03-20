package com.springbootplayground.product.exception;

import com.springbootplayground.common.exception.NotFoundException;

public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException(Long id) {
        super("Product not found: " + id);
    }
}
