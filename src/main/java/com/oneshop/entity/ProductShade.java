package com.oneshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProductShade {
    @Column(name = "shade_label", length = 100)
    private String label;
    @Column(name = "shade_color", length = 7)
    private String color;
    @Column(name = "shade_image_url", length = 300)
    private String imageUrl;
    @Column(name = "shade_image_alt", length = 300)
    private String imageAlt;
    @Column(name = "shade_image_type", length = 20)
    private String imageType;

    public ProductShade() { }
    public ProductShade(String label, String color) { this.label = label; this.color = color; }
    public ProductShade(String label, String color, String imageUrl, String imageAlt, String imageType) {
        this(label, color);
        this.imageUrl = imageUrl;
        this.imageAlt = imageAlt;
        this.imageType = imageType;
    }
    public String getLabel() { return label; }
    public String getColor() { return color; }
    public String getImageUrl() { return imageUrl; }
    public String getImageAlt() { return imageAlt; }
    public String getImageType() { return imageType; }
}
