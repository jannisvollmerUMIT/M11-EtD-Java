package com.m11.etivityd.java;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class BatteryTestConsoleApp {
    private static final double MIN_VOLTAGE = 5.0;
    private static final double MAX_VOLTAGE = 10.0;
    private static final double MIN_CURRENT = 0.0;
    private static final double MAX_CURRENT = 125.0;
    private static double MIN_CAPACITY; // Set dynamically based on user input or battery specifications

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String filePath = promptForInput(scanner, "Bitte geben Sie den Dateipfad zu den Messdaten ein: ");
        String batteryName = promptForInput(scanner, "Bitte geben Sie den Namen der Batterie ein: ");
        String deviceName = promptForInput(scanner, "Bitte geben Sie den Namen des Gerätes ein: ");


        // Set the minimum capacity based on user input or predefined specification
        MIN_CAPACITY = Double.parseDouble(promptForInput(scanner, "Bitte geben Sie die minimale Kapazität in Ah ein: "));

        String dutId = generateDutId(batteryName, deviceName);
        System.out.println("Generierte DUT-ID: " + dutId);

        List<double[]> measurements = readDataFile(filePath);
        if (measurements.isEmpty()) {
            System.out.println("Keine Messdaten gefunden.");
            return;
        }

        displayMeasurements(measurements);

        List<String> failReasons = new ArrayList<>();
        boolean pass = validateMeasurements(measurements, failReasons);
        double capacity = calculateCapacity(measurements);
        if (capacity < MIN_CAPACITY) {
            failReasons.add("Kapazität unter dem Minimum.");
        }

        generateReport(dutId, pass, capacity, measurements, failReasons);

        String saveRawDataResponse = promptForInput(scanner, "Möchten Sie die Rohdaten speichern? (ja/nein): ");
        if (saveRawDataResponse.equalsIgnoreCase("ja")) {
            saveRawData(dutId, measurements);
        }

        scanner.close();
    }

    private static String promptForInput(Scanner scanner, String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    private static String generateDutId(String batteryName, String deviceName) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return batteryName + deviceName + date;
    }

    private static List<double[]> readDataFile(String filePath) {
        List<double[]> measurements = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\\s+");
                if (values.length == 2) {
                    double voltage = Double.parseDouble(values[0]);
                    double current = Double.parseDouble(values[1]);
                    measurements.add(new double[]{voltage, current});
                }
            }
        } catch (IOException e) {
            System.out.println("Fehler beim Lesen der Datei: " + e.getMessage());
        }
        return measurements;
    }

    private static void displayMeasurements(List<double[]> measurements) {
        for (double[] measurement : measurements) {
            System.out.printf("Spannung: %.2f V, Stromstärke: %.2f A%n", measurement[0], measurement[1]);
        }
    }

    private static boolean validateMeasurements(List<double[]> measurements, List<String> failReasons) {
        boolean isValid = true;
        for (double[] measurement : measurements) {
            double voltage = measurement[0];
            double current = measurement[1];
            if (voltage < MIN_VOLTAGE || voltage > MAX_VOLTAGE) {
                failReasons.add(String.format("Spannung außerhalb des Bereichs: %.2f V", voltage));
                isValid = false;
            }
            if (current < MIN_CURRENT || current > MAX_CURRENT) {
                failReasons.add(String.format("Stromstärke außerhalb des Bereichs: %.2f A", current));
                isValid = false;
            }
        }
        return isValid;
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

    private static void generateReport(String dutId, boolean pass, double capacity, List<double[]> measurements, List<String> failReasons) {
        boolean capacityPass = capacity >= MIN_CAPACITY;

        double minVoltage = Double.MAX_VALUE;
        double maxVoltage = Double.MIN_VALUE;
        double minCurrent = Double.MAX_VALUE;
        double maxCurrent = Double.MIN_VALUE;

        for (double[] measurement : measurements) {
            double voltage = measurement[0];
            double current = measurement[1];
            if (voltage < minVoltage) minVoltage = voltage;
            if (voltage > maxVoltage) maxVoltage = voltage;
            if (current < minCurrent) minCurrent = current;
            if (current > maxCurrent) maxCurrent = current;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(dutId + "_report.txt"))) {
            writer.println("Battery Test Report");
            writer.println("DUT-ID: " + dutId);
            writer.println("Minimum Capacity: " + MIN_CAPACITY);
            writer.println("Measured Capacity: " + capacity);
            writer.println("Voltage Range: " + MIN_VOLTAGE + " - " + MAX_VOLTAGE);
            writer.println("Current Range: " + MIN_CURRENT + " - " + MAX_CURRENT);
            writer.println("Measured Voltage Range: " + minVoltage + " - " + maxVoltage + " V");
            writer.println("Measured Current Range: " + minCurrent + " - " + maxCurrent + " A");
            writer.println("Test Result: " + ((pass && capacityPass) ? "PASS" : "FAIL"));
            if (!failReasons.isEmpty()) {
                writer.println("Fail Reasons:");
                for (String reason : failReasons) {
                    writer.println("- " + reason);
                }
            }
            writer.println();
            writer.println("Measurement Data:");
            for (double[] measurement : measurements) {
                writer.printf("Spannung: %.2f V, Stromstärke: %.2f A%n", measurement[0], measurement[1]);
            }
        } catch (IOException e) {
            System.out.println("Fehler beim Erstellen des Berichts: " + e.getMessage());
        }
    }

    private static void saveRawData(String dutId, List<double[]> measurements) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(dutId + "_raw_data.txt"))) {
            for (double[] measurement : measurements) {
                writer.printf("%.2f %.2f%n", measurement[0], measurement[1]);
            }
        } catch (IOException e) {
            System.out.println("Fehler beim Speichern der Rohdaten: " + e.getMessage());
        }
    }
}
