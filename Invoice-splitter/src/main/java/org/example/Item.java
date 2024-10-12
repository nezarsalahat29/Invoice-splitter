package org.example;

public class Item {
    private String name;
    private int count;
    private double priceBeforeTax;
    private double priceAfterTax;

    // Constructor
    public Item(String name, int count, double priceBeforeTax, double priceAfterTax) {
        this.name = name;
        this.count = count;
        this.priceBeforeTax = priceBeforeTax;
        this.priceAfterTax = priceAfterTax;
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public double getPriceBeforeTax() {
        return priceBeforeTax;
    }

    public double getPriceAfterTax() {
        return priceAfterTax;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPriceAfterTax(double priceAfterTax) {
        this.priceAfterTax = priceAfterTax;
    }

    public void setPriceBeforeTax(double priceBeforeTax) {
        this.priceBeforeTax = priceBeforeTax;
    }

    public void setCount(int count) {
        this.count = count;
    }

    // toString method for easy printing
    @Override
    public String toString() {
        return String.format("Item{name='%s', count=%d, priceBeforeTax=%.2f, priceAfterTax=%.2f}",
                name, count, priceBeforeTax, priceAfterTax);
    }
}
