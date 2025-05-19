package Election;

import java.io.IOException;
import java.io.PrintWriter;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class MainWindow extends Application {
    private static final String PASSWORD = "admin";
    private final ObservableList<Election.Major> majorsList = FXCollections.observableArrayList();
    private final Queue<Election.Major> votingQueue = new LinkedList<>();
    private TableView<Election.Major> tableView;

    @Override
    public void start(Stage primaryStage) {
        if (!authorizeAccess()) return;

        primaryStage.setTitle("Система Виборів");

        tableView = new TableView<>();
        setupTableColumns();
        loadMajorsFromDB();

        // Поля введення
        TextField surnameField = new TextField();
        surnameField.setPromptText("Прізвище");

        TextField nameField = new TextField();
        nameField.setPromptText("Ім’я");

        TextField patronymicField = new TextField();
        patronymicField.setPromptText("По батькові");

        TextField birthplaceField = new TextField();
        birthplaceField.setPromptText("Місце народження");

        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll("ч", "ж");
        genderBox.setValue("ч");

        TextField birthYearField = new TextField();
        birthYearField.setPromptText("Рік народження");

        ComboBox<String> popularityBox = new ComboBox<>();
        popularityBox.getItems().addAll(
                "Підтримка президента (70)",
                "Опозиційна партія (15)",
                "Опозиційний кандидат (10)",
                "Інше (5)"
        );
        popularityBox.setValue("Підтримка президента (70)");

        Button addButton = new Button("Додати");
        addButton.setOnAction(e -> {
            try {
                int year = Integer.parseInt(birthYearField.getText().trim());
                if (year < 1900 || year > 2025) {
                    showAlert("Помилка", "Некоректний рік народження");
                    return;
                }

                int index = switch (popularityBox.getSelectionModel().getSelectedIndex()) {
                    case 0 -> 70;
                    case 1 -> 15;
                    case 2 -> 10;
                    case 3 -> 5;
                    default -> 0;
                };

                String gender = genderBox.getValue();
                String photoPath = gender.equals("ч") ? "images/man.jpg" : "images/woman.jpg";
                if (surnameField.getText().trim().isEmpty() ||
                        nameField.getText().trim().isEmpty() ||
                        patronymicField.getText().trim().isEmpty() ||
                        birthplaceField.getText().trim().isEmpty()) {
                    showAlert("Помилка", "Усі текстові поля повинні бути заповнені!");
                    return;
                }

                Election.Major major = new Election.Major(
                        surnameField.getText().trim(),
                        nameField.getText().trim(),
                        patronymicField.getText().trim(),
                        birthplaceField.getText().trim(),
                        gender,
                        year,
                        index
                );
                addMajorToDB(major);
                votingQueue.offer(major);

                surnameField.clear();
                nameField.clear();
                patronymicField.clear();
                birthplaceField.clear();
                birthYearField.clear();
                loadMajorsFromDB();

            } catch (NumberFormatException ex) {
                showAlert("Помилка", "Введіть коректний рік!");
            }
        });

        Button deleteButton = new Button("Видалити за прізвищем");
        TextField deleteSurnameField = new TextField();
        deleteSurnameField.setPromptText("Прізвище");
        deleteButton.setOnAction(e -> {
            deleteMajorBySurname(deleteSurnameField.getText().trim());
            deleteSurnameField.clear();
            loadMajorsFromDB();
        });

        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll("Прізвище", "Ім’я", "Популярність", "Стать", "Місто");
        sortBox.setValue("Прізвище");

        Button sortButton = new Button("Сортувати");
        sortButton.setOnAction(e -> {
            Comparator<Election.Major> comparator = switch (sortBox.getValue()) {
                case "Прізвище" -> Comparator.comparing(m -> m.surname);
                case "Ім’я" -> Comparator.comparing(m -> m.name);
                case "Популярність" -> Comparator.comparingInt(m -> -m.indexPopularity);
                case "Стать" -> Comparator.comparing(m -> m.gender);
                case "Місто" -> Comparator.comparing(m -> m.birthplace);
                default -> null;
            };
            if (comparator != null) {
                majorsList.setAll(majorsList.stream().sorted(comparator).collect(Collectors.toList()));
            }
        });

        Button editButton = new Button("Редагувати");
        Button updateButton = new Button("Оновити");
        editButton.setOnAction(ed -> {
            Election.Major selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Помилка", "Виберіть кандидата для редагування.");
                return;
            }

            surnameField.setText(selected.surname);
            nameField.setText(selected.name);
            patronymicField.setText(selected.patronymic);
            birthplaceField.setText(selected.birthplace);
            genderBox.setValue(selected.gender);
            birthYearField.setText(String.valueOf(selected.birthYear));

            int index = selected.indexPopularity;
            if (index == 70) popularityBox.setValue("Підтримка президента (70)");
            else if (index == 15) popularityBox.setValue("Опозиційна партія (15)");
            else if (index == 10) popularityBox.setValue("Опозиційний кандидат (10)");
            else popularityBox.setValue("Інше (5)");

            addButton.setDisable(true); // тимчасово блокувати додавання
        });
        updateButton.setOnAction(ep -> {
            Election.Major selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Помилка", "Виберіть кандидата для оновлення.");
                return;
            }

            try {
                int year = Integer.parseInt(birthYearField.getText().trim());
                int index = switch (popularityBox.getSelectionModel().getSelectedIndex()) {
                    case 0 -> 70;
                    case 1 -> 15;
                    case 2 -> 10;
                    case 3 -> 5;
                    default -> 0;
                };

                String photoPath = genderBox.getValue().equals("ч") ? "images/man.jpg" : "images/woman.jpg";

                String sql = "UPDATE majors SET name=?, patronymic=?, birthplace=?, gender=?, birth_year=?, index_popularity=?, photo_path=? WHERE surname=?";
                try (Connection conn = DataBaseManager.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, nameField.getText().trim());
                    pstmt.setString(2, patronymicField.getText().trim());
                    pstmt.setString(3, birthplaceField.getText().trim());
                    pstmt.setString(4, genderBox.getValue());
                    pstmt.setInt(5, year);
                    pstmt.setInt(6, index);
                    pstmt.setString(7, photoPath);
                    pstmt.setString(8, surnameField.getText().trim());
                    pstmt.executeUpdate();
                }

                loadMajorsFromDB(); // оновити таблицю
                addButton.setDisable(false); // розблокувати додавання

                // очистити поля
                surnameField.clear(); nameField.clear(); patronymicField.clear();
                birthplaceField.clear(); birthYearField.clear();
            } catch (Exception ex) {
                showAlert("Помилка", "Невірні дані для оновлення.");
            }
        });

        Button voteListButton = new Button("Формувати список для голосування");
        voteListButton.setOnAction(e -> {
            List<Election.Major> votingList = majorsList.stream()
                    .sorted(Comparator.comparingInt(Election.Major::getIndexPopularity).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            Stage voteStage = new Stage();
            voteStage.setTitle("Список для голосування");

            TableView<Election.Major> voteTable = new TableView<>();
            voteTable.setItems(FXCollections.observableArrayList(votingList));

            TableColumn<Election.Major, String> surnameCol = new TableColumn<>("Прізвище");
            surnameCol.setCellValueFactory(new PropertyValueFactory<>("surname"));

            TableColumn<Election.Major, String> nameCol = new TableColumn<>("Ім’я");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

            TableColumn<Election.Major, String> birthplaceCol = new TableColumn<>("Місце народження");
            birthplaceCol.setCellValueFactory(new PropertyValueFactory<>("birthplace"));

            TableColumn<Election.Major, String> genderCol = new TableColumn<>("Стать");
            genderCol.setCellValueFactory(new PropertyValueFactory<>("gender"));

            TableColumn<Election.Major, Integer> popularityCol = new TableColumn<>("Популярність");
            popularityCol.setCellValueFactory(new PropertyValueFactory<>("indexPopularity"));

            voteTable.getColumns().addAll(surnameCol, nameCol, birthplaceCol, genderCol, popularityCol);

            // Кнопка друку
            Button printButton = new Button("Друк");
            printButton.setOnAction(ev -> {
                StringBuilder sb = new StringBuilder("=== Список для голосування ===\n");
                for (Election.Major m : votingList) {
                    sb.append(m.getSurname()).append(" ").append(m.getName())
                            .append(" (").append(m.getBirthplace()).append(") - ")
                            .append("Популярність: ").append(m.getIndexPopularity()).append("\n");
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Друк");
                alert.setHeaderText("Імітація друку:");
                alert.setContentText(sb.toString());
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.showAndWait();
            });

            // Кнопка збереження у файл
            Button saveButton = new Button("Зберегти у файл");
            saveButton.setOnAction(ev -> {
                try (PrintWriter writer = new PrintWriter("voting_list.txt")) {
                    for (Election.Major m : votingList) {
                        writer.println(m.getSurname() + " " + m.getName() + " (" + m.getBirthplace() + ") - Популярність: " + m.getIndexPopularity());
                    }
                    showAlert("Успіх", "Список збережено у файл voting_list.txt");
                } catch (IOException ex) {
                    showAlert("Помилка", "Помилка збереження у файл: " + ex.getMessage());
                }
            });


            HBox buttons = new HBox(10, printButton, saveButton);
            buttons.setPadding(new Insets(10));

            VBox vbox = new VBox(10, voteTable, buttons);
            vbox.setPadding(new Insets(10));
            Scene scene = new Scene(vbox, 600, 350);
            voteStage.setScene(scene);
            voteStage.show();
        });



        HBox inputRow1 = new HBox(10, surnameField, nameField, patronymicField, birthplaceField);
        HBox inputRow2 = new HBox(10, genderBox, birthYearField, popularityBox, addButton);
        HBox deleteRow = new HBox(10, deleteSurnameField, deleteButton);

        HBox sortRow = new HBox(10, sortBox, sortButton, voteListButton, editButton, updateButton);
        VBox layout = new VBox(10, tableView, inputRow1, inputRow2, deleteRow, sortRow);

        layout.setPadding(new Insets(15));

        primaryStage.setScene(new Scene(layout, 1200, 600));
        primaryStage.show();
    }
    private boolean authorizeAccess() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Авторизація");
        dialog.setHeaderText("Введіть пароль для входу в систему");

        ButtonType loginButtonType = new ButtonType("Увійти", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");

        VBox content = new VBox(10, new Label("Пароль:"), passwordField);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        return dialog.showAndWait().map(input -> {
            if (!input.equals(PASSWORD)) {
                showAlert("Доступ заборонено", "Невірний пароль!");
                return false;
            }
            return true;
        }).orElse(false);
    }


    private void setupTableColumns() {
        TableColumn<Election.Major, String> surnameCol = new TableColumn<>("Прізвище");
        surnameCol.setCellValueFactory(new PropertyValueFactory<>("surname"));

        TableColumn<Election.Major, String> nameCol = new TableColumn<>("Ім’я");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Election.Major, String> patronymicCol = new TableColumn<>("По батькові");
        patronymicCol.setCellValueFactory(new PropertyValueFactory<>("patronymic"));

        TableColumn<Election.Major, String> birthplaceCol = new TableColumn<>("Місце народження");
        birthplaceCol.setCellValueFactory(new PropertyValueFactory<>("birthplace"));

        TableColumn<Election.Major, String> genderCol = new TableColumn<>("Стать");
        genderCol.setCellValueFactory(new PropertyValueFactory<>("gender"));

        TableColumn<Election.Major, Integer> yearCol = new TableColumn<>("Рік народження");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("birthYear"));

        TableColumn<Election.Major, Integer> popularityCol = new TableColumn<>("Популярність");
        popularityCol.setCellValueFactory(new PropertyValueFactory<>("indexPopularity"));

        TableColumn<Election.Major, String> photoCol = new TableColumn<>("Фото");
        photoCol.setCellFactory(column -> new TableCell<>() {
            private final ImageView imageView = new ImageView();

            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null) {
                    setGraphic(null);
                } else {
                    imageView.setImage(new Image(path, 40, 40, true, true));
                    setGraphic(imageView);
                }
            }
        });
        photoCol.setCellValueFactory(new PropertyValueFactory<>("photoPath"));

        tableView.getColumns().addAll(photoCol, surnameCol, nameCol, patronymicCol, birthplaceCol, genderCol, yearCol, popularityCol);
        tableView.setItems(majorsList);
    }

    private void loadMajorsFromDB() {
        majorsList.clear();
        String sql = "SELECT * FROM majors";

        try (Connection conn = DataBaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Election.Major major = new Election.Major(
                        rs.getString("surname"),
                        rs.getString("name"),
                        rs.getString("patronymic"),
                        rs.getString("birthplace"),
                        rs.getString("gender"),
                        rs.getInt("birth_year"),
                        rs.getInt("index_popularity")
                );
                majorsList.add(major);
            }

        } catch (SQLException e) {
            showAlert("Помилка", "Не вдалося завантажити дані: " + e.getMessage());
        }
    }

    private void addMajorToDB(Election.Major major) {
        String sql = "INSERT INTO majors (surname, name, patronymic, birthplace, gender, birth_year, index_popularity, photo_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DataBaseManager.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, major.surname);
            pstmt.setString(2, major.name);
            pstmt.setString(3, major.patronymic);
            pstmt.setString(4, major.birthplace);
            pstmt.setString(5, major.gender);
            pstmt.setInt(6, major.birthYear);
            pstmt.setInt(7, major.indexPopularity);
            pstmt.setString(8, major.photoPath);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Помилка", "Не вдалося додати кандидата: " + e.getMessage());
        }
    }

    private void deleteMajorBySurname(String surname) {
        String sql = "DELETE FROM majors WHERE surname = ?";

        try (Connection conn = DataBaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, surname);
            int affected = pstmt.executeUpdate();

            if (affected == 0) {
                showAlert("Інфо", "Кандидата не знайдено.");
            }

        } catch (SQLException e) {
            showAlert("Помилка", "Не вдалося видалити: " + e.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
