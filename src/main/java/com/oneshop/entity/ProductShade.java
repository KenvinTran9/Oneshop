package com.oneshop.entity;

public class ProductShade {
    private String label;
    private String color;
    private String imageUrl;
    private String imageAlt;
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
