package com.springbootplayground.product.exception;

import com.springbootplayground.common.exception.AlreadyExistsException;

public class ProductSkuAlreadyExistsException extends AlreadyExistsException {

    public ProductSkuAlreadyExistsException(String sku) {
        super("SKU already exists: " + sku);
    }
}
