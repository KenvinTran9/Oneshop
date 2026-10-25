package com.oneshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProductShade {
    @Column(name = "shade_label", length = 100)
    private String label;
    @Column(name = "shade_color", length = 7)
    private String color;

    public ProductShade() { }
    public ProductShade(String label, String color) { this.label = label; this.color = color; }
    public String getLabel() { return label; }
    public String getColor() { return color; }
}
