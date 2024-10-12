package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

class Recipient {
    String name;
    String email;

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}

public class EmailApp {
    private JFrame frame;
    private JButton sendEmailButton;
    private JButton uploadButton;
    private File selectedPdfFile;
    private List<Recipient> recipients;
    private List<JCheckBox> checkBoxes;

    public EmailApp() {
        frame = new JFrame("Order Email Sender");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        // Load recipients from JSON
        loadRecipientsFromJson("src/main/resources/recipients.json");

        // Panel for checkboxes
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkBoxes = new ArrayList<>();

        // Create checkboxes for each recipient
        for (Recipient recipient : recipients) {
            JCheckBox checkBox = new JCheckBox(recipient.getName());
            checkBox.setActionCommand(recipient.getEmail());
            checkBoxes.add(checkBox);
            checkboxPanel.add(checkBox);
        }

        // Button to upload PDF
        uploadButton = new JButton("Upload PDF");
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadPdf();
            }
        });

        // Send email button
        sendEmailButton = new JButton("Send Email");
        sendEmailButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> selectedEmails = new ArrayList<>();
                for (JCheckBox checkBox : checkBoxes) {
                    if (checkBox.isSelected()) {
                        selectedEmails.add(checkBox.getActionCommand());
                    }
                }
                if (selectedEmails.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please select at least one recipient.");
                } else if (selectedPdfFile != null) {
                    List<Item> items = parseOrderDetailsFromPdf(selectedPdfFile);
                    String subject = "Your Order Details";
                    String body = prepareEmailBody(items);

                    // Join selected emails into a single string
                    String emailToSendTo = String.join(",", selectedEmails);
                    sendEmailViaOutlook(emailToSendTo, subject, body);
                } else {
                    JOptionPane.showMessageDialog(frame, "Please upload a PDF file first.");
                }
            }
        });

        // Add components to the frame
        frame.add(checkboxPanel, BorderLayout.CENTER);
        frame.add(uploadButton, BorderLayout.NORTH);
        frame.add(sendEmailButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void loadRecipientsFromJson(String filePath) {
        try {
            Gson gson = new Gson();
            FileReader reader = new FileReader(filePath);
            recipients = gson.fromJson(reader, new TypeToken<List<Recipient>>() {}.getType());
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error loading recipients: " + e.getMessage());
        }
    }

    private void uploadPdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedPdfFile = fileChooser.getSelectedFile();
            JOptionPane.showMessageDialog(frame, "Selected PDF: " + selectedPdfFile.getAbsolutePath());
        }
    }

    private List<Item> parseOrderDetailsFromPdf(File pdfFile) {
        List<Item> items = new ArrayList<>();
        try {
            PDDocument document = PDDocument.load(pdfFile);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            document.close();
            items = parseOrderDetails(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static void sendEmailViaOutlook(String recipientEmail, String subject, String body) {
        try {
            String encodedSubject = URLEncoder.encode(subject, StandardCharsets.UTF_8.toString()).replace("+", "%20").replace("%0A", "%0D%0A");;
            String encodedBody = URLEncoder.encode(body, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20").replace("%0A", "%0D%0A");

            String uriStr = String.format("mailto:%s?subject=%s&body=%s",
                    recipientEmail, encodedSubject, encodedBody);

            URI mailto = new URI(uriStr);
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

        // Add item details
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
                Item item = new Item(itemName, count, priceBeforeTax/count, 0.0);
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

    public static void main(String[] args) {
        new EmailApp();
    }
}
