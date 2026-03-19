import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PayrollSystemGUI extends Application {

    private static final String EMPLOYEE_CSV = "data/EmployeeData.csv";
    private static final String USERS_CSV = "data/Users.csv";
    private static final String ATTENDANCE_CSV = "data/employee_attendance.csv"; // Attendance data
    private static final String LOGO_PNG = "data/motorPH_logo.png";

    private static final Charset EMPLOYEE_CSV_CHARSET = Charset.forName("cp1252");

    private Stage primaryStage;
    private UserAccount session;  // The current logged-in user
    private final ObservableList<Employee> employees = FXCollections.observableArrayList();
    private final TableView<Employee> table = new TableView<>();
    private boolean darkModeEnabled = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("MotorPH Payroll System");
        stage.setScene(buildLoginScene());
        stage.show();
    }

    // ------------------------- Login Scene -------------------------
    private Scene buildLoginScene() {
        var root = new StackPane();
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1f2937);");

        var card = new VBox(14);
        card.setPadding(new Insets(22));
        card.setMaxWidth(380);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 22, 0.2, 0, 6);");

        var title = new Label("MotorPH Payroll");
        title.setFont(javafx.scene.text.Font.font(22));
        title.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: 700;");

        var subtitle = new Label("Sign in to continue");
        subtitle.setStyle("-fx-text-fill: #475569;");

        var logoView = buildLogoView();

        var username = new TextField();
        username.setPromptText("Username (admin or Employee # e.g. 10001)");
        styleInput(username);

        var password = new PasswordField();
        password.setPromptText("Password");
        styleInput(password);

        var loginBtn = new Button("Login");
        stylePrimaryButton(loginBtn);
        loginBtn.setDefaultButton(true);

        var msg = new Label("");
        msg.setStyle("-fx-text-fill: #b91c1c;");

        Runnable doLogin = () -> {
            try {
                String enteredUsername = safeTrim(username.getText());
                String enteredPassword = safeTrim(password.getText());
                if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
                    msg.setText("Enter both username and password.");
                    return;
                }

                this.session = authenticate(username.getText(), password.getText());
                if (this.session == null) {
                    msg.setText("Invalid username or password.");
                    return;
                }
                loadEmployeesFromCsv();
                if (!this.session.isAdmin() && employees.stream().noneMatch(e -> safe(e.getId()).equals(this.session.employeeId()))) {
                    msg.setText("Login succeeded, but no employee profile was found for ID " + this.session.employeeId() + ".");
                    this.session = null;
                    return;
                }
                msg.setText("");
                primaryStage.setScene(buildAppScene());
            } catch (Exception ex) {
                msg.setText("Login error: " + ex.getMessage());
            }
        };

        loginBtn.setOnAction(e -> doLogin.run());
        username.setOnAction(e -> doLogin.run());
        password.setOnAction(e -> doLogin.run());

        if (logoView != null) {
            card.getChildren().add(logoView);
        }
        card.getChildren().addAll(title, subtitle, spacer(6), username, password, loginBtn, msg);
        root.getChildren().add(card);
        return new Scene(root, 900, 600);
    }

    private ImageView buildLogoView() {
        Path logoPath = resolveDataPath(LOGO_PNG);
        if (logoPath == null) {
            return null;
        }

        Image logo = new Image(logoPath.toUri().toString());
        if (logo.isError()) {
            return null;
        }

        ImageView logoView = new ImageView(logo);
        logoView.setPreserveRatio(true);
        logoView.setFitWidth(140);
        logoView.setSmooth(true);
        logoView.setCache(true);
        return logoView;
    }

    private Path resolveDataPath(String relativePath) {
        Path[] candidates = new Path[]{
                Paths.get(relativePath),
                Paths.get("motorph").resolve(relativePath)
        };

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.exists(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private void loadEmployeesFromCsv() throws IOException, CsvValidationException {
        employees.clear();
        File f = new File(EMPLOYEE_CSV);
        if (!f.exists()) {
            return;
        }

        try (CSVReader reader = new CSVReader(new FileReader(f, EMPLOYEE_CSV_CHARSET))) {
            String[] header = reader.readNext();
            if (header == null) {
                return;
            }
            Map<String, Integer> headerIndexes = buildHeaderIndexMap(header);
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 9) {
                    System.out.println("Skipping malformed employee row (length=" + row.length + "): " + Arrays.toString(row));
                    continue;
                }

                String id = readText(row, headerIndexes, 0, "Employee #");
                String lastName = readText(row, headerIndexes, 1, "Last Name");
                String firstName = readText(row, headerIndexes, 2, "First Name");
                String birthdate = readText(row, headerIndexes, 3, "Birthday");
                String address = readText(row, headerIndexes, 4, "Address");
                String phoneNumber = readText(row, headerIndexes, 5, "Phone Number");
                String sssNumber = readText(row, headerIndexes, 6, "SSS Number");
                String philhealthNumber = readText(row, headerIndexes, 7, "PhilHealth Number");
                String tinNumber = readText(row, headerIndexes, -1, "TIN Number");
                String pagibigNumber = readText(row, headerIndexes, 8, "PAG-IBIG Number");
                String employmentType = resolveEmploymentType(row, headerIndexes);
                String positionStr = readText(row, headerIndexes, 9, "Position");
                String department = readText(row, headerIndexes, 10, "Department");
                double basicSalary = readAmount(row, headerIndexes, 11, "Basic Salary", 0.0);
                double riceSubsidy = readAmount(row, headerIndexes, 12, "Rice Subsidy", 0.0);
                double phoneAllowance = readAmount(row, headerIndexes, 13, "Phone Allowance", 0.0);
                double clothingAllowance = readAmount(row, headerIndexes, 14, "Clothing Allowance", 0.0);
                double grossSemiMonthlyRate = readAmount(row, headerIndexes, -1, "Gross Semi-Monthly Rate", basicSalary / 2.0);
                double hourlyRate = readAmount(row, headerIndexes, -1, "Hourly Rate", deriveHourlyRate(basicSalary, employmentType));

                EmployeeFormData formData = new EmployeeFormData(
                        id,
                        lastName,
                        firstName,
                        birthdate,
                        address,
                        phoneNumber,
                        sssNumber,
                        philhealthNumber,
                        tinNumber,
                        pagibigNumber,
                        "contract".equals(employmentType) ? EmployeeFormData.CONTRACT : EmployeeFormData.FULL_TIME,
                        positionStr,
                        department,
                        basicSalary,
                        riceSubsidy,
                        phoneAllowance,
                        clothingAllowance,
                        hourlyRate
                );
                Employee emp = formData.toEmployee();
                emp.setGrossSemiMonthlyRate(grossSemiMonthlyRate);
                employees.add(emp);
            }
        }

        try {
            Employee.loadContributionRates();
        } catch (Exception ignore) {
        }
    }

    private UserAccount authenticate(String rawUser, String rawPass) throws IOException, CsvValidationException {
        String username = safeTrim(rawUser);
        String password = safeTrim(rawPass);
        if (username.isEmpty() || password.isEmpty()) {
            return null;
        }

        for (UserAccount account : readUsers(USERS_CSV)) {
            if (account.matchesCredentials(username, password)) {
                return account;
            }
        }
        return null;
    }

    private List<UserAccount> readUsers(String filePath) throws IOException, CsvValidationException {
        File f = new File(filePath);
        if (!f.exists()) {
            throw new FileNotFoundException("Missing " + filePath);
        }

        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            reader.readNext();
            List<UserAccount> out = new ArrayList<>();
            String[] row;

            while ((row = reader.readNext()) != null) {
                if (row.length < 4) {
                    System.out.println("Skipping malformed row: " + Arrays.toString(row));
                    continue;
                }

                String u = safeTrim(row[0]);
                String p = safeTrim(row[1]);
                String role = safeTrim(row[2]);
                String employeeId = safeTrim(row[3]);
                boolean isAdmin = "admin".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(u);
                if (!isAdmin && employeeId.isEmpty()) {
                    employeeId = u;
                }
                out.add(new UserAccount(u, p, isAdmin, employeeId));
            }

            return out;
        }
    }

    // ------------------------- Main App UI -------------------------
    private Scene buildAppScene() {
        var root = new BorderPane();
        root.getStyleClass().addAll("app-root", darkModeEnabled ? "theme-dark" : "theme-light");

        root.setTop(buildTopBar(root));
        root.setCenter(buildCenter());
        root.setRight(buildRightPanel());
        root.setPadding(new Insets(16));
        var scene = new Scene(root, 1100, 700);
        attachStylesheet(scene);
        return scene;
    }

    private void attachStylesheet(Scene scene) {
        var stylesheet = getClass().getResource("styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private HBox buildTopBar(BorderPane root) {
        var title = new Label("Payroll Dashboard");
        title.getStyleClass().add("app-topbar-title");

        var badge = new Label(session.isAdmin() ? "ADMIN" : "USER");
        badge.getStyleClass().addAll("app-role-badge", session.isAdmin() ? "app-role-admin" : "app-role-user");

        String userDisplay = session.isAdmin() ? session.username() : ("Employee # " + session.employeeId());
        var who = new Label(userDisplay);
        who.getStyleClass().add("app-topbar-user");

        var themeToggle = new ToggleButton();
        themeToggle.setSelected(darkModeEnabled);
        themeToggle.getStyleClass().add("theme-toggle");
        updateThemeToggleText(themeToggle);
        themeToggle.setTooltip(new Tooltip("Switch between light mode and dark mode."));
        themeToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            darkModeEnabled = isSelected;
            primaryStage.setScene(buildAppScene());
        });

        var logout = new Button("Logout");
        styleGhostButton(logout);
        logout.setOnAction(e -> handleLogout());

        var left = new HBox(12, title, badge);
        left.setAlignment(Pos.CENTER_LEFT);

        var right = new HBox(12, themeToggle, who, logout);
        right.setAlignment(Pos.CENTER_RIGHT);

        var bar = new HBox();
        bar.getStyleClass().add("app-topbar");
        bar.setPadding(new Insets(6, 6, 14, 6));
        HBox.setHgrow(left, Priority.ALWAYS);
        bar.getChildren().addAll(left, right);
        return bar;
    }

    private void handleLogout() {
        session = null;
        darkModeEnabled = false;
        employees.clear();
        table.getSelectionModel().clearSelection();
        table.getItems().clear();
        primaryStage.setScene(buildLoginScene());
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();
    }

    private void applyTheme(Pane root) {
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        root.getStyleClass().add(darkModeEnabled ? "theme-dark" : "theme-light");
    }

    private void updateThemeToggleText(ToggleButton toggle) {
        toggle.setText(darkModeEnabled ? "Dark Mode" : "Light Mode");
    }

    private VBox buildCenter() {
        if (session.isAdmin()) {
            return buildAdminDashboard();
        } else {
            return buildEmployeeDashboard();
        }
    }

    private VBox buildAdminDashboard() {
        var card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle(adminCardStyle());
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);

        var header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 8, 0));

        var label = new Label("Employees");
        label.setStyle(adminHeadingStyle());

        var refresh = new Button("Refresh");
        styleGhostButton(refresh);
        refresh.setOnAction(e -> {
            try {
                loadEmployeesFromCsv();
            } catch (Exception ex) {
                showError("Refresh failed", ex.getMessage());
            }
        });

        header.getChildren().addAll(label, spacer(1), refresh);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        configureTable();
        reloadTableItems();
        var attendanceInsights = buildAdminAttendanceInsightsSection();
        card.getChildren().addAll(header, table, attendanceInsights);
        VBox.setVgrow(table, Priority.ALWAYS);

        return card;
    }

    private VBox buildEmployeeDashboard() {
        Employee currentEmployee = employees.stream()
                .filter(e -> safe(e.getId()).equals(session.employeeId()))
                .findFirst()
                .orElse(null);

        if (currentEmployee == null) {
            var root = new VBox(18);
            root.setPadding(new Insets(4));
            root.getStyleClass().add("employee-dashboard");
            var errorCard = new VBox(12);
            errorCard.setPadding(new Insets(30));
            errorCard.setAlignment(Pos.CENTER);
            errorCard.getStyleClass().add("employee-empty-state");

            var errorLabel = new Label("Employee profile not found");
            errorLabel.getStyleClass().add("employee-empty-state-text");
            errorCard.getChildren().add(errorLabel);
            root.getChildren().add(errorCard);
            return root;
        }

        return buildRefactoredEmployeeDashboard(currentEmployee);
    }

    private VBox buildWelcomeSection(Employee emp) {
        var card = new VBox(12);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #2563eb, #1d4ed8); -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 16, 0.15, 0, 6);");

        var greeting = new Label("Welcome back, " + safe(emp.getFirstName()) + "! 👋");
        greeting.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: 700;");

        var subtext = new Label("Employee ID: " + safe(emp.getId()) + " • " + safe(emp.getDepartment()));
        subtext.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13;");

        card.getChildren().addAll(greeting, subtext);
        return card;
    }

    private HBox buildStatisticsCards(Employee emp) {
        var container = new HBox(14);
        container.setStyle("-fx-background-color: transparent;");
        container.setPadding(new Insets(0, 0, 8, 0));

        // Load contribution data
        try {
            Employee.loadContributionRates();
        } catch (Exception ignored) {
        }

        // Salary Card
        var salaryCard = buildStatCard(getCompensationCardTitle(emp),
            formatCurrency(getPrimaryCompensationValue(emp)),
            getCompensationCardSubtitle(emp),
            "#3b82f6", "stat-card stat-card-blue");
        HBox.setHgrow(salaryCard, Priority.ALWAYS);
        container.getChildren().add(salaryCard);

        // Position Card
        var positionCard = buildStatCard("Position",
            safe(emp.getPosition()),
            safe(emp.getDepartment()),
            "#10b981", "stat-card stat-card-green");
        HBox.setHgrow(positionCard, Priority.ALWAYS);
        container.getChildren().add(positionCard);

        // Gross Salary Card
        var grossCard = buildStatCard("Gross Pay",
            formatCurrency(getDashboardGrossPayValue(emp)),
            getGrossPayCardSubtitle(emp),
            "#f59e0b", "stat-card stat-card-amber");
        HBox.setHgrow(grossCard, Priority.ALWAYS);
        container.getChildren().add(grossCard);

        // Benefits Card
        double totalBenefits = emp.getRiceSubsidy() + emp.getPhoneAllowance() + emp.getClothingAllowance();
        var benefitsCard = buildStatCard("Total Benefits",
            String.format("₱%.2f", totalBenefits),
            "Rice, phone, clothing",
            "#8b5cf6", "stat-card stat-card-purple");
        HBox.setHgrow(benefitsCard, Priority.ALWAYS);
        container.getChildren().add(benefitsCard);

        return container;
    }

    private VBox buildStatCard(String title, String value, String subtitle, String colorHex, String cssClass) {
        var card = new VBox(8);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: " + colorHex + "22; -fx-border-color: " + colorHex + 
                      "44; -fx-border-width: 1.5; -fx-background-radius: 12; -fx-border-radius: 12; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.1, 0, 2); -fx-min-height: 140;");
        card.setMaxWidth(Double.MAX_VALUE);
        
        var titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12; -fx-font-weight: 600;");

        var valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: 700;");
        valueLabel.setWrapText(true);

        var subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11;");

        card.getChildren().addAll(titleLabel, valueLabel, subtitleLabel);
        return card;
    }

    private HBox buildDetailsAndAttendance(Employee emp) {
        var container = new HBox(14);
        container.setStyle("-fx-background-color: transparent;");

        // Details Panel
        var detailsCard = buildDetailsCard(emp);
        HBox.setHgrow(detailsCard, Priority.ALWAYS);
        container.getChildren().add(detailsCard);

        // Actions Panel (right side)
        var actionsPanel = buildEmployeeActionsPanel(emp);
        actionsPanel.setPrefWidth(300);
        container.getChildren().add(actionsPanel);

        return container;
    }

    private VBox buildDetailsCard(Employee emp) {
        var card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 16; -fx-border-color: #1e293b; -fx-border-width: 1; -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 24, 0.15, 0, 8);");
        card.setMaxWidth(Double.MAX_VALUE);

        var title = new Label("Personal Information");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 16; -fx-font-weight: 700;");

        var detailsGrid = new GridPane();
        detailsGrid.setHgap(24);
        detailsGrid.setVgap(14);
        detailsGrid.setPrefWidth(Double.MAX_VALUE);

        addDetailRow(detailsGrid, 0, "Full Name", safe(emp.getFirstName()) + " " + safe(emp.getLastName()));
        addDetailRow(detailsGrid, 1, "Date of Birth", safe(emp.getBirthdate()));
        addDetailRow(detailsGrid, 2, "Address", safe(emp.getAddress()));
        addDetailRow(detailsGrid, 3, "Phone Number", safe(emp.getPhoneNumber()));
        addDetailRow(detailsGrid, 4, "SSS #", safe(emp.getSssNo()));
        addDetailRow(detailsGrid, 5, "PhilHealth #", safe(emp.getPhilhealthNo()));

        card.getChildren().addAll(title, detailsGrid);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        var labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12; -fx-font-weight: 600;");

        var valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13; -fx-font-weight: 500;");
        valueNode.setWrapText(true);

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
        GridPane.setHgrow(valueNode, Priority.ALWAYS);
    }

    private VBox buildEmployeeActionsPanel(Employee emp) {
        var panel = new VBox(12);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 16; -fx-border-color: #1e293b; -fx-border-width: 1; -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 24, 0.15, 0, 8);");
        panel.setMaxHeight(Double.MAX_VALUE);

        var title = new Label("Quick Actions");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 16; -fx-font-weight: 700; -fx-padding: 0 0 8 0;");

        var viewPayroll = new Button("View Payroll");
        styleActionButton(viewPayroll, "#3b82f6");
        viewPayroll.setMaxWidth(Double.MAX_VALUE);
        viewPayroll.setOnAction(e -> showEmployeePayroll(emp));

        var viewAttendance = new Button("View Attendance");
        styleActionButton(viewAttendance, "#8b5cf6");
        viewAttendance.setMaxWidth(Double.MAX_VALUE);
        viewAttendance.setOnAction(e -> showEmployeeAttendance(emp));

        var spacer1 = spacer(4);
        
        var logIn = new Button("Clock In");
        styleActionButton(logIn, "#10b981");
        logIn.setMaxWidth(Double.MAX_VALUE);
        logIn.setOnAction(e -> logInEmployee(emp));

        var logOut = new Button("Clock Out");
        styleActionButton(logOut, "#f59e0b");
        logOut.setMaxWidth(Double.MAX_VALUE);
        logOut.setOnAction(e -> logOutEmployee(emp));

        var spacer2 = spacer(8);
        
        var refreshBtn = new Button("Refresh Dashboard");
        styleSecondaryActionButton(refreshBtn);
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setOnAction(e -> {
            try {
                loadEmployeesFromCsv();
                primaryStage.setScene(buildAppScene());
            } catch (Exception ex) {
                showError("Refresh failed", ex.getMessage());
            }
        });

        panel.getChildren().addAll(title, viewPayroll, viewAttendance, spacer1, logIn, logOut, spacer2, refreshBtn);
        VBox.setVgrow(refreshBtn, Priority.ALWAYS);
        return panel;
    }

    private VBox buildModernWelcomeSection(Employee emp) {
        var card = new VBox(18);
        card.setPadding(new Insets(26));
        card.getStyleClass().add("employee-hero");

        var topRow = new HBox(14);
        topRow.setAlignment(Pos.CENTER_LEFT);

        var intro = new VBox(6);
        var eyebrow = new Label("Employee workspace");
        eyebrow.getStyleClass().add("employee-hero-eyebrow");

        var greeting = new Label("Welcome back, " + safe(emp.getFirstName()));
        greeting.getStyleClass().add("employee-hero-title");

        var subtext = new Label(safe(emp.getPosition()) + " - " + safe(emp.getDepartment()));
        subtext.getStyleClass().add("employee-hero-subtitle");
        intro.getChildren().addAll(eyebrow, greeting, subtext);

        var idBadge = new Label("ID " + safe(emp.getId()));
        idBadge.getStyleClass().add("employee-id-badge");

        topRow.getChildren().addAll(intro, spacer(1), idBadge);
        HBox.setHgrow(topRow.getChildren().get(1), Priority.ALWAYS);

        var chipRow = new HBox(10);
        chipRow.getChildren().addAll(
                buildModernHeroChip("Status", "Active employee"),
                buildModernHeroChip("Phone", safe(emp.getPhoneNumber())),
                buildModernHeroChip("SSS", safe(emp.getSssNo()))
        );

        card.getChildren().addAll(topRow, chipRow);
        return card;
    }

    private VBox buildModernHeroChip(String label, String value) {
        var chip = new VBox(4);
        chip.setPrefWidth(180);
        chip.getStyleClass().add("employee-hero-chip");

        var labelNode = new Label(label);
        labelNode.getStyleClass().add("employee-hero-chip-label");

        var valueNode = new Label(value);
        valueNode.getStyleClass().add("employee-hero-chip-value");
        valueNode.setWrapText(true);

        chip.getChildren().addAll(labelNode, valueNode);
        return chip;
    }

    private HBox buildModernStatisticsCards(Employee emp) {
        var container = new HBox(14);
        container.setPadding(new Insets(0, 0, 6, 0));

        try {
            Employee.loadContributionRates();
        } catch (Exception ignored) {
        }

        var salaryCard = buildModernStatCard(
                getCompensationCardTitle(emp),
                formatPhp(getPrimaryCompensationValue(emp)),
                getCompensationCardSubtitle(emp),
                "#1d4ed8",
                "dashboard-stat-card stat-card-blue"
        );
        HBox.setHgrow(salaryCard, Priority.ALWAYS);

        var positionCard = buildModernStatCard(
                "Position",
                safe(emp.getPosition()),
                safe(emp.getDepartment()),
                "#0f766e",
                "dashboard-stat-card stat-card-teal"
        );
        HBox.setHgrow(positionCard, Priority.ALWAYS);

        var grossCard = buildModernStatCard(
                "Gross Pay",
                formatPhp(getDashboardGrossPayValue(emp)),
                getGrossPayCardSubtitle(emp),
                "#b45309",
                "dashboard-stat-card stat-card-amber"
        );
        HBox.setHgrow(grossCard, Priority.ALWAYS);

        double totalBenefits = emp.getRiceSubsidy() + emp.getPhoneAllowance() + emp.getClothingAllowance();
        var benefitsCard = buildModernStatCard(
                "Total Benefits",
                String.format("PHP %.2f", totalBenefits),
                "Rice, phone, clothing",
                "#c2410c",
                "dashboard-stat-card stat-card-orange"
        );
        HBox.setHgrow(benefitsCard, Priority.ALWAYS);

        container.getChildren().addAll(salaryCard, positionCard, grossCard, benefitsCard);
        return container;
    }

    private VBox buildModernStatCard(String title, String value, String subtitle, String colorHex, String cssClasses) {
        var card = new VBox(12);
        card.setPadding(new Insets(18));
        card.getStyleClass().addAll(cssClasses.split(" "));
        card.setMaxWidth(Double.MAX_VALUE);

        var accent = new Region();
        accent.setPrefHeight(4);
        accent.setMinHeight(4);
        accent.setMaxWidth(Double.MAX_VALUE);
        accent.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 999;");

        var titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-stat-title");

        var valueLabel = new Label(value);
        valueLabel.getStyleClass().add("dashboard-stat-value");
        valueLabel.setWrapText(true);

        var subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("dashboard-stat-subtitle");

        card.getChildren().addAll(accent, titleLabel, valueLabel, subtitleLabel);
        return card;
    }

    private HBox buildModernDetailsAndAttendance(Employee emp) {
        var container = new HBox(14);

        var detailsCard = buildModernDetailsCard(emp);
        HBox.setHgrow(detailsCard, Priority.ALWAYS);

        var actionsPanel = buildModernEmployeeActionsPanel(emp);
        actionsPanel.setPrefWidth(300);

        container.getChildren().addAll(detailsCard, actionsPanel);
        return container;
    }

    private VBox buildModernDetailsCard(Employee emp) {
        var card = new VBox(18);
        card.setPadding(new Insets(22));
        card.getStyleClass().add("employee-surface-card");
        card.setMaxWidth(Double.MAX_VALUE);

        var header = new VBox(4);
        var title = new Label("Employee details");
        title.getStyleClass().add("employee-section-title");

        var subtitle = new Label("Personal information and government IDs");
        subtitle.getStyleClass().add("employee-section-subtitle");
        header.getChildren().addAll(title, subtitle);

        var detailsGrid = new GridPane();
        detailsGrid.setHgap(18);
        detailsGrid.setVgap(12);
        detailsGrid.setPrefWidth(Double.MAX_VALUE);

        var leftCol = new ColumnConstraints();
        leftCol.setMinWidth(150);
        leftCol.setPrefWidth(170);
        var rightCol = new ColumnConstraints();
        rightCol.setHgrow(Priority.ALWAYS);
        detailsGrid.getColumnConstraints().addAll(leftCol, rightCol);

        addModernDetailRow(detailsGrid, 0, "Full Name", safe(emp.getFirstName()) + " " + safe(emp.getLastName()));
        addModernDetailRow(detailsGrid, 1, "Date of Birth", safe(emp.getBirthdate()));
        addModernDetailRow(detailsGrid, 2, "Address", safe(emp.getAddress()));
        addModernDetailRow(detailsGrid, 3, "Phone Number", safe(emp.getPhoneNumber()));
        addModernDetailRow(detailsGrid, 4, "SSS #", safe(emp.getSssNo()));
        addModernDetailRow(detailsGrid, 5, "PhilHealth #", safe(emp.getPhilhealthNo()));

        card.getChildren().addAll(header, detailsGrid);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private void addModernDetailRow(GridPane grid, int row, String label, String value) {
        var labelNode = new Label(label);
        labelNode.getStyleClass().add("employee-detail-label");

        var valueNode = new Label(value);
        valueNode.getStyleClass().add("employee-detail-value");
        valueNode.setWrapText(true);
        valueNode.setMaxWidth(Double.MAX_VALUE);
        valueNode.setPadding(new Insets(10, 12, 10, 12));

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
        GridPane.setHgrow(valueNode, Priority.ALWAYS);
    }

    private VBox buildModernEmployeeActionsPanel(Employee emp) {
        var panel = new VBox(12);
        panel.setPadding(new Insets(22));
        panel.getStyleClass().add("employee-actions-card");
        panel.setMaxHeight(Double.MAX_VALUE);

        var title = new Label("Quick actions");
        title.getStyleClass().add("employee-section-title");

        var hint = new Label("Open payroll details, review attendance, or update today's time logs.");
        hint.getStyleClass().add("employee-section-subtitle");

        var viewPayroll = new Button("View Payroll");
        styleActionButton(viewPayroll, "#2563eb");
        viewPayroll.getStyleClass().add("employee-action-button");
        viewPayroll.setMaxWidth(Double.MAX_VALUE);
        viewPayroll.setOnAction(e -> showEmployeePayroll(emp));

        var viewAttendance = new Button("View Attendance");
        styleActionButton(viewAttendance, "#0f766e");
        viewAttendance.getStyleClass().add("employee-action-button");
        viewAttendance.setMaxWidth(Double.MAX_VALUE);
        viewAttendance.setOnAction(e -> showEmployeeAttendance(emp));

        var logIn = new Button("Clock In");
        styleActionButton(logIn, "#16a34a");
        logIn.getStyleClass().add("employee-action-button");
        logIn.setMaxWidth(Double.MAX_VALUE);
        logIn.setOnAction(e -> logInEmployee(emp));

        var logOut = new Button("Clock Out");
        styleActionButton(logOut, "#ea580c");
        logOut.getStyleClass().add("employee-action-button");
        logOut.setMaxWidth(Double.MAX_VALUE);
        logOut.setOnAction(e -> logOutEmployee(emp));

        var refreshBtn = new Button("Refresh Dashboard");
        styleSecondaryActionButton(refreshBtn);
        refreshBtn.getStyleClass().addAll("employee-action-button", "employee-action-button-secondary");
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setOnAction(e -> {
            try {
                loadEmployeesFromCsv();
                primaryStage.setScene(buildAppScene());
            } catch (Exception ex) {
                showError("Refresh failed", ex.getMessage());
            }
        });

        panel.getChildren().addAll(title, hint, viewPayroll, viewAttendance, logIn, logOut, spacer(6), refreshBtn);
        VBox.setVgrow(refreshBtn, Priority.ALWAYS);
        return panel;
    }

    private VBox buildRefactoredEmployeeDashboard(Employee emp) {
        var dashboard = new VBox(18);
        dashboard.setPadding(new Insets(4));
        dashboard.getStyleClass().add("employee-dashboard");

        var content = new VBox(18);
        content.getChildren().addAll(buildModernWelcomeSection(emp), buildModernStatisticsCards(emp));

        var formState = new DashboardFormState();
        var infoPane = createDashboardPane(
                "Employee Information",
                "Grouped personal details and government IDs for payroll records.",
                buildEmployeeInformationSection(emp, formState));
        var salaryPane = createDashboardPane(
                "Salary Details",
                "Select a payroll month and year to refresh hours worked, deductions, and take-home pay.",
                buildSalaryDetailsSection(emp, formState));
        var benefitsPane = createDashboardPane(
                "Benefits Summary",
                "Review benefits, contributions, and tax references used in payroll.",
                buildBenefitsSummarySection(emp));
        var actionsPane = createDashboardPane(
                "Actions",
                "Use the main employee actions with clearer feedback and confirmation steps.",
                buildRefactoredActionsSection(emp, formState));

        var accordion = new Accordion(infoPane, salaryPane, benefitsPane, actionsPane);
        accordion.getStyleClass().add("dashboard-accordion");
        accordion.setExpandedPane(infoPane);

        var scrollPane = new ScrollPane(accordion);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("dashboard-scroll");

        var navBar = new HBox(10);
        navBar.getStyleClass().add("dashboard-nav");
        navBar.getChildren().addAll(
                createSectionNavButton("Employee Information", scrollPane, infoPane),
                createSectionNavButton("Salary Details", scrollPane, salaryPane),
                createSectionNavButton("Benefits Summary", scrollPane, benefitsPane),
                createSectionNavButton("Actions", scrollPane, actionsPane)
        );

        content.getChildren().addAll(navBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        dashboard.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);

        playIntroAnimation(infoPane, salaryPane, benefitsPane, actionsPane);
        return dashboard;
    }

    private Button createSectionNavButton(String text, ScrollPane scrollPane, Region target) {
        var button = new Button(text);
        button.getStyleClass().add("dashboard-nav-button");
        button.setAccessibleText(text);
        button.setOnAction(e -> {
            if (target instanceof TitledPane pane) {
                pane.setExpanded(true);
            }
            scrollToSection(scrollPane, target);
        });
        return button;
    }

    private VBox buildEmployeeInformationSection(Employee emp, DashboardFormState state) {
        var section = new VBox(18);

        state.firstNameField = createDashboardField("First Name", safe(emp.getFirstName()), false, "Employee first name used in payroll records.");
        state.lastNameField = createDashboardField("Last Name", safe(emp.getLastName()), false, "Employee last name used in payroll records.");
        state.birthdateField = createDashboardField("Birthdate", safe(emp.getBirthdate()), true, "Birthdate in YYYY-MM-DD format.");
        state.phoneField = createDashboardField("Phone Number", safe(emp.getPhoneNumber()), true, "Primary contact number for employee records.");
        state.addressArea = createDashboardArea("Address", safe(emp.getAddress()), true, "Current employee address.");
        state.sssField = createDashboardField("SSS Number", safe(emp.getSssNo()), true, "SSS number used to compute Social Security deductions.");
        state.philhealthField = createDashboardField("PhilHealth Number", safe(emp.getPhilhealthNo()), true, "PhilHealth number used to compute health insurance deductions.");
        state.tinField = createDashboardField("TIN", safe(emp.getTinNo()), true, "Tax identification number used for withholding tax.");
        state.pagibigField = createDashboardField("Pag-IBIG Number", safe(emp.getPagibigNo()), true, "Pag-IBIG number used for housing fund deductions.");

        var groupedFields = new FlowPane();
        groupedFields.setHgap(16);
        groupedFields.setVgap(16);
        groupedFields.setPrefWrapLength(880);

        var personalGroup = buildFieldGroupCard(
                "Name, Address, and Contact Information",
                "These fields will be used for employee records on payroll and employee communications.");
        personalGroup.getChildren().addAll(
                buildFieldCard("ID", "First Name", state.firstNameField, "This field will be used for employee records on payroll.", null),
                buildFieldCard("ID", "Last Name", state.lastNameField, "This field will be used for employee records on payroll.", null),
                buildFieldCard("CAL", "Birthdate", state.birthdateField, "Use YYYY-MM-DD so payroll reports stay consistent.", "Birthdate is required when validating employee records."),
                buildFieldCard("TEL", "Phone Number", state.phoneField, "Use a valid mobile or landline format for payroll contacts.", "Used for employee contact details and record verification."),
                buildFieldCard("ADR", "Address", state.addressArea, "Enter the full home address shown on employee records.", "Stored with the employee profile for payroll and HR reference.")
        );

        var governmentGroup = buildFieldGroupCard(
                "Government IDs and Payroll Deductions",
                "These identifiers support payroll deductions and statutory contribution tracking.");
        governmentGroup.getChildren().addAll(
                buildFieldCard("SSS", "SSS Number", state.sssField, "Used in Social Security deduction computations for payroll.", "SSS contributions are part of the employee's required payroll deductions."),
                buildFieldCard("PH", "PhilHealth Number", state.philhealthField, "Used in PhilHealth contribution computations for payroll.", "PhilHealth contributions affect the employee's health insurance deduction."),
                buildFieldCard("TAX", "TIN", state.tinField, "Used in withholding tax calculations and tax reporting.", "The Tax Identification Number is referenced for payroll tax deductions."),
                buildFieldCard("HDMF", "Pag-IBIG Number", state.pagibigField, "Used in Pag-IBIG contribution computations for payroll.", "Pag-IBIG values are used for housing fund deductions in payroll.")
        );

        groupedFields.getChildren().addAll(personalGroup, governmentGroup);

        state.feedbackLabel = new Label("Fields are locked by default. Select Edit to make changes, then save to update your profile.");
        state.feedbackLabel.getStyleClass().addAll("dashboard-feedback", "dashboard-feedback-info");
        state.feedbackLabel.setWrapText(true);

        section.getChildren().addAll(groupedFields, state.feedbackLabel);
        return section;
    }

    private VBox buildSalaryDetailsSection(Employee emp, DashboardFormState state) {
        var section = new VBox(16);

        List<String[]> attendanceRows;
        try {
            attendanceRows = loadAttendanceData().stream()
                    .filter(row -> safeRowValue(row, 0).equals(emp.getId()))
                    .toList();
        } catch (IOException | CsvValidationException ex) {
            attendanceRows = List.of();
        }

        state.payrollMonthBox = new ComboBox<>();
        state.payrollMonthBox.getItems().addAll(List.of(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"));
        state.payrollMonthBox.getStyleClass().add("dashboard-input");
        state.payrollMonthBox.setMaxWidth(Double.MAX_VALUE);
        state.payrollMonthBox.setTooltip(new Tooltip("Choose the payroll month to refresh computed pay details."));

        state.payrollYearBox = new ComboBox<>();
        state.payrollYearBox.getStyleClass().add("dashboard-input");
        state.payrollYearBox.setMaxWidth(Double.MAX_VALUE);
        state.payrollYearBox.setTooltip(new Tooltip("Choose the payroll year to refresh computed pay details."));

        state.payrollDatePicker = new DatePicker();
        state.payrollDatePicker.getStyleClass().add("dashboard-input");
        state.payrollDatePicker.setMaxWidth(Double.MAX_VALUE);
        state.payrollDatePicker.setTooltip(new Tooltip("Calendar selector for the payroll period. The chosen date updates month and year."));

        LocalDate latestAttendanceDate = findLatestAttendanceDate(attendanceRows).orElse(LocalDate.now());
        state.payrollYearBox.getItems().setAll(collectAttendanceYears(attendanceRows));
        if (state.payrollYearBox.getItems().isEmpty()) {
            state.payrollYearBox.getItems().add(latestAttendanceDate.getYear());
        }
        state.payrollMonthBox.setValue(formatMonth(latestAttendanceDate));
        state.payrollYearBox.setValue(latestAttendanceDate.getYear());
        state.payrollDatePicker.setValue(latestAttendanceDate);

        var periodSelectors = new FlowPane();
        periodSelectors.setHgap(12);
        periodSelectors.setVgap(12);
        periodSelectors.setPrefWrapLength(880);
        periodSelectors.getChildren().addAll(
                createLabeledNode("Payroll Month", state.payrollMonthBox),
                createLabeledNode("Payroll Year", state.payrollYearBox),
                createLabeledNode("Payroll Calendar", state.payrollDatePicker)
        );

        state.payrollPeriodSummary = new Label();
        state.payrollPeriodSummary.getStyleClass().addAll("dashboard-feedback", "dashboard-feedback-info");
        state.payrollPeriodSummary.setWrapText(true);

        state.totalHoursLabel = new Label();
        state.compensationBasisTitleLabel = new Label();
        state.compensationBasisSubtitleLabel = new Label();
        state.hourlyRateLabel = new Label();
        state.grossPayLabel = new Label();
        state.totalDeductionsLabel = new Label();
        state.netPayLabel = new Label();
        state.completedAttendanceLabel = new Label();

        var metrics = new FlowPane();
        metrics.setHgap(14);
        metrics.setVgap(14);
        metrics.setPrefWrapLength(880);
        metrics.getChildren().addAll(
                buildDynamicMetricTile("Total Hours Worked", state.totalHoursLabel, "Calculated from completed attendance records for the selected period."),
                buildDynamicMetricTile(state.compensationBasisTitleLabel, state.hourlyRateLabel, state.compensationBasisSubtitleLabel),
                buildDynamicMetricTile("Gross Pay", state.grossPayLabel, "Base pay plus employee allowances for the selected period."),
                buildDynamicMetricTile("Total Deductions", state.totalDeductionsLabel, "SSS, PhilHealth, Pag-IBIG, and tax deductions."),
                buildDynamicMetricTile("Net Pay", state.netPayLabel, "Estimated take-home pay for the selected period."),
                buildDynamicMetricTile("Attendance Entries", state.completedAttendanceLabel, "Completed clock in and clock out records counted in the calculation.")
        );

        var breakdown = new VBox(10);
        breakdown.getStyleClass().add("dashboard-subsection");
        breakdown.getChildren().addAll(
                createInfoRow("Basic Salary", String.format("PHP %.2f", emp.getBasicSalary())),
                createInfoRow("Rice Subsidy", String.format("PHP %.2f", emp.getRiceSubsidy())),
                createInfoRow("Phone Allowance", String.format("PHP %.2f", emp.getPhoneAllowance())),
                createInfoRow("Clothing Allowance", String.format("PHP %.2f", emp.getClothingAllowance())),
                createInfoRow("Government Deductions", "SSS, PhilHealth, Pag-IBIG, and withholding tax are recalculated per selected period.")
        );

        final List<String[]> employeeAttendanceRows = attendanceRows;
        Runnable refreshPayrollSummary = () -> refreshPayrollSummary(emp, employeeAttendanceRows, state);
        state.payrollMonthBox.setOnAction(e -> {
            syncDatePickerWithMonthYear(state.payrollMonthBox, state.payrollYearBox, state.payrollDatePicker);
            refreshPayrollSummary.run();
        });
        state.payrollYearBox.setOnAction(e -> {
            syncDatePickerWithMonthYear(state.payrollMonthBox, state.payrollYearBox, state.payrollDatePicker);
            refreshPayrollSummary.run();
        });
        state.payrollDatePicker.setOnAction(e -> {
            LocalDate selectedDate = state.payrollDatePicker.getValue();
            if (selectedDate != null) {
                state.payrollMonthBox.setValue(formatMonth(selectedDate));
                if (!state.payrollYearBox.getItems().contains(selectedDate.getYear())) {
                    state.payrollYearBox.getItems().add(selectedDate.getYear());
                    FXCollections.sort(state.payrollYearBox.getItems());
                }
                state.payrollYearBox.setValue(selectedDate.getYear());
            }
            refreshPayrollSummary.run();
        });
        refreshPayrollSummary.run();

        section.getChildren().addAll(periodSelectors, state.payrollPeriodSummary, metrics, breakdown);
        return section;
    }

    private VBox buildBenefitsSummarySection(Employee emp) {
        var section = new VBox(16);

        var benefitsGrid = new FlowPane();
        benefitsGrid.setHgap(14);
        benefitsGrid.setVgap(14);
        benefitsGrid.setPrefWrapLength(880);
        benefitsGrid.getChildren().addAll(
                buildMetricTile("Rice Subsidy", String.format("PHP %.2f", emp.getRiceSubsidy()), "Monthly food support."),
                buildMetricTile("Phone Allowance", String.format("PHP %.2f", emp.getPhoneAllowance()), "Communication allowance included in gross pay."),
                buildMetricTile("Clothing Allowance", String.format("PHP %.2f", emp.getClothingAllowance()), "Workwear support included in gross pay."),
                buildMetricTile("SSS Deduction", String.format("PHP %.2f", emp.getSssDeduction()), "Social Security deduction used in payroll."),
                buildMetricTile("PhilHealth Deduction", String.format("PHP %.2f", emp.getPhilhealthDeduction()), "Health insurance deduction used in payroll."),
                buildMetricTile("Pag-IBIG Deduction", String.format("PHP %.2f", emp.getPagibigDeduction()), "Housing fund deduction used in payroll."),
                buildMetricTile("Tax Deduction", String.format("PHP %.2f", emp.getTaxDeduction()), "Withholding tax applied to payroll."),
                buildMetricTile("Employee Status", safe(emp.getStatus()), safe(emp.getSupervisor()).isEmpty() ? "No supervisor assigned." : "Reports to " + safe(emp.getSupervisor()))
        );

        var notes = new Label("Tooltips on government ID fields explain how each contribution is used in payroll computations.");
        notes.getStyleClass().add("dashboard-helper-text");
        notes.setWrapText(true);

        section.getChildren().addAll(benefitsGrid, notes);
        return section;
    }

    private VBox buildRefactoredActionsSection(Employee emp, DashboardFormState state) {
        var section = new VBox(16);

        var buttonRow = new FlowPane();
        buttonRow.setHgap(12);
        buttonRow.setVgap(12);
        buttonRow.setPrefWrapLength(880);

        var editButton = createDashboardActionButton("EDIT  Edit Profile", "dashboard-button-primary", "Enable profile editing for the fields in Employee Information.");
        editButton.setOnAction(e -> setEditMode(state, true, "Edit mode enabled. Update the required fields, then save your changes."));

        var saveButton = createDashboardActionButton("SAVE  Save Changes", "dashboard-button-success", "Validate entries and save profile updates.");
        saveButton.setOnAction(e -> saveEmployeeProfile(emp, state));

        var exportButton = createDashboardActionButton("EXP  Export Summary", "dashboard-button-secondary", "Export a text summary of the employee payroll dashboard.");
        exportButton.setOnAction(e -> exportEmployeeSummary(emp));

        var payrollButton = createDashboardActionButton("PAY  View Payroll", "dashboard-button-neutral", "Open the payroll summary dialog.");
        payrollButton.setOnAction(e -> showEmployeePayroll(emp));

        var attendanceButton = createDashboardActionButton("TIME  View Attendance", "dashboard-button-neutral", "Open the employee attendance history.");
        attendanceButton.setOnAction(e -> showEmployeeAttendance(emp));

        var clockInButton = createDashboardActionButton("IN  Clock In", "dashboard-button-success", "Record a clock in entry for today.");
        clockInButton.setOnAction(e -> logInEmployee(emp));

        var clockOutButton = createDashboardActionButton("OUT  Clock Out", "dashboard-button-warning", "Record a clock out entry for today.");
        clockOutButton.setOnAction(e -> logOutEmployee(emp));

        buttonRow.getChildren().addAll(editButton, saveButton, exportButton, payrollButton, attendanceButton, clockInButton, clockOutButton);

        var help = new Label("Validation checks birthdate, address, phone number, and payroll IDs. Save shows a confirmation dialog before anything is written.");
        help.getStyleClass().add("dashboard-helper-text");
        help.setWrapText(true);

        section.getChildren().addAll(buttonRow, help);
        return section;
    }

    private TitledPane createDashboardPane(String title, String subtitle, Node content) {
        var wrapper = new VBox(14);
        wrapper.getStyleClass().add("dashboard-pane-content");

        var subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("employee-section-subtitle");
        subtitleLabel.setWrapText(true);

        wrapper.getChildren().addAll(subtitleLabel, content);

        var pane = new TitledPane(title, wrapper);
        pane.getStyleClass().add("dashboard-pane");
        pane.setExpanded(false);
        return pane;
    }

    private TextField createDashboardField(String prompt, String value, boolean editable, String accessibleText) {
        var field = new TextField(value);
        field.setPromptText(prompt);
        field.setEditable(editable);
        field.setDisable(!editable);
        field.getStyleClass().add("dashboard-input");
        field.setAccessibleText(accessibleText);
        return field;
    }

    private TextArea createDashboardArea(String prompt, String value, boolean editable, String accessibleText) {
        var area = new TextArea(value);
        area.setPromptText(prompt);
        area.setWrapText(true);
        area.setPrefRowCount(3);
        area.setEditable(editable);
        area.setDisable(!editable);
        area.getStyleClass().add("dashboard-input");
        area.setAccessibleText(accessibleText);
        return area;
    }

    private VBox buildFieldGroupCard(String title, String subtitle) {
        var group = new VBox(14);
        group.getStyleClass().add("dashboard-group-card");
        group.setPrefWidth(420);

        var titleNode = new Label(title);
        titleNode.getStyleClass().add("employee-section-title");

        var subtitleNode = new Label(subtitle);
        subtitleNode.getStyleClass().add("employee-section-subtitle");
        subtitleNode.setWrapText(true);

        group.getChildren().addAll(titleNode, subtitleNode);
        return group;
    }

    private VBox buildFieldCard(String iconText, String label, Control control, String hint, String tooltipText) {
        var card = new VBox(8);
        card.getStyleClass().add("dashboard-field-card");

        var labelNode = new Label(iconText + "  " + label);
        labelNode.getStyleClass().add("dashboard-field-label");

        var hintNode = new Label(hint);
        hintNode.getStyleClass().add("dashboard-helper-text");
        hintNode.setWrapText(true);

        if (tooltipText != null && !tooltipText.isBlank()) {
            var tooltip = new Tooltip(tooltipText);
            labelNode.setTooltip(tooltip);
            control.setTooltip(new Tooltip(tooltipText));
        }

        card.getChildren().addAll(labelNode, control, hintNode);
        return card;
    }

    private HBox createInfoRow(String label, String value) {
        var row = new HBox();
        row.getStyleClass().add("dashboard-info-row");
        row.setAlignment(Pos.CENTER_LEFT);

        var labelNode = new Label(label);
        labelNode.getStyleClass().add("dashboard-info-label");

        var valueNode = new Label(value);
        valueNode.getStyleClass().add("dashboard-info-value");

        row.getChildren().addAll(labelNode, spacer(1), valueNode);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        return row;
    }

    private VBox buildMetricTile(String title, String value, String subtitle) {
        var tile = new VBox(6);
        tile.getStyleClass().add("dashboard-metric-tile");

        var titleNode = new Label(title);
        titleNode.getStyleClass().add("dashboard-stat-title");

        var valueNode = new Label(value);
        valueNode.getStyleClass().add("dashboard-stat-value");

        var subtitleNode = new Label(subtitle);
        subtitleNode.getStyleClass().add("dashboard-stat-subtitle");
        subtitleNode.setWrapText(true);

        tile.getChildren().addAll(titleNode, valueNode, subtitleNode);
        return tile;
    }

    private VBox buildDynamicMetricTile(String title, Label valueNode, String subtitle) {
        var tile = new VBox(6);
        tile.getStyleClass().add("dashboard-metric-tile");

        var titleNode = new Label(title);
        titleNode.getStyleClass().add("dashboard-stat-title");

        valueNode.getStyleClass().add("dashboard-stat-value");
        valueNode.setWrapText(true);

        var subtitleNode = new Label(subtitle);
        subtitleNode.getStyleClass().add("dashboard-stat-subtitle");
        subtitleNode.setWrapText(true);

        tile.getChildren().addAll(titleNode, valueNode, subtitleNode);
        return tile;
    }

    private VBox buildDynamicMetricTile(Label titleNode, Label valueNode, Label subtitleNode) {
        var tile = new VBox(6);
        tile.getStyleClass().add("dashboard-metric-tile");

        titleNode.getStyleClass().add("dashboard-stat-title");
        valueNode.getStyleClass().add("dashboard-stat-value");
        valueNode.setWrapText(true);
        subtitleNode.getStyleClass().add("dashboard-stat-subtitle");
        subtitleNode.setWrapText(true);

        tile.getChildren().addAll(titleNode, valueNode, subtitleNode);
        return tile;
    }

    private VBox createLabeledNode(String label, Node node) {
        var wrapper = new VBox(6);
        wrapper.getStyleClass().add("dashboard-control-block");
        wrapper.setPrefWidth(220);

        var labelNode = new Label(label);
        labelNode.getStyleClass().add("dashboard-info-label");

        wrapper.getChildren().addAll(labelNode, node);
        return wrapper;
    }

    private Button createDashboardActionButton(String text, String styleClass, String tooltipText) {
        var button = new Button(text);
        button.getStyleClass().addAll("dashboard-action-button", styleClass);
        button.setAccessibleText(text);
        button.setTooltip(new Tooltip(tooltipText));
        return button;
    }

    private void setEditMode(DashboardFormState state, boolean editable, String message) {
        for (Control control : state.editableControls()) {
            control.setDisable(!editable);
            if (control instanceof TextInputControl input) {
                input.setEditable(editable);
            }
        }
        state.feedbackLabel.setText(message);
        state.feedbackLabel.getStyleClass().setAll("dashboard-feedback", editable ? "dashboard-feedback-warning" : "dashboard-feedback-success");
    }

    private void saveEmployeeProfile(Employee emp, DashboardFormState state) {
        if (!showConfirmation(
                "Confirm Save",
                "Save employee profile changes?",
                "This will update the employee record used by payroll. Continue?")) {
            state.feedbackLabel.setText("Save cancelled. No changes were written.");
            state.feedbackLabel.getStyleClass().setAll("dashboard-feedback", "dashboard-feedback-info");
            return;
        }

        clearValidationStyles(state);
        List<String> errors = validateEmployeeProfile(state);
        if (!errors.isEmpty()) {
            state.feedbackLabel.setText(String.join("\n", errors));
            state.feedbackLabel.getStyleClass().setAll("dashboard-feedback", "dashboard-feedback-error");
            return;
        }

        emp.setBirthdate(state.birthdateField.getText().trim());
        emp.setPhoneNumber(state.phoneField.getText().trim());
        emp.setAddress(state.addressArea.getText().trim());
        emp.setSssNo(state.sssField.getText().trim());
        emp.setPhilhealthNo(state.philhealthField.getText().trim());
        emp.setTinNo(state.tinField.getText().trim());
        emp.setPagibigNo(state.pagibigField.getText().trim());

        if (!saveEmployeesToCsv()) {
            state.feedbackLabel.setText("Unable to save changes. Review the error dialog and try again.");
            state.feedbackLabel.getStyleClass().setAll("dashboard-feedback", "dashboard-feedback-error");
            return;
        }

        setEditMode(state, false, "Changes saved successfully. Your employee record is now up to date.");
        showInfo("Changes Saved", "Changes saved successfully.");
    }

    private boolean saveEmployeesToCsv() {
        return writeEmployeesToCsv();
    }

    private List<String> validateEmployeeProfile(DashboardFormState state) {
        List<String> errors = new ArrayList<>();

        // Validate the most important editable fields before persisting anything to CSV.
        validateRequired(state.birthdateField, "Birthdate is required.", errors);
        validateRequired(state.phoneField, "Phone number is required.", errors);
        validateRequired(state.addressArea, "Address is required.", errors);
        validateRequired(state.sssField, "SSS number is required.", errors);
        validateRequired(state.philhealthField, "PhilHealth number is required.", errors);
        validateRequired(state.tinField, "TIN is required.", errors);
        validateRequired(state.pagibigField, "Pag-IBIG number is required.", errors);

        String birthdate = state.birthdateField.getText().trim();
        if (!birthdate.isEmpty() && !birthdate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            markInvalid(state.birthdateField);
            errors.add("Invalid birthdate format. Use YYYY-MM-DD.");
        }

        String phone = state.phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("[0-9+\\-() ]{7,20}")) {
            markInvalid(state.phoneField);
            errors.add("Invalid phone number format. Use digits and standard phone symbols only.");
        }

        return errors;
    }

    private void validateRequired(Control control, String message, List<String> errors) {
        String value = control instanceof TextInputControl input ? input.getText().trim() : "";
        if (value.isEmpty()) {
            markInvalid(control);
            errors.add(message);
        }
    }

    private void clearValidationStyles(DashboardFormState state) {
        for (Control control : state.editableControls()) {
            control.getStyleClass().remove("field-invalid");
        }
    }

    private void markInvalid(Control control) {
        if (!control.getStyleClass().contains("field-invalid")) {
            control.getStyleClass().add("field-invalid");
        }
    }

    private void refreshPayrollSummary(Employee emp, List<String[]> attendanceRows, DashboardFormState state) {
        Integer year = state.payrollYearBox == null ? null : state.payrollYearBox.getValue();
        int month = state.payrollMonthBox == null ? -1 : parseMonthIndex(state.payrollMonthBox.getValue());
        if (year == null || month < 1) {
            state.payrollPeriodSummary.setText("Select a valid payroll month and year to refresh the salary details.");
            state.payrollPeriodSummary.getStyleClass().setAll("dashboard-feedback", "dashboard-feedback-warning");
            state.totalHoursLabel.setText("0.00");
            state.compensationBasisTitleLabel.setText(getCompensationMetricTitle(emp));
            state.compensationBasisSubtitleLabel.setText(getCompensationMetricSubtitle(emp));
            state.hourlyRateLabel.setText(formatPhp(getDisplayedCompensationBasis(emp)));
            state.grossPayLabel.setText("PHP 0.00");
            state.totalDeductionsLabel.setText("PHP 0.00");
            state.netPayLabel.setText("PHP 0.00");
            state.completedAttendanceLabel.setText("0");
            return;
        }

        double totalHoursWorked = calculateMonthlyHoursForEmployee(attendanceRows, emp.getId(), month, year);
        long completedAttendanceRecords = attendanceRows.stream()
                .filter(row -> matchesMonthYear(row, month, year))
                .filter(row -> calculateWorkedHours(row) > 0)
                .count();

        try {
            PayrollProcessor.PayrollComputation payroll = new PayrollProcessor().computePayroll(emp, totalHoursWorked);
            state.payrollPeriodSummary.setText(String.format(
                    "Payroll period: %s %d. The dashboard is showing %d completed attendance record(s) and %.2f total hour(s) worked.",
                    state.payrollMonthBox.getValue(),
                    year,
                    completedAttendanceRecords,
                    totalHoursWorked));
            state.payrollPeriodSummary.getStyleClass().setAll("dashboard-feedback", "dashboard-feedback-info");
            state.totalHoursLabel.setText(String.format("%.2f hrs", totalHoursWorked));
            state.compensationBasisTitleLabel.setText(getCompensationMetricTitle(emp));
            state.compensationBasisSubtitleLabel.setText(getCompensationMetricSubtitle(emp));
            state.hourlyRateLabel.setText(formatPhp(getDisplayedCompensationBasis(emp, payroll)));
            state.grossPayLabel.setText(formatPhp(payroll.grossPay()));
            state.totalDeductionsLabel.setText(formatPhp(payroll.totalDeductions()));
            state.netPayLabel.setText(formatPhp(payroll.netPay()));
            state.completedAttendanceLabel.setText(String.valueOf(completedAttendanceRecords));
        } catch (IllegalStateException ex) {
            state.payrollPeriodSummary.setText("Payroll computation could not be completed: " + ex.getMessage());
            state.payrollPeriodSummary.getStyleClass().setAll("dashboard-feedback", "dashboard-feedback-error");
            state.totalHoursLabel.setText(String.format("%.2f hrs", totalHoursWorked));
            state.compensationBasisTitleLabel.setText(getCompensationMetricTitle(emp));
            state.compensationBasisSubtitleLabel.setText(getCompensationMetricSubtitle(emp));
            state.hourlyRateLabel.setText(formatPhp(getDisplayedCompensationBasis(emp)));
            state.grossPayLabel.setText("PHP 0.00");
            state.totalDeductionsLabel.setText("PHP 0.00");
            state.netPayLabel.setText("PHP 0.00");
            state.completedAttendanceLabel.setText(String.valueOf(completedAttendanceRecords));
        }
    }

    private boolean showConfirmation(String title, String header, String message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(title);
        confirm.setHeaderText(header);
        confirm.setContentText(message);
        return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void exportEmployeeSummary(Employee emp) {
        File directory = new File("exports");
        if (!directory.exists() && !directory.mkdirs()) {
            showError("Export failed", "Unable to create exports directory.");
            return;
        }

        File output = new File(directory, "employee_" + safe(emp.getId()) + "_summary.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(output))) {
            writer.println("MotorPH Employee Payroll Dashboard Export");
            writer.println("Employee ID: " + safe(emp.getId()));
            writer.println("Name: " + safe(emp.getFirstName()) + " " + safe(emp.getLastName()));
            writer.println("Department: " + safe(emp.getDepartment()));
            writer.println("Position: " + safe(emp.getPosition()));
            writer.println(getCompensationCardTitle(emp) + ": " + formatPhp(getPrimaryCompensationValue(emp)));
            writer.println("Gross Pay: " + formatPhp(getDashboardGrossPayValue(emp)));
            writer.println("Total Deductions: " + String.format("PHP %.2f", emp.calculateDeductions()));
            writer.println("Net Pay: " + String.format("PHP %.2f", emp.calculateNetSalary()));
        } catch (IOException ex) {
            showError("Export failed", ex.getMessage());
            return;
        }

        showInfo("Export complete", "Saved summary to " + output.getPath());
    }

    private void scrollToSection(ScrollPane scrollPane, Region target) {
        Bounds viewport = scrollPane.getViewportBounds();
        Bounds contentBounds = scrollPane.getContent().getLayoutBounds();
        Bounds targetBounds = target.getBoundsInParent();
        double available = contentBounds.getHeight() - viewport.getHeight();
        if (available <= 0) {
            scrollPane.setVvalue(0);
            return;
        }
        scrollPane.setVvalue(Math.max(0, Math.min(1, targetBounds.getMinY() / available)));
    }

    private void playIntroAnimation(Node... nodes) {
        List<ParallelTransition> transitions = new ArrayList<>();
        for (int i = 0; i < nodes.length; i++) {
            Node node = nodes[i];
            node.setOpacity(0);
            node.setTranslateY(18);

            // Subtle staggered motion makes the dashboard feel responsive without being distracting.
            var fade = new FadeTransition(Duration.millis(260), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(i * 70L));

            var move = new TranslateTransition(Duration.millis(320), node);
            move.setFromY(18);
            move.setToY(0);
            move.setDelay(Duration.millis(i * 70L));

            transitions.add(new ParallelTransition(fade, move));
        }

        for (ParallelTransition transition : transitions) {
            transition.play();
        }
    }

    private record AttendanceStatusRow(
            String employeeId,
            String employeeName,
            String date,
            String clockIn,
            String clockOut,
            String status,
            String hoursWorked) {
    }

    private record PayrollPeriodSelection(
            String monthName,
            int month,
            int year) {
    }

    private record PayrollComputationRequest(
            Employee employee,
            PayrollPeriodSelection period) {
    }

    private static final class DashboardFormState {
        private TextField firstNameField;
        private TextField lastNameField;
        private TextField birthdateField;
        private TextField phoneField;
        private TextArea addressArea;
        private TextField sssField;
        private TextField philhealthField;
        private TextField tinField;
        private TextField pagibigField;
        private Label feedbackLabel;
        private ComboBox<String> payrollMonthBox;
        private ComboBox<Integer> payrollYearBox;
        private DatePicker payrollDatePicker;
        private Label payrollPeriodSummary;
        private Label totalHoursLabel;
        private Label compensationBasisTitleLabel;
        private Label compensationBasisSubtitleLabel;
        private Label hourlyRateLabel;
        private Label grossPayLabel;
        private Label totalDeductionsLabel;
        private Label netPayLabel;
        private Label completedAttendanceLabel;

        private List<Control> editableControls() {
            return List.of(birthdateField, phoneField, addressArea, sssField, philhealthField, tinField, pagibigField);
        }
    }

    private void styleActionButton(Button btn, String colorHex) {
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: 700; " +
                    "-fx-background-radius: 10; -fx-padding: 12 16; -fx-font-size: 13; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: derive(" + colorHex + ", -10%); -fx-text-fill: white; -fx-font-weight: 700; " +
                    "-fx-background-radius: 10; -fx-padding: 12 16; -fx-font-size: 13; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: 700; " +
                    "-fx-background-radius: 10; -fx-padding: 12 16; -fx-font-size: 13; -fx-cursor: hand;"));
    }

    private void styleSecondaryActionButton(Button btn) {
        btn.setStyle("-fx-background-color: rgba(148,163,184,0.16); -fx-text-fill: #e2e8f0; " +
                    "-fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 10 14; -fx-font-size: 12; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(148,163,184,0.24); -fx-text-fill: #e2e8f0; " +
                    "-fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 10 14; -fx-font-size: 12; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: rgba(148,163,184,0.16); -fx-text-fill: #e2e8f0; " +
                    "-fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 10 14; -fx-font-size: 12; -fx-cursor: hand;"));
    }

    private void showEmployeePayroll(Employee emp) {
        try {
            Employee.loadContributionRates();
        } catch (Exception ignored) {
        }

        PayrollProcessor.PayrollComputation payroll = new PayrollProcessor().computePayroll(emp, emp.getHoursWorked());
        boolean contractEmployee = PayrollProcessor.isContractEmployee(emp);

        StringBuilder content = new StringBuilder();
        content.append("===================================\n");
        content.append("         PAYROLL SUMMARY\n");
        content.append("===================================\n\n");

        content.append("INCOME\n");
        content.append("------------------------------------\n");
        if (contractEmployee) {
            content.append(String.format("Hourly Rate:         PHP %10.2f\n", payroll.hourlyRate()));
            content.append(String.format("Hours Worked:            %10.2f\n", payroll.hoursWorked()));
            content.append(String.format("Hourly Pay:          PHP %10.2f\n", payroll.basePay()));
        } else {
            content.append(String.format("Basic Salary:        PHP %10.2f\n", payroll.basePay()));
            content.append(String.format("Rice Subsidy:        PHP %10.2f\n", emp.getRiceSubsidy()));
            content.append(String.format("Phone Allowance:     PHP %10.2f\n", emp.getPhoneAllowance()));
            content.append(String.format("Clothing Allowance:  PHP %10.2f\n", emp.getClothingAllowance()));
        }
        content.append("------------------------------------\n");
        content.append(String.format("Gross Salary:        PHP %10.2f\n\n", payroll.grossPay()));

        content.append("DEDUCTIONS\n");
        content.append("------------------------------------\n");
        content.append(String.format("SSS:                 PHP %10.2f\n", payroll.sssDeduction()));
        content.append(String.format("PhilHealth:          PHP %10.2f\n", payroll.philhealthDeduction()));
        content.append(String.format("Pag-IBIG:            PHP %10.2f\n", payroll.pagibigDeduction()));
        content.append(String.format("Withholding Tax:     PHP %10.2f\n", payroll.taxDeduction()));
        content.append("------------------------------------\n");
        content.append(String.format("Total Deductions:    PHP %10.2f\n\n", payroll.totalDeductions()));

        content.append("===================================\n");
        content.append(String.format("NET SALARY:          PHP %10.2f\n", payroll.netPay()));
        content.append("===================================\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payroll Summary");
        alert.setHeaderText("Payroll for " + safe(emp.getFirstName()) + " " + safe(emp.getLastName()));
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    private void showEmployeeAttendance(Employee emp) {
        Stage stage = new Stage();
        stage.setTitle("My Attendance Records");

        TableView<String[]> table = new TableView<>();
        TableColumn<String[], String> colDate = new TableColumn<>("Date");
        TableColumn<String[], String> colLogIn = new TableColumn<>("Clock In");
        TableColumn<String[], String> colLogOut = new TableColumn<>("Clock Out");

        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[3]));
        colLogIn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[4]));
        colLogOut.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[5]));

        colDate.setPrefWidth(150);
        colLogIn.setPrefWidth(150);
        colLogOut.setPrefWidth(150);

        @SuppressWarnings("unchecked")
        TableColumn<String[], String>[] cols = new TableColumn[]{colDate, colLogIn, colLogOut};
        table.getColumns().addAll(cols);

        try {
            List<String[]> attendanceData = loadAttendanceData();
            attendanceData = attendanceData.stream()
                .filter(row -> row.length > 0 && row[0].equals(emp.getId()))
                .toList();
            ObservableList<String[]> data = FXCollections.observableArrayList(attendanceData);
            table.setItems(data);
        } catch (IOException | CsvValidationException e) {
            showError("Error", "Failed to load attendance data.");
        }

        VBox vbox = new VBox(table);
        vbox.setPadding(new Insets(16));
        vbox.setStyle("-fx-background-color: #0b1220;");
        Scene scene = new Scene(vbox, 600, 400);
        stage.setScene(scene);
        stage.show();
    }

    private void logInEmployee(Employee emp) {
        try {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            File file = new File(ATTENDANCE_CSV);
            boolean fileExists = file.exists() && file.length() > 0;

            try (FileWriter fw = new FileWriter(ATTENDANCE_CSV, true);
                 CSVWriter writer = new CSVWriter(fw)) {
                if (!fileExists) {
                    writer.writeNext(new String[]{"Employee #", "Last Name", "First Name", "Date", "Log In", "Log Out"});
                }
                writer.writeNext(new String[]{emp.getId(), emp.getLastName(), emp.getFirstName(), date, time, ""});
                writer.flush();
            }
            showInfo("Clocked In", "You clocked in at " + time);
        } catch (Exception ex) {
            showError("Clock In Error", ex.getMessage());
        }
    }

    private void logOutEmployee(Employee emp) {
        try {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            File file = new File(ATTENDANCE_CSV);
            if (!file.exists()) {
                showError("Error", "Attendance file not found");
                return;
            }

            List<String[]> allData = new ArrayList<>();
            try (CSVReader reader = new CSVReader(new FileReader(ATTENDANCE_CSV))) {
                String[] row;
                while ((row = reader.readNext()) != null) {
                    allData.add(row);
                }
            }

            boolean found = false;
            for (int i = allData.size() - 1; i >= 0; i--) {
                String[] row = allData.get(i);
                if (row.length >= 6 && row[0].equals(emp.getId()) && row[3].equals(date) && row[5] != null && row[5].isEmpty()) {
                    row[5] = time;
                    found = true;
                    break;
                }
            }

            if (!found) {
                showError("Error", "No clock in record found for today");
                return;
            }

            try (FileWriter fw = new FileWriter(ATTENDANCE_CSV);
                 CSVWriter writer = new CSVWriter(fw)) {
                writer.writeAll(allData, false);
                writer.flush();
            }
            showInfo("Clocked Out", "You clocked out at " + time);
        } catch (Exception ex) {
            showError("Clock Out Error", ex.getMessage());
        }
    }

    private VBox buildRightPanel() {
        if (session.isAdmin()) {
            return buildAdminActionsPanel();
        } else {
            // Employee dashboard has its own actions panel integrated
            return new VBox();
        }
    }

    private VBox buildAdminActionsPanel() {
        var panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(320);
        panel.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 16; -fx-border-color: #1e293b; -fx-border-width: 1; -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 24, 0.15, 0, 8);");
        panel.setMaxHeight(Double.MAX_VALUE);

        var title = new Label("Admin Actions");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 16; -fx-font-weight: 700; -fx-padding: 0 0 8 0;");

        var view = new Button("View Profile");
        stylePrimaryButton(view);
        view.setOnAction(e -> viewSelectedProfile());

        var payroll = new Button("Compute Payroll");
        stylePrimaryButton(payroll);
        payroll.setOnAction(e -> computeSelectedPayroll());

        var add = new Button("Add Employee");
        styleSecondaryButton(add);
        add.setOnAction(e -> addEmployee());

        var update = new Button("Update Employee");
        styleSecondaryButton(update);
        update.setOnAction(e -> updateEmployee());

        var del = new Button("Delete Employee");
        styleDangerButton(del);
        del.setOnAction(e -> deleteEmployee());

        var hint = new Label("Manage employee records and payroll data.");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");

        var attendanceButton = createAttendanceButton();

        panel.getChildren().addAll(title, view, payroll, spacer(10), add, update, del, attendanceButton, spacer(8), hint);
        VBox.setVgrow(hint, Priority.ALWAYS);
        return panel;
    }

    // Button to view attendance
    private Button createAttendanceButton() {
        Button viewAttendanceButton = new Button("View Attendance");
        stylePrimaryButton(viewAttendanceButton);
        viewAttendanceButton.setOnAction(e -> showAttendanceWindow());  // Set the action to show the window
        return viewAttendanceButton;
    }

    // Show the attendance window
    private void showAttendanceWindow() {
        Stage stage = new Stage();
        stage.setTitle("Employee Attendance");

        TableView<String[]> table = new TableView<>();
        TableColumn<String[], String> colId = new TableColumn<>("Employee #");
        TableColumn<String[], String> colLastName = new TableColumn<>("Last Name");
        TableColumn<String[], String> colFirstName = new TableColumn<>("First Name");
        TableColumn<String[], String> colDate = new TableColumn<>("Date");
        TableColumn<String[], String> colLogIn = new TableColumn<>("Log In");
        TableColumn<String[], String> colLogOut = new TableColumn<>("Log Out");

        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[0]));
        colLastName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[1]));
        colFirstName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[2]));
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[3]));
        colLogIn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[4]));
        colLogOut.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[5]));

        @SuppressWarnings("unchecked")
        TableColumn<String[], String>[] cols = new TableColumn[]{colId, colLastName, colFirstName, colDate, colLogIn, colLogOut};
        table.getColumns().addAll(cols);

        try {
            List<String[]> rawAttendanceData = loadAttendanceData();
            final List<String[]> attendanceData;
            if (!session.isAdmin()) {
                attendanceData = rawAttendanceData.stream()
                    .filter(row -> row.length > 0 && row[0].equals(session.employeeId()))
                    .toList();
            } else {
                attendanceData = rawAttendanceData;
            }

            var monthLabel = new Label("Month");
            var monthBox = new ComboBox<String>();
            monthBox.getItems().addAll(List.of(
                    "All Months",
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"));
            monthBox.setMaxWidth(Double.MAX_VALUE);

            var yearLabel = new Label("Year");
            var yearBox = new ComboBox<Integer>();
            yearBox.setMaxWidth(Double.MAX_VALUE);

            var availableYears = collectAttendanceYears(attendanceData);
            yearBox.getItems().addAll(availableYears);

            LocalDate latestAttendanceDate = findLatestAttendanceDate(attendanceData).orElse(null);
            monthBox.setValue(latestAttendanceDate == null ? "All Months" : formatMonth(latestAttendanceDate));
            if (latestAttendanceDate != null) {
                yearBox.setValue(latestAttendanceDate.getYear());
            }

            Runnable refreshAttendanceTable = () -> {
                int selectedMonth = parseMonthIndex(monthBox.getValue());
                Integer selectedYear = yearBox.getValue();
                List<String[]> filteredRows = filterAttendanceRowsByPeriod(attendanceData, selectedMonth, selectedYear);
                table.setItems(FXCollections.observableArrayList(filteredRows));
            };

            monthBox.setOnAction(e -> refreshAttendanceTable.run());
            yearBox.setOnAction(e -> refreshAttendanceTable.run());
            refreshAttendanceTable.run();

            var filters = new GridPane();
            filters.setHgap(12);
            filters.setVgap(8);
            filters.add(monthLabel, 0, 0);
            filters.add(monthBox, 0, 1);
            filters.add(yearLabel, 1, 0);
            filters.add(yearBox, 1, 1);
            GridPane.setHgrow(monthBox, Priority.ALWAYS);
            GridPane.setHgrow(yearBox, Priority.ALWAYS);

            VBox vbox = new VBox(12, filters, table);
            vbox.setPadding(new Insets(12));
            Scene scene = new Scene(vbox, 900, 520);
            stage.setScene(scene);
            stage.show();
        } catch (IOException | CsvValidationException e) {
            showError("Error", "Failed to load attendance data.");
        }
    }

    // Load attendance data from CSV
    private List<String[]> loadAttendanceData() throws IOException, CsvValidationException {
        List<String[]> attendanceList = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(ATTENDANCE_CSV))) {
            String[] nextLine;
            reader.readNext(); // Skip header
            while ((nextLine = reader.readNext()) != null) {
                attendanceList.add(nextLine);
            }
        }
        return attendanceList;
    }

    private VBox buildAdminAttendanceInsightsSection() {
        var section = new VBox(12);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: #111827; -fx-background-radius: 14; -fx-border-color: #1f2937; -fx-border-radius: 14;");

        var title = new Label("Attendance Insights");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 15; -fx-font-weight: 700;");

        var subtitle = new Label("Review employee status by date and calculate worked hours for a selected month.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");

        var monthLabel = new Label("Month");
        monthLabel.setStyle("-fx-text-fill: #cbd5e1;");
        var monthBox = new ComboBox<String>();
        monthBox.getItems().addAll(List.of(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"));
        monthBox.setMaxWidth(Double.MAX_VALUE);

        var yearLabel = new Label("Year");
        yearLabel.setStyle("-fx-text-fill: #cbd5e1;");
        var yearBox = new ComboBox<Integer>();
        yearBox.setMaxWidth(Double.MAX_VALUE);

        var dateLabel = new Label("Specific Date");
        dateLabel.setStyle("-fx-text-fill: #cbd5e1;");
        var datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setPromptText("Select a date");

        List<String[]> attendanceRows;
        try {
            attendanceRows = loadAttendanceData();
        } catch (IOException | CsvValidationException e) {
            attendanceRows = Collections.emptyList();
        }

        var availableYears = collectAttendanceYears(attendanceRows);
        if (availableYears.isEmpty()) {
            availableYears.add(LocalDate.now().getYear());
        }
        yearBox.getItems().addAll(availableYears);

        LocalDate initialDate = findLatestAttendanceDate(attendanceRows).orElse(LocalDate.now());
        monthBox.setValue(initialDate.getMonth().name().charAt(0) + initialDate.getMonth().name().substring(1).toLowerCase());
        yearBox.setValue(initialDate.getYear());
        datePicker.setValue(initialDate);

        var filters = new GridPane();
        filters.setHgap(12);
        filters.setVgap(8);
        filters.add(monthLabel, 0, 0);
        filters.add(monthBox, 0, 1);
        filters.add(yearLabel, 1, 0);
        filters.add(yearBox, 1, 1);
        filters.add(dateLabel, 2, 0);
        filters.add(datePicker, 2, 1);
        GridPane.setHgrow(monthBox, Priority.ALWAYS);
        GridPane.setHgrow(yearBox, Priority.ALWAYS);
        GridPane.setHgrow(datePicker, Priority.ALWAYS);

        var statusTable = new TableView<AttendanceStatusRow>();
        statusTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        statusTable.setPrefHeight(240);

        var employeeIdCol = new TableColumn<AttendanceStatusRow, String>("Employee #");
        employeeIdCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().employeeId()));

        var employeeNameCol = new TableColumn<AttendanceStatusRow, String>("Employee");
        employeeNameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().employeeName()));

        var attendanceDateCol = new TableColumn<AttendanceStatusRow, String>("Date");
        attendanceDateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().date()));

        var clockInCol = new TableColumn<AttendanceStatusRow, String>("Clock In");
        clockInCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().clockIn()));

        var clockOutCol = new TableColumn<AttendanceStatusRow, String>("Clock Out");
        clockOutCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().clockOut()));

        var statusCol = new TableColumn<AttendanceStatusRow, String>("Status");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status()));

        var hoursCol = new TableColumn<AttendanceStatusRow, String>("Hours");
        hoursCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().hoursWorked()));

        statusTable.getColumns().addAll(employeeIdCol, employeeNameCol, attendanceDateCol, clockInCol, clockOutCol, statusCol, hoursCol);

        var employeeHoursLabel = new Label("Employee Monthly Hours");
        employeeHoursLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13; -fx-font-weight: 700;");

        var employeeSelector = new ComboBox<Employee>();
        employeeSelector.setItems(employees);
        employeeSelector.setMaxWidth(Double.MAX_VALUE);
        employeeSelector.setPromptText("Select employee");
        employeeSelector.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatEmployeeDisplay(item));
            }
        });
        employeeSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatEmployeeDisplay(item));
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                employeeSelector.setValue(newValue);
            }
        });
        if (!employees.isEmpty()) {
            employeeSelector.setValue(employees.getFirst());
        }

        var monthlyHoursSummary = new Label();
        monthlyHoursSummary.setWrapText(true);
        monthlyHoursSummary.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13; -fx-background-color: #0f172a; -fx-padding: 12; -fx-background-radius: 10;");

        Runnable refreshInsights = () -> refreshAdminAttendanceInsights(
                monthBox,
                yearBox,
                datePicker,
                employeeSelector,
                statusTable,
                monthlyHoursSummary);

        monthBox.setOnAction(e -> {
            syncDatePickerWithMonthYear(monthBox, yearBox, datePicker);
            refreshInsights.run();
        });
        yearBox.setOnAction(e -> {
            syncDatePickerWithMonthYear(monthBox, yearBox, datePicker);
            refreshInsights.run();
        });
        datePicker.setOnAction(e -> {
            LocalDate pickedDate = datePicker.getValue();
            if (pickedDate != null) {
                monthBox.setValue(formatMonth(pickedDate));
                yearBox.setValue(pickedDate.getYear());
            }
            refreshInsights.run();
        });
        employeeSelector.setOnAction(e -> refreshInsights.run());

        refreshInsights.run();

        section.getChildren().addAll(title, subtitle, filters, statusTable, employeeHoursLabel, employeeSelector, monthlyHoursSummary);
        return section;
    }

    private void refreshAdminAttendanceInsights(
            ComboBox<String> monthBox,
            ComboBox<Integer> yearBox,
            DatePicker datePicker,
            ComboBox<Employee> employeeSelector,
            TableView<AttendanceStatusRow> statusTable,
            Label monthlyHoursSummary) {
        try {
            List<String[]> attendanceRows = loadAttendanceData();
            LocalDate selectedDate = datePicker.getValue();
            int selectedMonth = parseMonthIndex(monthBox.getValue());
            Integer selectedYear = yearBox.getValue();

            List<AttendanceStatusRow> statusRows = buildAttendanceStatusRows(attendanceRows, selectedDate, selectedMonth, selectedYear);
            statusTable.setItems(FXCollections.observableArrayList(statusRows));

            Employee selectedEmployee = employeeSelector.getValue();
            if (selectedEmployee == null) {
                monthlyHoursSummary.setText("Select an employee to calculate total worked hours for the chosen month.");
                return;
            }
            if (selectedYear == null || selectedMonth < 1) {
                monthlyHoursSummary.setText("Select a valid month and year to calculate worked hours.");
                return;
            }

            double totalHours = calculateMonthlyHoursForEmployee(attendanceRows, selectedEmployee.getId(), selectedMonth, selectedYear);
            long workedDays = attendanceRows.stream()
                    .filter(row -> safeRowValue(row, 0).equals(selectedEmployee.getId()))
                    .filter(row -> matchesMonthYear(row, selectedMonth, selectedYear))
                    .filter(row -> calculateWorkedHours(row) > 0)
                    .count();

            monthlyHoursSummary.setText(String.format(
                    "%s worked %.2f hours across %d completed attendance record(s) for %s %d.",
                    formatEmployeeDisplay(selectedEmployee),
                    totalHours,
                    workedDays,
                    monthBox.getValue(),
                    selectedYear));
        } catch (IOException | CsvValidationException e) {
            statusTable.setItems(FXCollections.observableArrayList());
            monthlyHoursSummary.setText("Attendance data could not be loaded.");
        }
    }

    private List<AttendanceStatusRow> buildAttendanceStatusRows(
            List<String[]> attendanceRows,
            LocalDate selectedDate,
            int selectedMonth,
            Integer selectedYear) {
        List<AttendanceStatusRow> rows = new ArrayList<>();
        Map<String, String[]> rowsByEmployee = new HashMap<>();

        for (String[] row : attendanceRows) {
            if (!matchesMonthYear(row, selectedMonth, selectedYear)) {
                continue;
            }
            LocalDate attendanceDate = parseAttendanceDate(safeRowValue(row, 3)).orElse(null);
            if (selectedDate != null && !selectedDate.equals(attendanceDate)) {
                continue;
            }
            String employeeId = safeRowValue(row, 0);
            String[] existingRow = rowsByEmployee.get(employeeId);
            if (existingRow == null || calculateWorkedHours(row) > calculateWorkedHours(existingRow)) {
                rowsByEmployee.put(employeeId, row);
            }
        }

        String displayDate = selectedDate == null ? "" : selectedDate.toString();
        for (Employee employee : employees) {
            String[] row = rowsByEmployee.get(employee.getId());
            if (row == null) {
                rows.add(new AttendanceStatusRow(
                        safe(employee.getId()),
                        safe(employee.getFirstName()) + " " + safe(employee.getLastName()),
                        displayDate,
                        "",
                        "",
                        "No attendance record",
                        "0.00"));
                continue;
            }

            double hoursWorked = calculateWorkedHours(row);
            String clockIn = safeRowValue(row, 4);
            String clockOut = safeRowValue(row, 5);
            String status = clockIn.isBlank() ? "No clock in"
                    : clockOut.isBlank() ? "Missing clock out"
                    : "Present";

            rows.add(new AttendanceStatusRow(
                    safeRowValue(row, 0),
                    safeRowValue(row, 2) + " " + safeRowValue(row, 1),
                    safeRowValue(row, 3),
                    clockIn,
                    clockOut,
                    status,
                    String.format("%.2f", hoursWorked)));
        }

        rows.sort(Comparator.comparing(AttendanceStatusRow::employeeId));
        return rows;
    }

    private double calculateMonthlyHoursForEmployee(List<String[]> attendanceRows, String employeeId, int month, int year) {
        double totalHours = 0;
        for (String[] row : attendanceRows) {
            if (!safeRowValue(row, 0).equals(employeeId)) {
                continue;
            }
            if (!matchesMonthYear(row, month, year)) {
                continue;
            }
            totalHours += calculateWorkedHours(row);
        }
        return totalHours;
    }

    private List<String[]> filterAttendanceRowsByPeriod(List<String[]> attendanceRows, int month, Integer year) {
        if (month < 1 || year == null) {
            return new ArrayList<>(attendanceRows);
        }

        return attendanceRows.stream()
                .filter(row -> matchesMonthYear(row, month, year))
                .toList();
    }

    private double calculateWorkedHours(String[] row) {
        Optional<LocalTime> logIn = parseAttendanceTime(safeRowValue(row, 4));
        Optional<LocalTime> logOut = parseAttendanceTime(safeRowValue(row, 5));
        if (logIn.isEmpty() || logOut.isEmpty()) {
            return 0;
        }

        long minutesWorked = ChronoUnit.MINUTES.between(logIn.get(), logOut.get());
        if (minutesWorked <= 0) {
            return 0;
        }
        return minutesWorked / 60.0;
    }

    private boolean matchesMonthYear(String[] row, int month, Integer year) {
        Optional<LocalDate> attendanceDate = parseAttendanceDate(safeRowValue(row, 3));
        return attendanceDate.isPresent()
                && month > 0
                && year != null
                && attendanceDate.get().getMonthValue() == month
                && attendanceDate.get().getYear() == year;
    }

    private Optional<LocalDate> parseAttendanceDate(String value) {
        String trimmed = safeTrim(value);
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("M/d/yy"),
                DateTimeFormatter.ISO_LOCAL_DATE);

        for (DateTimeFormatter formatter : formatters) {
            try {
                return Optional.of(LocalDate.parse(trimmed, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }

    private Optional<LocalTime> parseAttendanceTime(String value) {
        String trimmed = safeTrim(value);
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:mm:ss"),
                DateTimeFormatter.ofPattern("HH:mm:ss"));

        for (DateTimeFormatter formatter : formatters) {
            try {
                return Optional.of(LocalTime.parse(trimmed, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }

    private List<Integer> collectAttendanceYears(List<String[]> attendanceRows) {
        TreeSet<Integer> years = new TreeSet<>();
        for (String[] row : attendanceRows) {
            parseAttendanceDate(safeRowValue(row, 3)).ifPresent(date -> years.add(date.getYear()));
        }
        return new ArrayList<>(years);
    }

    private Optional<LocalDate> findLatestAttendanceDate(List<String[]> attendanceRows) {
        return attendanceRows.stream()
                .map(row -> parseAttendanceDate(safeRowValue(row, 3)))
                .flatMap(Optional::stream)
                .max(LocalDate::compareTo);
    }

    private void syncDatePickerWithMonthYear(ComboBox<String> monthBox, ComboBox<Integer> yearBox, DatePicker datePicker) {
        Integer year = yearBox.getValue();
        int month = parseMonthIndex(monthBox.getValue());
        if (year == null || month < 1) {
            return;
        }

        LocalDate current = datePicker.getValue();
        int day = current == null ? 1 : Math.min(current.getDayOfMonth(), LocalDate.of(year, month, 1).lengthOfMonth());
        datePicker.setValue(LocalDate.of(year, month, day));
    }

    private int parseMonthIndex(String monthName) {
        if (monthName == null || monthName.isBlank()) {
            return -1;
        }
        List<String> months = List.of(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        return months.indexOf(monthName) + 1;
    }

    private String formatMonth(LocalDate date) {
        return date.getMonth().name().charAt(0) + date.getMonth().name().substring(1).toLowerCase();
    }

    private String formatEmployeeDisplay(Employee employee) {
        return safe(employee.getId()) + " - " + safe(employee.getFirstName()) + " " + safe(employee.getLastName());
    }

    private String safeRowValue(String[] row, int index) {
        if (row == null || index < 0 || index >= row.length) {
            return "";
        }
        return safe(row[index]);
    }

    // Add Employee
    private void addEmployee() {
        EmployeeFormData data = EmployeeFormDialog.show(primaryStage, "Add Employee", null);
        if (data == null) return;

        if (employees.stream().anyMatch(e -> safe(e.getId()).equals(data.id()))) {
            showError("Duplicate Employee #", "Employee # already exists: " + data.id());
            return;
        }

        employees.add(data.toEmployee());
        persistEmployees();
        reloadTableItems();
    }

    // Update Employee
    private void updateEmployee() {
        Employee selected = selectedOrNull();
        if (selected == null) {
            showError("No selection", "Select an employee to update.");
            return;
        }

        EmployeeFormData data = EmployeeFormDialog.show(primaryStage, "Update Employee", EmployeeFormData.from(selected));
        if (data == null) return;

        // Prevent changing ID to an existing one
        boolean idChanged = !safe(selected.getId()).equals(data.id());
        if (idChanged && employees.stream().anyMatch(e -> safe(e.getId()).equals(data.id()))) {
            showError("Duplicate Employee #", "Employee # already exists: " + data.id());
            return;
        }

        // Replace in list
        int idx = employees.indexOf(selected);
        employees.set(idx, data.toEmployee());
        persistEmployees();
        reloadTableItems();
        table.getSelectionModel().select(idx);
    }

    // Configure table columns
    private void configureTable() {
    table.getColumns().clear();

    // Create columns
    TableColumn<Employee, String> colId = new TableColumn<>("Employee #");
    colId.setCellValueFactory(cellData -> new SimpleStringProperty(safe(cellData.getValue().getId())));
    colId.setPrefWidth(80);

    TableColumn<Employee, String> colLastName = new TableColumn<>("Last Name");
    colLastName.setCellValueFactory(cellData -> new SimpleStringProperty(safe(cellData.getValue().getLastName())));
    colLastName.setPrefWidth(100);

    TableColumn<Employee, String> colFirstName = new TableColumn<>("First Name");
    colFirstName.setCellValueFactory(cellData -> new SimpleStringProperty(safe(cellData.getValue().getFirstName())));
    colFirstName.setPrefWidth(100);

    TableColumn<Employee, String> colPosition = new TableColumn<>("Position");
    colPosition.setCellValueFactory(cellData -> new SimpleStringProperty(safe(cellData.getValue().getPosition())));
    colPosition.setPrefWidth(120);

    TableColumn<Employee, String> colDepartment = new TableColumn<>("Department");
    colDepartment.setCellValueFactory(cellData -> new SimpleStringProperty(safe(cellData.getValue().getDepartment())));
    colDepartment.setPrefWidth(120);

    // Create an array or list and add columns to it
    List<TableColumn<Employee, ?>> columns = Arrays.asList(colId, colLastName, colFirstName, colPosition, colDepartment);

    // Add columns to the table
    table.getColumns().addAll(columns);
}
    // Get selected employee or null
    private Employee selectedOrNull() {
        return table.getSelectionModel().getSelectedItem();
    }

    // View selected profile
    private void viewSelectedProfile() {
        Employee selected = selectedOrNull();
        if (selected == null) {
            showError("No selection", "Select an employee to view.");
            return;
        }

        StringBuilder details = new StringBuilder();
        details.append("Employee #: ").append(safe(selected.getId())).append("\n");
        details.append("Name: ").append(safe(selected.getFirstName())).append(" ").append(safe(selected.getLastName())).append("\n");
        details.append("Position: ").append(safe(selected.getPosition())).append("\n");
        details.append("Department: ").append(safe(selected.getDepartment())).append("\n");
        details.append("Birthday: ").append(safe(selected.getBirthdate())).append("\n");
        details.append("Address: ").append(safe(selected.getAddress())).append("\n");
        details.append("Phone: ").append(safe(selected.getPhoneNumber())).append("\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Employee Profile");
        alert.setHeaderText("Employee Profile");
        alert.setContentText(details.toString());
        alert.showAndWait();
    }

    private Optional<PayrollComputationRequest> promptPayrollComputationSelection() {
        Employee initiallySelectedEmployee = session.isAdmin()
                ? selectedOrNull()
                : employees.stream()
                    .filter(e -> safe(e.getId()).equals(session.employeeId()))
                    .findFirst()
                    .orElse(null);

        var employeeBox = new ComboBox<Employee>();
        employeeBox.setItems(employees);
        employeeBox.setMaxWidth(Double.MAX_VALUE);
        employeeBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatEmployeeDisplay(item));
            }
        });
        employeeBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatEmployeeDisplay(item));
            }
        });

        if (initiallySelectedEmployee != null) {
            employeeBox.setValue(initiallySelectedEmployee);
        } else if (!employees.isEmpty()) {
            employeeBox.setValue(employees.getFirst());
        }
        employeeBox.setDisable(!session.isAdmin());

        List<String[]> attendanceRows;
        try {
            attendanceRows = loadAttendanceData();
        } catch (IOException | CsvValidationException e) {
            showError("Attendance Error", "Failed to load attendance data for payroll computation.");
            return Optional.empty();
        }

        var monthBox = new ComboBox<String>();
        monthBox.getItems().addAll(List.of(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"));
        monthBox.setMaxWidth(Double.MAX_VALUE);

        var yearBox = new ComboBox<Integer>();
        yearBox.setMaxWidth(Double.MAX_VALUE);
        Runnable refreshAvailablePeriods = () -> {
            Employee selectedEmployee = employeeBox.getValue();
            List<String[]> employeeAttendanceRows = selectedEmployee == null
                    ? List.of()
                    : attendanceRows.stream()
                        .filter(row -> safeRowValue(row, 0).equals(selectedEmployee.getId()))
                        .toList();

            yearBox.getItems().setAll(collectAttendanceYears(employeeAttendanceRows));
            LocalDate latestAttendanceDate = findLatestAttendanceDate(employeeAttendanceRows).orElse(LocalDate.now());
            monthBox.setValue(formatMonth(latestAttendanceDate));
            yearBox.setValue(latestAttendanceDate.getYear());
        };
        refreshAvailablePeriods.run();

        var form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.add(new Label("Employee"), 0, 0);
        form.add(employeeBox, 1, 0);
        form.add(new Label("Month"), 0, 1);
        form.add(monthBox, 1, 1);
        form.add(new Label("Year"), 0, 2);
        form.add(yearBox, 1, 2);
        GridPane.setHgrow(employeeBox, Priority.ALWAYS);
        GridPane.setHgrow(monthBox, Priority.ALWAYS);
        GridPane.setHgrow(yearBox, Priority.ALWAYS);

        employeeBox.setOnAction(e -> refreshAvailablePeriods.run());

        Dialog<PayrollComputationRequest> dialog = new Dialog<>();
        dialog.setTitle("Payroll Period");
        dialog.setHeaderText("Select employee and attendance month for payroll computation");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            Employee employee = employeeBox.getValue();
            int month = parseMonthIndex(monthBox.getValue());
            Integer year = yearBox.getValue();
            if (employee == null || month < 1 || year == null) {
                return null;
            }
            return new PayrollComputationRequest(
                    employee,
                    new PayrollPeriodSelection(monthBox.getValue(), month, year));
        });

        Optional<PayrollComputationRequest> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }

        PayrollComputationRequest request = result.get();
        List<String[]> employeeAttendanceRows = attendanceRows.stream()
                .filter(row -> safeRowValue(row, 0).equals(request.employee().getId()))
                .toList();
        if (employeeAttendanceRows.isEmpty()) {
            showError("Attendance Error", "No attendance data found for the selected employee.");
            return Optional.empty();
        }

        if (request.period().month() < 1 || request.period().year() < 1) {
            showError("Invalid Period", "Select a valid payroll month and year.");
            return Optional.empty();
        }

        return result;
    }

    // Compute payroll for selected employee
    private void computeSelectedPayroll() {
        Optional<PayrollComputationRequest> payrollRequest = promptPayrollComputationSelection();
        if (payrollRequest.isEmpty()) {
            return;
        }

        Employee selected = payrollRequest.get().employee();
        PayrollPeriodSelection payrollPeriod = payrollRequest.get().period();

        PayrollProcessor.PayrollComputation payroll;

        double totalHoursWorked;
        long completedAttendanceRecords;
        try {
            List<String[]> attendanceRows = loadAttendanceData();
            totalHoursWorked = calculateMonthlyHoursForEmployee(
                    attendanceRows,
                    selected.getId(),
                    payrollPeriod.month(),
                    payrollPeriod.year());
            completedAttendanceRecords = attendanceRows.stream()
                    .filter(row -> safeRowValue(row, 0).equals(selected.getId()))
                    .filter(row -> matchesMonthYear(row, payrollPeriod.month(), payrollPeriod.year()))
                    .filter(row -> calculateWorkedHours(row) > 0)
                    .count();
        } catch (IOException | CsvValidationException e) {
            showError("Attendance Error", "Failed to calculate hours worked from attendance data.");
            return;
        }

        try {
            payroll = new PayrollProcessor().computePayroll(selected, totalHoursWorked);
        } catch (IllegalStateException ex) {
            showError("Payroll Error", ex.getMessage());
            return;
        }

        StringBuilder content = new StringBuilder();
        content.append("Payroll period:").append(" ")
                .append(payrollPeriod.monthName()).append(" ")
                .append(payrollPeriod.year()).append("\n");
        content.append("Employee:").append(" ").append(formatEmployeeDisplay(selected)).append("\n");
        content.append("Total hours worked:").append(" ").append(String.format("%.2f", totalHoursWorked)).append("\n");
        content.append("Completed attendance records:").append(" ").append(completedAttendanceRecords).append("\n\n");
        if (PayrollProcessor.isContractEmployee(selected)) {
            content.append("Hourly rate:").append(" ").append(String.format("%.2f", payroll.hourlyRate())).append("\n");
            content.append("Hourly pay:").append(" ").append(String.format("%.2f", payroll.basePay())).append("\n\n");
        } else {
            content.append("Basic salary:").append(" ").append(String.format("%.2f", payroll.basePay())).append("\n");
            content.append("Rice subsidy:").append(" ").append(String.format("%.2f", selected.getRiceSubsidy())).append("\n");
            content.append("Phone allowance:").append(" ").append(String.format("%.2f", selected.getPhoneAllowance())).append("\n");
            content.append("Clothing allowance:").append(" ").append(String.format("%.2f", selected.getClothingAllowance())).append("\n\n");
        }
        content.append("Gross salary:").append(" ").append(String.format("%.2f", payroll.grossPay())).append("\n\n");

        content.append("SSS deduction:").append(" ").append(String.format("%.2f", payroll.sssDeduction())).append("\n");
        content.append("PhilHealth deduction:").append(" ").append(String.format("%.2f", payroll.philhealthDeduction())).append("\n");
        content.append("Pag-IBIG deduction:").append(" ").append(String.format("%.2f", payroll.pagibigDeduction())).append("\n");
        content.append("Tax deduction:").append(" ").append(String.format("%.2f", payroll.taxDeduction())).append("\n");
        content.append("-----------------------\n");
        content.append("Total deductions:").append(" ").append(String.format("%.2f", payroll.totalDeductions())).append("\n\n");
        content.append("Net salary:").append(" ").append(String.format("%.2f", payroll.netPay())).append("\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payroll Computation");
        alert.setHeaderText("Payroll for " + safe(selected.getFirstName()) + " " + safe(selected.getLastName()));
        alert.setContentText(content.toString());
        alert.getDialogPane().setExpandableContent(new TextArea(content.toString()));
        alert.showAndWait();
    }

    // Delete selected employee
    private void deleteEmployee() {
        Employee selected = selectedOrNull();
        if (selected == null) {
            showError("No selection", "Select an employee to delete.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Employee?");
        confirm.setContentText("Are you sure you want to delete " + safe(selected.getFirstName()) + " " + safe(selected.getLastName()) + "?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            employees.remove(selected);
            persistEmployees();
            reloadTableItems();
        }
    }

    // Persist employees to CSV
    private void persistEmployees() {
        writeEmployeesToCsv();
    }

    private boolean writeEmployeesToCsv() {
        try (FileWriter fw = new FileWriter(EMPLOYEE_CSV, EMPLOYEE_CSV_CHARSET);
             CSVWriter writer = new CSVWriter(fw)) {
            
            writer.writeNext(new String[]{
                "Employee #", "Last Name", "First Name", "Birthday", "Address", 
                "Phone Number", "SSS Number", "PhilHealth Number", "TIN Number", "PAG-IBIG Number",
                "Employment Type", "Position", "Department", "Basic Salary", "Rice Subsidy", 
                "Phone Allowance", "Clothing Allowance", "Gross Semi-Monthly Rate", "Hourly Rate"
            });

            for (Employee emp : employees) {
                writer.writeNext(new String[]{
                    safe(emp.getId()),
                    safe(emp.getLastName()),
                    safe(emp.getFirstName()),
                    safe(emp.getBirthdate()),
                    safe(emp.getAddress()),
                    safe(emp.getPhoneNumber()),
                    safe(emp.getSssNo()),
                    safe(emp.getPhilhealthNo()),
                    safe(emp.getTinNo()),
                    safe(emp.getPagibigNo()),
                    safe(emp.getEmploymentType()),
                    safe(emp.getPosition()),
                    safe(emp.getDepartment()),
                    String.valueOf(emp.getBasicSalary()),
                    String.valueOf(emp.getRiceSubsidy()),
                    String.valueOf(emp.getPhoneAllowance()),
                    String.valueOf(emp.getClothingAllowance()),
                    String.valueOf(emp.getGrossSemiMonthlyRate()),
                    String.valueOf(emp.getHourlyRate())
                });
            }
            return true;
        } catch (IOException ex) {
            showError("Save Error", "Failed to save employee data: " + ex.getMessage());
            return false;
        }
    }

    // Reload Table Items
    private void reloadTableItems() {
        if (!session.isAdmin()) {
            return; // Skip for non-admin users as they use the employee dashboard
        }
        table.setItems(employees);  // Admin sees all employees
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg == null ? "" : msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg == null ? "" : msg);
        a.showAndWait();
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(safeTrim(s).replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static Map<String, Integer> buildHeaderIndexMap(String[] header) {
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            indexes.put(safeTrim(header[i]).toLowerCase(), i);
        }
        return indexes;
    }

    private static String readText(String[] row, Map<String, Integer> headerIndexes, int legacyIndex, String... columnNames) {
        for (String columnName : columnNames) {
            Integer index = headerIndexes.get(columnName.toLowerCase());
            if (index != null && index >= 0 && index < row.length) {
                return safeTrim(row[index]);
            }
        }
        if (legacyIndex >= 0 && legacyIndex < row.length) {
            return safeTrim(row[legacyIndex]);
        }
        return "";
    }

    private static double readAmount(String[] row, Map<String, Integer> headerIndexes, int legacyIndex, String columnName, double fallbackValue) {
        String value = readText(row, headerIndexes, legacyIndex, columnName);
        return value.isEmpty() ? fallbackValue : parseDouble(value);
    }

    private static String resolveEmploymentType(String[] row, Map<String, Integer> headerIndexes) {
        String explicitEmploymentType = readText(row, headerIndexes, -1, "Employment Type");
        if (!explicitEmploymentType.isEmpty()) {
            return EmployeeFormData.normalizeEmploymentType(explicitEmploymentType);
        }

        String status = readText(row, headerIndexes, -1, "Status");
        if (!status.isEmpty()) {
            return EmployeeFormData.normalizeEmploymentType(status);
        }

        String position = readText(row, headerIndexes, 9, "Position");
        return position.toLowerCase().contains("contract") ? "contract" : "fulltime";
    }

    private static double deriveHourlyRate(double basicSalary, String employmentType) {
        if ("contract".equals(employmentType)) {
            return 0.0;
        }
        return basicSalary <= 0 ? 0.0 : basicSalary / 168.0;
    }

    private static String formatPhp(double amount) {
        return String.format("PHP %.2f", amount);
    }

    private static String formatCurrency(double amount) {
        return String.format("₱%.2f", amount);
    }

    private static String getCompensationCardTitle(Employee emp) {
        return PayrollProcessor.isContractEmployee(emp) ? "Hourly Rate" : "Monthly Salary";
    }

    private static String getCompensationCardSubtitle(Employee emp) {
        return PayrollProcessor.isContractEmployee(emp) ? "Contract hourly pay basis" : "Basic salary";
    }

    private static String getGrossPayCardSubtitle(Employee emp) {
        return PayrollProcessor.isContractEmployee(emp)
                ? "Based on recorded hours worked"
                : "Salary plus allowances";
    }

    private static double getPrimaryCompensationValue(Employee emp) {
        return PayrollProcessor.isContractEmployee(emp) ? emp.getHourlyRate() : emp.getBasicSalary();
    }

    private static double getDashboardGrossPayValue(Employee emp) {
        return PayrollProcessor.isContractEmployee(emp) ? emp.calculateGrossPay(emp.getHoursWorked()) : emp.grossSalary();
    }

    private static String getCompensationMetricTitle(Employee emp) {
        return PayrollProcessor.isContractEmployee(emp) ? "Hourly Rate" : "Basic Salary";
    }

    private static String getCompensationMetricSubtitle(Employee emp) {
        return PayrollProcessor.isContractEmployee(emp)
                ? "Hourly rate multiplied by hours worked for the selected period."
                : "Fixed monthly salary used directly in payroll computation.";
    }

    private static double getDisplayedCompensationBasis(Employee emp) {
        return PayrollProcessor.isContractEmployee(emp) ? emp.getHourlyRate() : emp.getBasicSalary();
    }

    private static double getDisplayedCompensationBasis(Employee emp, PayrollProcessor.PayrollComputation payroll) {
        return PayrollProcessor.isContractEmployee(emp) ? payroll.hourlyRate() : payroll.basePay();
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private static void styleInput(TextField tf) {
    tf.setStyle("-fx-background-color: white; -fx-border-radius: 10; -fx-border-color: rgba(15,23,42,0.12); -fx-padding: 10 12;");
}

private static void stylePrimaryButton(Button b) {
    b.setMaxWidth(Double.MAX_VALUE);
    b.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 12px 14px; -fx-cursor: pointer;");
    b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 12px 14px; -fx-cursor: pointer;"));
    b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 12px 14px; -fx-cursor: pointer;"));
}

private static void styleSecondaryButton(Button b) {
    b.setMaxWidth(Double.MAX_VALUE);
    b.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 10px 14px; -fx-cursor: pointer;");
    b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #cbd5e1; -fx-text-fill: #334155; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 10px 14px; -fx-cursor: pointer;"));
    b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 10px 14px; -fx-cursor: pointer;"));
}

private static void styleDangerButton(Button b) {
    b.setMaxWidth(Double.MAX_VALUE);
    b.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 12px 14px; -fx-cursor: pointer;");
    b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 12px 14px; -fx-cursor: pointer;"));
    b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 12px 14px; -fx-cursor: pointer;"));
}

private void styleGhostButton(Button b) {
    String baseTextColor = darkModeEnabled ? "#e2e8f0" : "#1e293b";
    String baseBackground = darkModeEnabled ? "rgba(148,163,184,0.12)" : "rgba(15,23,42,0.06)";
    String hoverBackground = darkModeEnabled ? "rgba(148,163,184,0.24)" : "rgba(15,23,42,0.12)";
    b.setStyle("-fx-background-color: " + baseBackground + "; -fx-text-fill: " + baseTextColor + "; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 8px 12px; -fx-cursor: pointer;");
    b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: " + hoverBackground + "; -fx-text-fill: " + baseTextColor + "; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 8px 12px; -fx-cursor: pointer;"));
    b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + baseBackground + "; -fx-text-fill: " + baseTextColor + "; -fx-font-weight: 700; -fx-border-radius: 10px; -fx-padding: 8px 12px; -fx-cursor: pointer;"));
}

private String adminCardStyle() {
    if (darkModeEnabled) {
        return "-fx-background-color: #0f172a; -fx-background-radius: 16; -fx-border-color: #1e293b; -fx-border-width: 1; -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 24, 0.15, 0, 8);";
    }
    return "-fx-background-color: #ffffff; -fx-background-radius: 16; -fx-border-color: #dbe4ee; -fx-border-width: 1; -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 18, 0.12, 0, 6);";
}

private String adminHeadingStyle() {
    return darkModeEnabled
            ? "-fx-text-fill: #e2e8f0; -fx-font-size: 16; -fx-font-weight: 700;"
            : "-fx-text-fill: #0f172a; -fx-font-size: 16; -fx-font-weight: 700;";
}
}
