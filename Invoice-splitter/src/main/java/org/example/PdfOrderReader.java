package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
public class PdfOrderReader {

    public static void main(String[] args) {
//        String filePath = "path/to/your/order.pdf"; // Update the path to your PDF file
        String filePath = "C:\\Users\\moham\\Downloads\\orders.pdf"; // Update the path to your PDF file

        try {
            // Load the PDF document
            PDDocument document = PDDocument.load(new File(filePath));

            // Initialize PDFTextStripper to extract text
            PDFTextStripper pdfStripper = new PDFTextStripper();

            // Extract text from the PDF
            String text = pdfStripper.getText(document);

            // Close the document
            document.close();

            // Parse and extract the order details
            List<Item> items=parseOrderDetails(text);
            String emailBody = prepareEmailBody(items);

            // Open Outlook with prefilled email body
            sendEmailViaOutlook("Nizar.salahat@globitel.com", "Your Order Details", emailBody);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendEmailViaOutlook(String recipientEmail, String subject, String body) {
        try {
            String encodedSubject = URLEncoder.encode(subject, StandardCharsets.UTF_8.toString());
            String encodedBody = URLEncoder.encode(body, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20").replace("%0A", "%0D%0A");

            String uriStr = String.format("""
                            mailto:%s?subject=%s&body=%s""",
                    recipientEmail, encodedSubject, encodedBody);

            // Encode the email content to a URI

            URI mailto = new URI(uriStr);

            // Use Desktop API to open the mail client (Outlook)
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().mail(mailto);
            } else {
                System.err.println("Desktop email feature not supported.");
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
    public static String prepareEmailBody(List<Item> items) {
        StringBuilder emailBody = new StringBuilder();

        emailBody.append("Dear Customer,\n\n")
                .append("Thank you for your order!\n\n")
                .append("Here are your order details:\n\n");

        // Add item details in plain text table format
        // Table header
        for (Item item : items) {
            emailBody.append("- **").append(item.getName()).append("** (Quantity: ")
                    .append(item.getCount()).append(")\n")
                    .append("  - Price Before Tax: ")
                    .append(String.format("%.2f", item.getPriceBeforeTax())).append(" JOD\n")
                    .append("  - Price After Tax: ")
                    .append(String.format("%.2f", item.getPriceAfterTax())).append(" JOD\n\n");
        }



        // Final message
        emailBody.append("\nWe hope you enjoy your meal!\n")
                .append("Best regards\n");
        System.out.println(emailBody);
        return emailBody.toString();
    }
    public static List<Item> parseOrderDetails(String text) {
        // Split the text by lines
        String[] lines = text.split("\\r?\\n");

        // Variables to store order details
        double total = 0.0;
        double salesTax = 0.0;
        double serviceFee = 0.0;
        double combinedTaxAndFee = 0.0;
        double taxPercentage = 0.0;
        List<Item> items = new ArrayList<>();

        System.out.println("Order Details:");

        // First pass: Collect items, sales tax, service fee, and total
        for (String line : lines) {
            line = line.trim(); // Remove leading and trailing whitespace

            if (line.startsWith("Total JOD")) {
                total = Double.parseDouble(line.split(" ")[2].trim());
                System.out.println("Total: " + total + " JOD");
            } else if (line.startsWith("Sales Tax")) {
                salesTax = Double.parseDouble(line.split(" ")[2].trim());
                System.out.println("Sales Tax: " + salesTax + " JOD");
            } else if (line.startsWith("Service Fee")) {
                serviceFee = Double.parseDouble(line.split(" ")[2].trim());
                System.out.println("Service Fee: " + serviceFee + " JOD");
            } else if (line.matches(".*\\([0-9]+\\)\\s+[0-9]+\\.[0-9]{2}")) {
                // Line contains an item, e.g., "BLT (1) 2.75"

                // Extract item name, count, and price before tax
                String[] parts = line.split("\\s+");
                StringBuilder nameBuilder = new StringBuilder();
                int count = 1;
                double priceBeforeTax = 0.0;

                // Iterate through parts to build the name and extract count and price
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];

                    if (part.matches("\\([0-9]+\\)")) {
                        // Extract count
                        count = Integer.parseInt(part.replaceAll("[()]", ""));
                    } else if (part.matches("[0-9]+\\.[0-9]{2}")) {
                        // Extract price
                        priceBeforeTax = Double.parseDouble(part);
                    } else {
                        // Build item name
                        if (nameBuilder.length() > 0) {
                            nameBuilder.append(" ");
                        }
                        nameBuilder.append(part);
                    }
                }

                String itemName = nameBuilder.toString();

                // Create an Item object without tax adjustments yet
                Item item = new Item(itemName, count, priceBeforeTax, 0.0);
                items.add(item);
            }
        }

        // Second pass: Now that we have the total, sales tax, and service fee, calculate the tax percentage
        if (total > 0 && (salesTax > 0 || serviceFee > 0)) {
            combinedTaxAndFee = salesTax + serviceFee;
            taxPercentage = (combinedTaxAndFee / (total - combinedTaxAndFee)) * 100;
            System.out.printf("Combined Tax and Service Fee Percentage: %.2f%%\n", taxPercentage);

            // Update each item's price after tax
            for (Item item : items) {
                double priceAfterTax = item.getPriceBeforeTax() + (item.getPriceBeforeTax() * taxPercentage / 100);
                item.setPriceAfterTax(priceAfterTax);
            }
        } else {
            System.out.println("Could not calculate tax percentage.");
        }

        // Print all items with adjusted prices
        System.out.println("\nItems:");
        for (Item item : items) {
            System.out.println(item);
        }
        return items;
    }
}
