package Election;

import java.sql.*;
import java.util.Scanner;

public class Election {
    private static final Scanner scanner = new Scanner(System.in);
    private static final String PASSWORD = "admin";

    public static class Major {
        public String surname, name, patronymic, birthplace, gender, photoPath;
        public int birthYear, indexPopularity;

        public Major(String surname, String name, String patronymic, String birthplace, String gender, int birthYear, int indexPopularity) {
            this.surname = surname;
            this.name = name;
            this.patronymic = patronymic;
            this.birthplace = birthplace;
            this.gender = gender;
            this.birthYear = birthYear;
            this.indexPopularity = indexPopularity;
            this.photoPath = gender.equalsIgnoreCase("ч") ? "images/man.jpg" : "images/woman.jpg";
        }

        public String getSurname() { return surname; }
        public String getName() { return name; }
        public String getPatronymic() { return patronymic; }
        public String getBirthplace() { return birthplace; }
        public String getGender() { return gender; }
        public int getBirthYear() { return birthYear; }
        public int getIndexPopularity() { return indexPopularity; }
        public String getPhotoPath() { return photoPath; }
    }

    private static boolean authorizeAccess() {
        System.out.print("Введіть пароль для входу в програму: ");
        String input = scanner.nextLine().trim();
        return input.equals(PASSWORD);
    }

    private static void addMajorToDB() {
        System.out.print("Прізвище: ");
        String surname = scanner.nextLine();

        System.out.print("Ім’я: ");
        String name = scanner.nextLine();

        System.out.print("По батькові: ");
        String patronymic = scanner.nextLine();

        System.out.print("Місце народження: ");
        String birthplace = scanner.nextLine();

        String gender;
        while (true) {
            System.out.print("Стать (ч/ж): ");
            gender = scanner.nextLine().trim().toLowerCase();
            if (gender.equals("ч") || gender.equals("ж")) break;
            System.out.println("Будь ласка, введіть \"ч\" або \"ж\".");
        }

        int birthYear;
        while (true) {
            System.out.print("Рік народження: ");
            birthYear = getIntInput();
            if (birthYear > 1900 && birthYear <= 2025) break;
            System.out.println("Невірний рік народження. Введіть коректний рік у межах 1900-2025.");
        }

        System.out.println("\n1. Кандидата підтримав президент.");
        System.out.println("2. Кандидата підтримала опозиційна партія.");
        System.out.println("3. Опозиційний кандидат.");
        System.out.println("4. Жоден з перелічених.");
        System.out.print("Виберіть опцію: ");

        int choice = getIntInput();
        int popularityIndex = switch (choice) {
            case 1 -> 70;
            case 2 -> 15;
            case 3 -> 10;
            case 4 -> 5;
            default -> {
                System.out.println("Невірний вибір. Спробуйте ще раз.");
                yield 0;
            }
        };

        String photoPath = gender.equals("ч") ? "images/man.jpg" : "images/woman.jpg";

        String sql = "INSERT INTO majors (surname, name, patronymic, birthplace, gender, birth_year, index_popularity, photo_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DataBaseManager.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, surname);
            pstmt.setString(2, name);
            pstmt.setString(3, patronymic);
            pstmt.setString(4, birthplace);
            pstmt.setString(5, gender);
            pstmt.setInt(6, birthYear);
            pstmt.setInt(7, popularityIndex);
            pstmt.setString(8, photoPath);
            pstmt.executeUpdate();
            System.out.println("Кандидата додано!");
        } catch (SQLException e) {
            System.out.println("Помилка при додаванні кандидата: " + e.getMessage());
        }
    }

    private static void deleteMajorBySurname() {
        authorizeAccess();
        System.out.print("Введіть прізвище кандидата, якого потрібно видалити: ");
        String surnameToDelete = scanner.nextLine().trim();

        String sql = "DELETE FROM majors WHERE surname = ?";
        try (Connection conn = DataBaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, surnameToDelete);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Кандидата успішно видалено.");
            } else {
                System.out.println("Кандидата з таким прізвищем не знайдено.");
            }

        } catch (SQLException e) {
            System.out.println("Помилка при видаленні кандидата: " + e.getMessage());
        }
    }

    private static void printTableFromDB() {
        String sql = "SELECT * FROM majors";

        try (Connection conn = DataBaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n📋 Список кандидатів:");
            while (rs.next()) {
                System.out.printf(" %s %s %s, %s, %s, %d р.н., Популярність: %d, Фото: %s\n",
                        rs.getString("surname"),
                        rs.getString("name"),
                        rs.getString("patronymic"),
                        rs.getString("birthplace"),
                        rs.getString("gender"),
                        rs.getInt("birth_year"),
                        rs.getInt("index_popularity"),
                        rs.getString("photo_path"));
            }

        } catch (SQLException e) {
            System.out.println("Помилка при виведенні кандидатів: " + e.getMessage());
        }
    }

    private static int getIntInput() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Помилка! Введіть число: ");
            }
        }
    }
}
