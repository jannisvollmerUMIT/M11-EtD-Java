package com.m11.etivityd.java;

import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BatteryTestConsoleApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

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

        // Weiterführende Logik oder Datenhaltung könnte hier implementiert werden

        scanner.close();
    }

    private static String generateDutId(String batteryName, String deviceName) {
        // Aktuelles Datum im Format yyyyMMdd
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Zusammenstellung der DUT-ID
        return batteryName + deviceName + date;
    }
}