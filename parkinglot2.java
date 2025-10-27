import java.awt.*;
import java.sql.*;
import java.util.Scanner;
import javax.swing.*;

public class parkinglot2{
    static final String DB_URL = "jdbc:mysql://localhost:3306/parkinglot2";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "anuj@2007";
    static final int MAX_SLOTS = 10;
    static Scanner input = new Scanner(System.in);
    static ParkingGUI gui;

    public static void main(String[] args) {
        gui = new ParkingGUI(); // Initialize GUI
        refreshGUI(); // Initial visual update from DB

        System.out.println("--- Welcome to Parking Lot System ---");
        System.out.print("Login as (admin/user): ");
        String role = input.nextLine().toLowerCase();

        if (role.equals("admin")) {
            if (adminLogin()) {
                adminMenu();
            } else {
                System.out.println("Login failed.");
            }
        } else if (role.equals("user")) {
            userLogin();
        } else {
            System.out.println("Invalid role.");
        }
    }

    static boolean adminLogin() {
        System.out.print("Enter username: ");
        String username = input.nextLine();
        System.out.print("Enter password: ");
        String password = input.nextLine();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND role = 'admin'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    static void adminMenu() {
        while (true) {
            showAvailableSlots();
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Park Vehicle");
            System.out.println("2. Remove Vehicle");
            System.out.println("3. Show Vehicles");
            System.out.println("4. Search Vehicle");
            System.out.println("5. Logout");
            System.out.print("Choose: ");
            int choice = input.nextInt();
            input.nextLine();

            switch (choice) {
                case 1 -> parkVehicle();
                case 2 -> removeVehicle();
                case 3 -> showVehicles();
                case 4 -> searchVehicle();
                case 5 -> {
                    System.out.println("Logged out.");
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    static void userLogin() {
        System.out.print("Enter your name: ");
        String name = input.nextLine();
        System.out.println("Welcome, " + name + "! You can only view parked vehicles.");
        showVehicles();
    }

    static void showAvailableSlots() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String countQuery = "SELECT COUNT(*) FROM vehicles";
            try (PreparedStatement stmt = conn.prepareStatement(countQuery);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int used = rs.getInt(1);
                    System.out.println("Available Slots: " + (MAX_SLOTS - used) + "/" + MAX_SLOTS);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void parkVehicle() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String countQuery = "SELECT COUNT(*) FROM vehicles";
            int usedSlots = 0;
            try (PreparedStatement stmt = conn.prepareStatement(countQuery);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    usedSlots = rs.getInt(1);
                    if (usedSlots >= MAX_SLOTS) {
                        System.out.println("Parking Full!");
                        return;
                    }
                }
            }

            System.out.print("Enter vehicle number: ");
            String number = input.nextLine();
            System.out.print("Enter vehicle type: ");
            String type = input.nextLine();

            String insertQuery = "INSERT INTO vehicles (vehicle_number, vehicle_type, parked_time) VALUES (?, ?, NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setString(1, number);
                stmt.setString(2, type);
                stmt.executeUpdate();

                String carArt =
                        "        ______\n" +
                        "       /|||\\`.\n" +
                        "      (   _    _ _\\\n" +
                        "      =`-()--()-'\n";
                System.out.println(carArt + "Vehicle parked.");

                gui.parkVehicle(usedSlots, number);  // update GUI slot
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void removeVehicle() {
        System.out.print("Enter vehicle number to remove: ");
        String number = input.nextLine();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String timeQuery = "SELECT parked_time FROM vehicles WHERE vehicle_number = ?";
            Timestamp parkedTime = null;

            try (PreparedStatement timeStmt = conn.prepareStatement(timeQuery)) {
                timeStmt.setString(1, number);
                ResultSet rsTime = timeStmt.executeQuery();
                if (rsTime.next()) {
                    parkedTime = rsTime.getTimestamp("parked_time");
                }
            }

            int removedIndex = getVehicleSlotIndex(number);

            String deleteQuery = "DELETE FROM vehicles WHERE vehicle_number = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                stmt.setString(1, number);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Vehicle removed.");
                    gui.removeVehicle(removedIndex);  // update GUI
                    if (parkedTime != null) {
                        long durationMillis = System.currentTimeMillis() - parkedTime.getTime();
                        long minutes = durationMillis / (1000 * 60);
                        System.out.println("Vehicle was parked for " + minutes + " minutes.");
                    }
                } else {
                    System.out.println("Vehicle not found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void showVehicles() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String selectQuery = "SELECT vehicle_number, vehicle_type, parked_time FROM vehicles";
            try (PreparedStatement stmt = conn.prepareStatement(selectQuery);
                 ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("No vehicles parked.");
                    return;
                }

                System.out.println("Parked Vehicles:");
                do {
                    System.out.println(rs.getString("vehicle_number") + " - " +
                                       rs.getString("vehicle_type") + " | Parked at: " +
                                       rs.getTimestamp("parked_time"));
                } while (rs.next());
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void searchVehicle() {
        System.out.print("Enter vehicle number to search: ");
        String number = input.nextLine();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM vehicles WHERE vehicle_number = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, number);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.println("Vehicle Found: " +
                            rs.getString("vehicle_number") + " - " +
                            rs.getString("vehicle_type") + " | Parked at: " +
                            rs.getTimestamp("parked_time"));
                } else {
                    System.out.println("Vehicle not found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static int getVehicleSlotIndex(String vehicleNumber) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT vehicle_number FROM vehicles ORDER BY parked_time";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                int index = 0;
                while (rs.next()) {
                    if (rs.getString("vehicle_number").equals(vehicleNumber)) {
                        return index;
                    }
                    index++;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return -1;
    }

    static void refreshGUI() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT vehicle_number FROM vehicles ORDER BY parked_time";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                int index = 0;
                while (rs.next() && index < MAX_SLOTS) {
                    gui.parkVehicle(index, rs.getString("vehicle_number"));
                    index++;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // -------------------- GUI CLASS -----------------------
    static class ParkingGUI {
        private JFrame frame;
        private JLabel[] slots;

        public ParkingGUI() {
            frame = new JFrame("Parking Lot Visual");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLayout(new GridLayout(5, 2));

            slots = new JLabel[10];
            for (int i = 0; i < 10; i++) {
                slots[i] = new JLabel("Slot " + (i + 1) + ": Empty", SwingConstants.CENTER);
                slots[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                frame.add(slots[i]);
            }

            frame.setVisible(true);
        }

        public void parkVehicle(int index, String number) {
            if (index >= 0 && index < 10) {
                slots[index].setText("Slot " + (index + 1) + ": " + number);
                slots[index].setForeground(Color.BLUE);
            }
        }

        public void removeVehicle(int index) {
            if (index >= 0 && index < 10) {
                slots[index].setText("Slot " + (index + 1) + ": Empty");
                slots[index].setForeground(Color.BLACK);
}
}
}
}
