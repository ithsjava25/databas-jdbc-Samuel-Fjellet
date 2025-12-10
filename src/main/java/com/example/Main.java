package com.example;

import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;

import static org.testcontainers.shaded.org.apache.commons.lang3.StringUtils.substring;

public class Main {

    Scanner scanner = new Scanner(System.in);
    String temp = "1";


    static void main(String[] args) {
        if (isDevMode(args)) {
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    public void run() {
        // Resolve DB settings with precedence: System properties -> Environment variables
        String jdbcUrl = resolveConfig("APP_JDBC_URL", "APP_JDBC_URL");
        String dbUser = resolveConfig("APP_DB_USER", "APP_DB_USER");
        String dbPass = resolveConfig("APP_DB_PASS", "APP_DB_PASS");



        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                    "Missing DB configuration. Provide APP_JDBC_URL, APP_DB_USER, APP_DB_PASS " +
                            "as system properties (-Dkey=value) or environment variables.");
        }

        //Todo: Starting point for your code

            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl,
                    dbUser,
                    dbPass)) {

                startUpLogIn(connection);
                cliMenu(connection);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }



    }



    /**
     * Determines if the application is running in development mode based on system properties,
     * environment variables, or command-line arguments.
     *
     * @param args an array of command-line arguments
     * @return {@code true} if the application is in development mode; {@code false} otherwise
     */
    private static boolean isDevMode(String[] args) {
        if (Boolean.getBoolean("devMode"))  //Add VM option -DdevMode=true
            return true;
        if ("true".equalsIgnoreCase(System.getenv("DEV_MODE")))  //Environment variable DEV_MODE=true
            return true;
        return Arrays.asList(args).contains("--dev"); //Argument --dev
    }

    /**
     * Reads configuration with precedence: Java system property first, then environment variable.
     * Returns trimmed value or null if neither source provides a non-empty value.
     */
    private static String resolveConfig(String propertyKey, String envKey) {
        String v = System.getProperty(propertyKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(envKey);
        }
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }

    private void startUpLogIn(Connection connection) throws SQLException {
        boolean running = true;
        while (running) {
            System.out.println("Input Username: ");
            String userName = scanner.next();
            System.out.println("Input Password: ");
            String password = scanner.next();
            String query = "select count(*) from account where name = ? and password = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, userName);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    if (rs.getInt(1) != 0) {
                        running = false;
                    } else {
                        System.out.println("Invalid username or password, would you like to exit? (0) ");
                        temp = scanner.nextLine();
                        if (temp.equals("0"))
                            running = false;
                    }
                }
            }
        }
    }

    private void cliMenu(Connection connection) throws SQLException {
        boolean running = !temp.equals("0");
        while(running) {
            cliWriter();
            System.out.println("Please provide the command line arguments to execute: ");
            String arg = scanner.nextLine();

            switch (arg) {
                case "1":
                case "List":
                    databaseLister(connection);
                    break;

                case "2":
                case "Get":
                    databaseGetter(connection);
                    break;
                case "3":
                case "Count":
                    databaseCounter(connection);
                    break;
                case "4":
                case "Create":
                    databaseCreator(connection);
                    break;
                case "5":
                case "Update":
                    databaseUpdated(connection);
                    break;
                case "6":
                case "Delete":
                    databaseDeleter(connection);
                    break;
                case "0":
                case "Exit":
                    running = false;
            }
            System.out.println(" ");
        }
    }

    private void databaseDeleter(Connection connection) throws SQLException {
        System.out.println("Please enter the user_id to delete: ");
        String idToDelete = scanner.nextLine();
        String query = "delete from account where user_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, idToDelete);
            stmt.executeUpdate();
            System.out.println("Account deleted.");
        }
    }

    private void databaseUpdated(Connection connection) throws SQLException {
        boolean check = false;
        System.out.println("Please enter the user_id of the account you would like to update: ");
        String inputId = scanner.nextLine();
        String query = "select count(*) from account where user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, inputId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getInt(1) != 0)
                    check = true;
            }
        }

        if (check) {
            System.out.println("Provide new password: ");
            String newPassword = scanner.nextLine();
            query = "update account set password=? where user_id =?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, newPassword);
                stmt.setString(2, inputId);
                stmt.executeUpdate();
                System.out.println("New password updated.");
            }
        } else
            System.out.println("invalid user_id");
    }

    private void databaseCreator(Connection connection) throws SQLException {
        System.out.println("Provide firstname: ");
        String firstnameInput = scanner.nextLine();
        System.out.println("Provide lastname: ");
        String lastnameInput = scanner.nextLine();
        System.out.println("Provide ssn: ");
        String ssnInput = scanner.nextLine();
        System.out.println("Provide password: ");
        String passwordInput = scanner.nextLine();
        String nameInput = (substring(firstnameInput, 0, 2)).concat(substring(lastnameInput, 0, 2));

        String query = "insert into account (first_name, last_name, ssn, password, name) values (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, firstnameInput);
            stmt.setString(2, lastnameInput);
            stmt.setString(3, ssnInput);
            stmt.setString(4, passwordInput);
            stmt.setString(5, nameInput);
            stmt.executeUpdate();
            System.out.println("Account created successfully");
        }
    }

    private void databaseCounter(Connection connection) throws SQLException {
        System.out.println("Provide year: ");
        String year = scanner.nextLine();
        String query = "select count(*) from moon_mission where year(launch_date) = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, year);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String message = "The amount of missions launched year " + year + " was " + rs.getString(1) + ".";
                System.out.println(message);
            }
        }
    }

    private void databaseGetter(Connection connection) throws SQLException {
        System.out.println("Provide the mission id: ");
        String id = scanner.nextLine();
        String query = "select * from moon_mission where mission_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String message = rs.getString(1)
                        + " " + rs.getString(2)
                        + " " + rs.getString(3)
                        + " " + rs.getString(4)
                        + " " + rs.getString(5)
                        + " " + rs.getString(6)
                        + " " + rs.getString(7);
                System.out.println(message);
            }
        }
    }

    private void databaseLister(Connection connection) throws SQLException {
        String query = "select spacecraft from moon_mission";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString(1));
            }
        }
    }

    private static void cliWriter() {
        System.out.println("""
                   1) List moon missions (prints spacecraft names from `moon_mission`).
                   2) Get a moon mission by mission_id (prints details for that mission).
                   3) Count missions for a given year (prompts: year; prints the number of missions launched that year).
                   4) Create an account (prompts: first name, last name, ssn, password; prints confirmation).
                   5) Update an account password (prompts: user_id, new password; prints confirmation).
                   6) Delete an account (prompts: user_id; prints confirmation).
                   0) Exit.
                """);
    }
}
