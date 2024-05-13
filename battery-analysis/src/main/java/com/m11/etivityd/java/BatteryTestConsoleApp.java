package com.m11.etivityd.java;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BatteryTestConsoleApp {
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

        // Ausgabe der gespeicherten Messdaten
        for (double[] measurement : measurements) {
            System.out.printf("Spannung: %.2f V, Stromstärke: %.2f A%n", measurement[0], measurement[1]);
        scanner.close(); }
    }

    private static String generateDutId(String batteryName, String deviceName) {
        // Aktuelles Datum im Format yyyyMMdd
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

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
}