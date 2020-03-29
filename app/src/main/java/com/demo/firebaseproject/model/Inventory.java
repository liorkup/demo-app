package com.demo.firebaseproject.model;

import java.util.List;

public interface Inventory {

    List<Product> productsForSection(Section section);

    Product getProductById(int id);
}
