package Election;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseManager {
    private static final String URL = "jdbc:postgresql://34.116.209.199/election_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1111";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS majors (
                id SERIAL PRIMARY KEY,
                surname VARCHAR(50),
                initials VARCHAR(10),
                birthplace VARCHAR(100),
                birthYear INT,
                indexPopularity INT
            );
        """;

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Таблиця створена або вже існує.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
