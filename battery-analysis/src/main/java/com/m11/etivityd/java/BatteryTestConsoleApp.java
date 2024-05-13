package com.m11.etivityd.java;

import java.util.Scanner;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BatteryTestConsoleApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Eingabeaufforderung für die DUT-ID
        System.out.print("Bitte geben Sie die DUT-ID ein: ");
        String dutId = scanner.nextLine();
        System.out.println("DUT-ID wurde eingegeben: " + dutId);

        // Simulieren des Lesens von Messdaten
        System.out.println("Messdaten lesen...");
        double testData = Math.random() * 100;  // Zufälliger Testwert als Simulationsbeispiel
        System.out.println("Empfangene Messdaten: " + testData + " mAh");

        // Prüfung der Messdaten
        boolean testPassed = testData >= 50;
        String testResult = testPassed ? "Ja" : "Nein";
        System.out.println("Messdaten innerhalb der Spezifikation. Test " + (testPassed ? "bestanden." : "nicht bestanden."));

        // Daten in einer CSV-Datei speichern
        saveData(dtf.format(LocalDateTime.now()), dutId, testData, testResult);

        scanner.close();
    }

    private static void saveData(String timestamp, String dutId, double testData, String testResult) {
        String filename = "test_results.csv";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename, true))) {
            out.println(timestamp + "," + dutId + "," + testData + "," + testResult);
            System.out.println("Daten wurden erfolgreich in " + filename + " gespeichert.");
        } catch (IOException e) {
            System.out.println("Fehler beim Speichern der Daten: " + e.getMessage());
        }
    }
}


