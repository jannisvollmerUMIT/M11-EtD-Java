package com.m11.etivityd.java;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class BatteryTestConsoleApp {
    private static final double MIN_VOLTAGE = 5.0;
    private static final double MAX_VOLTAGE = 10.0;
    private static final double MIN_CURRENT = 0.0;
    private static final double MAX_CURRENT = 125.0;
    private static final double CAPACITY_THRESHOLD = 500;
    private static final double TOLERANCE = 0.015;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Eingabeaufforderung für den Namen der Datei mit den Messdaten
        System.out.print("Bitte geben Sie den Dateipfad zu den Messdaten ein: ");
        String filePath = scanner.nextLine();

        // Benutzeraufforderung für den Batterienamen
        System.out.print("Bitte geben Sie den Namen der Batterie ein: ");
        String batteryName = scanner.nextLine();

        // Benutzeraufforderung für den Gerätenamen
        System.out.print("Bitte geben Sie den Namen des Gerätes ein: ");
        String deviceName = scanner.nextLine();

        // Generierung der DUT-ID
        String dutId = generateDutId(batteryName, deviceName);

        // Ausgabe der generierten DUT-ID
        System.out.println("Generierte DUT-ID: " + dutId);

        // Daten aus der Datei lesen und in einer Liste speichern
        List<double[]> measurements = readDataFile(filePath);

        // Überprüfung der Messdaten und Bestimmung des Gesamtergebnisses
        String testResult = checkMeasurements(measurements);
        boolean testPassed = testResult.equals("Test erfolgreich bestanden");

        // Ermitteln der gemessenen minimalen und maximalen Werte
        double[] minMaxValues = getMinMaxValues(measurements);

        // PDF-Bericht erstellen
        createPdfReport(dutId, batteryName, deviceName, testPassed, measurements, dtf.format(LocalDateTime.now()), testResult, minMaxValues);

        scanner.close();
    }

    private static String generateDutId(String batteryName, String deviceName) {
        // Aktuelles Datum im Format yyyyMMdd
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Zusammenstellung der DUT-ID
        return batteryName + deviceName + date;
    }

    private static List<double[]> readDataFile(String filePath) {
        List<double[]> measurements = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\\s+");
                double voltage = Double.parseDouble(values[0]);
                double current = Double.parseDouble(values[1]);
                measurements.add(new double[]{voltage, current});
            }
        } catch (IOException e) {
            System.out.println("Fehler beim Lesen der Datei: " + e.getMessage());
        }
        return measurements;
    }

    private static String checkMeasurements(List<double[]> measurements) {
        double totalCapacity = calculateCapacity(measurements);
        for (double[] measurement : measurements) {
            double voltage = measurement[0];
            double current = measurement[1];

            if (voltage < MIN_VOLTAGE * (1 - TOLERANCE)) {
                return "Test fehlgeschlagen: Spannung unter Minimum";
            }
            if (voltage > MAX_VOLTAGE * (1 + TOLERANCE)) {
                return "Test fehlgeschlagen: Spannung über Maximum";
            }
            if (current < MIN_CURRENT * (1 - TOLERANCE)) {
                return "Test fehlgeschlagen: Stromstärke unter Minimum";
            }
            if (current > MAX_CURRENT * (1 + TOLERANCE)) {
                return "Test fehlgeschlagen: Stromstärke über Maximum";
            }
        }

        if (totalCapacity < CAPACITY_THRESHOLD * (1 - TOLERANCE)) {
            return "Test fehlgeschlagen: Kapazität unter Minimum";
        }

        return "Test erfolgreich bestanden";
    }

    private static double[] getMinMaxValues(List<double[]> measurements) {
        double minVoltage = Double.MAX_VALUE;
        double maxVoltage = Double.MIN_VALUE;
        double minCurrent = Double.MAX_VALUE;
        double maxCurrent = Double.MIN_VALUE;

        for (double[] measurement : measurements) {
            double voltage = measurement[0];
            double current = measurement[1];

            if (voltage < minVoltage) {
                minVoltage = voltage;
            }
            if (voltage > maxVoltage) {
                maxVoltage = voltage;
            }
            if (current < minCurrent) {
                minCurrent = current;
            }
            if (current > maxCurrent) {
                maxCurrent = current;
            }
        }

        return new double[]{minVoltage, maxVoltage, minCurrent, maxCurrent};
    }

    private static double calculateCapacity(List<double[]> measurements) {
        double capacity = 0.0;
        double interval = 8.0 / 3600.0; // 8 seconds converted to hours

        System.out.println("Calculating Capacity:");
        for (int i = 0; i < measurements.size() - 1; i++) {
            double current1 = measurements.get(i)[1];
            double current2 = measurements.get(i + 1)[1];
            double trapezoidArea = (current1 + current2) / 2.0 * interval;
            capacity += trapezoidArea;
            System.out.printf("Current1: %.2f A, Current2: %.2f A, Trapezoid Area: %.5f Ah, Cumulative Capacity: %.5f Ah%n",
                    current1, current2, trapezoidArea, capacity);
        }

        return capacity;
    }

    private static void createPdfReport(String dutId, String batteryName, String deviceName, boolean testPassed, List<double[]> measurements, String timestamp, String testResult, double[] minMaxValues) {
        String fileName = dutId + "Report.pdf";
        double totalCapacity = calculateCapacity(measurements);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Titel
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.setNonStrokingColor(testPassed ? Color.GREEN : Color.RED);
                contentStream.beginText();
                contentStream.newLineAtOffset(220, 750);
                contentStream.showText(testPassed ? "TEST SUCCESSFUL" : "TEST FAILED");
                contentStream.endText();

                // Wichtige Daten
                contentStream.setNonStrokingColor(Color.BLACK);
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("DUT-ID: " + dutId);
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Battery Name: " + batteryName);
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Device Name: " + deviceName);
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Test Date & Time: " + timestamp);
                contentStream.endText();

                // Zeichne Diagramm
                XYSeries series = new XYSeries("Spannung-Strom Diagramm");
                for (int i = 0; i < measurements.size(); i++) {
                    series.add(measurements.get(i)[0], measurements.get(i)[1]);
                }
                XYSeriesCollection dataset = new XYSeriesCollection(series);
                JFreeChart chart = ChartFactory.createXYLineChart(
                        "Spannung vs Strom",
                        "Spannung (V)",
                        "Strom (A)",
                        dataset
                );

                // Chart als Bild speichern
                java.io.File chartFile = new java.io.File("chart.png");
                ChartUtils.saveChartAsPNG(chartFile, chart, 600, 400);

                // Bild in PDF einfügen
                PDImageXObject pdImage = PDImageXObject.createFromFile("chart.png", document);
                contentStream.drawImage(pdImage, 100, 300, 400, 300);

                // Fehlgrund oder Erfolgsnachricht
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.setNonStrokingColor(testPassed ? Color.GREEN : Color.RED);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 250);
                contentStream.showText(testResult);
                contentStream.endText();

                // Testdetails
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.setNonStrokingColor(Color.BLACK);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 220);
                contentStream.showText("Gemessene Minimale Spannung: " + minMaxValues[0] + "V - " + (minMaxValues[0] >= MIN_VOLTAGE * (1 - TOLERANCE) ? "pass" : "fail"));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Gemessene Maximale Spannung: " + minMaxValues[1] + "V - " + (minMaxValues[1] <= MAX_VOLTAGE * (1 + TOLERANCE) ? "pass" : "fail"));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Gemessene Minimale Stromstärke: " + minMaxValues[2] + "A - " + (minMaxValues[2] >= MIN_CURRENT * (1 - TOLERANCE) ? "pass" : "fail"));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Gemessene Maximale Stromstärke: " + minMaxValues[3] + "A - " + (minMaxValues[3] <= MAX_CURRENT * (1 + TOLERANCE) ? "pass" : "fail"));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Gesamtkapazität: " + totalCapacity + "mAh - " + (totalCapacity >= CAPACITY_THRESHOLD * (1 - TOLERANCE) ? "pass" : "fail"));
                contentStream.endText();

                // Unterschrift
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 100);
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.showText("Unterschrift: _______________________");
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Datum: " + timestamp.split(" ")[0]);
                contentStream.endText();
            }

            document.save(fileName);
        } catch (IOException e) {
            System.out.println("Fehler beim Erstellen des PDF-Berichts: " + e.getMessage());
        }
    }

    private static boolean checkMinVoltage(List<double[]> measurements) {
        for (double[] measurement : measurements) {
            if (measurement[0] < MIN_VOLTAGE * (1 - TOLERANCE)) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkMaxVoltage(List<double[]> measurements) {
        for (double[] measurement : measurements) {
            if (measurement[0] > MAX_VOLTAGE * (1 + TOLERANCE)) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkMinCurrent(List<double[]> measurements) {
        for (double[] measurement : measurements) {
            if (measurement[1] < MIN_CURRENT * (1 - TOLERANCE)) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkMaxCurrent(List<double[]> measurements) {
        for (double[] measurement : measurements) {
            if (measurement[1] > MAX_CURRENT * (1 + TOLERANCE)) {
                return false;
            }
        }
        return true;
    }
}
