package motorph.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import motorph.model.AttendanceRecord;
import motorph.model.Employee;
import motorph.model.EmployeeFormData;
import motorph.model.LeaveRequest;
import motorph.model.UserAccount;
import motorph.repository.AttendanceCsvRepository;
import motorph.repository.EmployeeCsvRepository;
import motorph.repository.LeaveCsvRepository;
import motorph.repository.UserCsvRepository;
import motorph.service.AttendanceService;
import motorph.service.AuthenticationService;
import motorph.service.EmployeeService;
import motorph.service.LeaveService;
import motorph.service.PayrollService;
import motorph.service.ValidationService;
import motorph.util.FilePathResolver;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PayrollSystemGUI extends Application {

    private final ValidationService validationService = new ValidationService();
    private final EmployeeCsvRepository employeeRepository = new EmployeeCsvRepository();
    private final UserCsvRepository userRepository = new UserCsvRepository();
    private final AttendanceCsvRepository attendanceRepository = new AttendanceCsvRepository();
    private final LeaveCsvRepository leaveRepository = new LeaveCsvRepository();
    private final EmployeeService employeeService = new EmployeeService(employeeRepository, validationService);
    private final AuthenticationService authenticationService = new AuthenticationService(userRepository, employeeRepository, validationService);
    private final AttendanceService attendanceService = new AttendanceService(attendanceRepository, employeeRepository, validationService);
    private final LeaveService leaveService = new LeaveService(leaveRepository, employeeRepository, validationService);
    private final PayrollService payrollService = new PayrollService();

    private Stage primaryStage;
    private UserAccount session;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("MotorPH Payroll System");
        stage.setScene(buildLoginScene());
        stage.show();
    }

    private Scene buildLoginScene() {
        VBox header = new VBox(12);
        header.getStyleClass().add("login-header");
        Node logo = buildLoginLogo();
        Label brandName = new Label("MotorPH");
        brandName.getStyleClass().add("login-brand-name");
        Label eyebrow = new Label("MOTORPH PAYROLL PLATFORM");
        eyebrow.getStyleClass().add("eyebrow-label");
        Label title = new Label("Professional payroll management for daily operations");
        title.getStyleClass().add("login-title");
        title.setWrapText(true);
        Label subtitle = new Label("Sign in to manage employees, attendance, leave requests, payroll computations, and account setup.");
        subtitle.getStyleClass().add("login-subtitle");
        subtitle.setWrapText(true);
        VBox brandText = new VBox(2, brandName, eyebrow);
        brandText.getStyleClass().add("login-brand-text");
        HBox brandRow = logo == null
                ? new HBox(brandText)
                : new HBox(14, logo, brandText);
        brandRow.getStyleClass().add("login-brand-row");
        brandRow.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(brandRow, title, subtitle);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        styleTextInput(usernameField);
        usernameField.getStyleClass().add("login-field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().addAll("app-text-field", "login-field", "password-main-field");

        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("Password");
        visiblePasswordField.getStyleClass().addAll("app-text-field", "login-field", "password-main-field");
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);

        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());

        Button passwordToggle = new Button("Show");
        passwordToggle.getStyleClass().addAll("app-button", "password-toggle");
        passwordToggle.setFocusTraversable(false);

        StackPane passwordStack = new StackPane(passwordField, visiblePasswordField);
        passwordStack.getStyleClass().add("password-stack");
        HBox.setHgrow(passwordStack, Priority.ALWAYS);

        HBox passwordRow = new HBox(passwordStack, passwordToggle);
        passwordRow.getStyleClass().addAll("password-row", "password-group");
        passwordRow.setAlignment(Pos.CENTER_LEFT);

        Label feedback = new Label();
        feedback.getStyleClass().addAll("status-message", "status-error");
        feedback.setVisible(false);
        feedback.setManaged(false);

        Runnable loginAction = () -> {
            feedback.setText("");
            feedback.setVisible(false);
            feedback.setManaged(false);
            Optional<UserAccount> authenticated = authenticationService.authenticate(usernameField.getText(), passwordField.getText());
            if (authenticated.isEmpty()) {
                showInlineMessage(feedback, "Invalid username or password.", "status-error");
                return;
            }
            session = authenticated.get();
            if (!session.isAdmin() && employeeRepository.findById(session.employeeId()).isEmpty()) {
                showInlineMessage(feedback, "No employee profile is linked to this account.", "status-error");
                session = null;
                return;
            }
            primaryStage.setScene(buildDashboardScene());
        };

        Button loginButton = new Button("Login to Dashboard");
        loginButton.getStyleClass().addAll("app-button", "primary-button", "login-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> loginAction.run());

        usernameField.setOnAction(event -> {
            if (passwordField.getText() == null || passwordField.getText().isBlank()) {
                if (visiblePasswordField.isVisible()) {
                    visiblePasswordField.requestFocus();
                } else {
                    passwordField.requestFocus();
                }
                return;
            }
            loginAction.run();
        });
        passwordField.setOnAction(event -> loginAction.run());
        visiblePasswordField.setOnAction(event -> loginAction.run());

        passwordToggle.setOnAction(event -> {
            boolean showPassword = !visiblePasswordField.isVisible();
            visiblePasswordField.setVisible(showPassword);
            visiblePasswordField.setManaged(showPassword);
            passwordField.setVisible(!showPassword);
            passwordField.setManaged(!showPassword);
            passwordToggle.setText(showPassword ? "Hide" : "Show");
            if (showPassword) {
                visiblePasswordField.requestFocus();
                visiblePasswordField.positionCaret(visiblePasswordField.getText().length());
            } else {
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
        });

        VBox form = new VBox(14, usernameField, passwordRow, feedback, loginButton);
        form.getStyleClass().add("login-form");
        form.setFillWidth(true);

        VBox leftCard = new VBox(26, header, form);
        leftCard.getStyleClass().addAll("panel-card", "login-card");
        leftCard.setPadding(new Insets(34, 34, 30, 34));
        leftCard.setMaxWidth(540);

        StackPane root = new StackPane(leftCard);
        root.getStyleClass().add("app-root");
        root.getStyleClass().add("login-root");
        root.setPadding(new Insets(36));
        Scene scene = createScene(root, 1180, 780);
        Platform.runLater(usernameField::requestFocus);
        return scene;
    }

    private Node buildLoginLogo() {
        try {
            Optional<Path> logoPath = FilePathResolver.resolveExistingPath("motorPH_logo.png", "data/motorPH_logo.png", "motorph/data/motorPH_logo.png");
            if (logoPath.isPresent() && Files.exists(logoPath.get())) {
                ImageView imageView = new ImageView(new Image(logoPath.get().toUri().toString(), true));
                imageView.setFitWidth(120);
                imageView.setFitHeight(120);
                imageView.setPreserveRatio(true);
                StackPane wrapper = new StackPane(imageView);
                wrapper.getStyleClass().add("login-logo-wrap");
                return wrapper;
            }
        } catch (Exception ignored) {
        }

        try (InputStream stream = getClass().getResourceAsStream("/motorPH_logo.png")) {
            if (stream == null) {
                return null;
            }
            ImageView imageView = new ImageView(new Image(stream));
            imageView.setFitWidth(120);
            imageView.setFitHeight(120);
            imageView.setPreserveRatio(true);
            StackPane wrapper = new StackPane(imageView);
            wrapper.getStyleClass().add("login-logo-wrap");
            return wrapper;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Scene buildDashboardScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(buildTopBar());
        root.setCenter(session.isAdmin() ? buildAdminTabs() : buildEmployeeTabs());
        BorderPane.setMargin(root.getCenter(), new Insets(0, 24, 24, 24));
        return createScene(root, 1360, 860);
    }

    private HBox buildTopBar() {
        Label title = new Label(session.isAdmin() ? "Admin Dashboard" : "Employee Workspace");
        title.getStyleClass().add("topbar-title");
        Label subtitle = new Label(session.isAdmin()
                ? "Monitor operations, manage records, and prepare payroll outputs."
                : "Review your profile, attendance, leave requests, and payroll summaries.");
        subtitle.getStyleClass().add("topbar-subtitle");
        VBox titleBox = new VBox(4, title, subtitle);

        Label userLabel = new Label(session.isAdmin() ? session.username() : session.employeeId() + " | " + session.username());
        userLabel.getStyleClass().add("user-pill");

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().addAll("app-button", "secondary-button", "topbar-button");
        logoutButton.setOnAction(event -> {
            session = null;
            primaryStage.setScene(buildLoginScene());
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(16, titleBox, spacer, userLabel, logoutButton);
        bar.getStyleClass().add("top-header");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(20, 24, 20, 24));
        return bar;
    }

    private TabPane buildAdminTabs() {
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("dashboard-tabs");
        tabs.getTabs().addAll(
                tab("Employee Management", buildEmployeeManagementPane(true)),
                tab("Payroll Computation", buildPayrollPane()),
                tab("Attendance Management", buildAttendancePane(true)),
                tab("Leave Management", buildLeavePane(true)),
                tab("Account Management", buildAccountPane()),
                tab("Reports / Summaries", buildReportsPane())
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabs;
    }

    private TabPane buildEmployeeTabs() {
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("dashboard-tabs");
        tabs.getTabs().addAll(
                refreshableEmployeeTab("Profile", this::buildEmployeeProfilePane),
                refreshableEmployeeTab("Attendance", () -> buildAttendancePane(false)),
                refreshableEmployeeTab("Payroll Summary", this::buildPayrollPane),
                refreshableEmployeeTab("Leave Requests", () -> buildLeavePane(false)),
                refreshableEmployeeTab("Account Info", this::buildEmployeeAccountInfo)
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabs;
    }

    private Tab refreshableEmployeeTab(String title, Supplier<Node> contentSupplier) {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        Runnable refresh = () -> tab.setContent(contentSupplier.get());
        refresh.run();
        tab.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                refresh.run();
            }
        });
        return tab;
    }

    private Node buildEmployeeManagementPane(boolean adminView) {
        TableView<Employee> table = employeeTable(FXCollections.observableArrayList(employeeService.getAllEmployees()));
        TextField searchField = new TextField();
        searchField.setPromptText("Search by employee ID, name, position, or department");
        styleTextInput(searchField);

        Runnable refreshTable = () -> {
            List<Employee> filtered = employeeService.searchEmployees(searchField.getText() == null ? "" : searchField.getText().trim());
            table.setItems(FXCollections.observableArrayList(filtered));
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable.run());

        Label statusLabel = buildStatusLabel("Employee records are up to date.", "status-info");

        HBox toolbar = buildToolbar(searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setMaxWidth(Double.MAX_VALUE);
        toolbar.getChildren().add(buildToolbarSpacer());

        if (adminView) {
            Button addButton = actionButton("Add Employee", "primary-button", () -> {
                EmployeeFormData formData = EmployeeFormDialog.show(primaryStage, "Add Employee", null, validationService);
                if (formData == null) {
                    return;
                }
                List<String> errors = employeeService.addEmployee(formData);
                if (!errors.isEmpty()) {
                    showError("Validation Error", String.join("\n", errors));
                    return;
                }
                refreshTable.run();
                statusLabel.setText("Employee record added successfully.");
                statusLabel.getStyleClass().setAll("status-message", "status-success");
            });

            Button editButton = actionButton("Edit Employee", "secondary-button", () -> {
                Employee selected = table.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showError("No selection", "Select an employee to edit.");
                    return;
                }
                EmployeeFormData formData = EmployeeFormDialog.show(primaryStage, "Edit Employee", EmployeeFormData.from(selected), validationService);
                if (formData == null) {
                    return;
                }
                List<String> errors = employeeService.updateEmployee(formData);
                if (!errors.isEmpty()) {
                    showError("Validation Error", String.join("\n", errors));
                    return;
                }
                refreshTable.run();
                statusLabel.setText("Employee record updated successfully.");
                statusLabel.getStyleClass().setAll("status-message", "status-success");
            });

            Button deleteButton = actionButton("Delete Employee", "danger-button", () -> {
                Employee selected = table.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showError("No selection", "Select an employee to delete.");
                    return;
                }
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Employee");
                confirm.setHeaderText("Archive employee record");
                confirm.setContentText("This will archive " + selected.getFullName() + ". Continue?");
                styleAlert(confirm);
                Optional<ButtonType> decision = confirm.showAndWait();
                if (decision.isEmpty() || decision.get() != ButtonType.OK) {
                    return;
                }
                employeeService.archiveEmployee(selected.getId());
                refreshTable.run();
                statusLabel.setText("Employee record archived successfully.");
                statusLabel.getStyleClass().setAll("status-message", "status-warning");
            });

            Button refreshButton = actionButton("Refresh", "ghost-button", refreshTable);
            toolbar.getChildren().addAll(addButton, editButton, deleteButton, refreshButton);
        }

        Node summaryStrip = buildSummaryStrip(List.of(
                buildSummaryCard("Total Employees", String.valueOf(employeeService.getAllEmployees().size()), "active employee records", "summary-card accent-blue"),
                buildSummaryCard("Departments", String.valueOf(distinctDepartments(employeeService.getAllEmployees())), "represented across the roster", "summary-card"),
                buildSummaryCard("Active Accounts", String.valueOf(userRepository.findAll().stream().filter(account -> !account.isAdmin()).count()), "employee logins already linked", "summary-card")
        ));

        return buildModulePage(
                "Employee Management",
                "Manage employee records, profiles, and department assignments in a cleaner administrative workspace.",
                summaryStrip,
                buildCard("Records", "Search, maintain, and review employee information.", new VBox(14, toolbar, statusLabel, buildTableContainer(table))));
    }

    private Node buildPayrollPane() {
        List<Employee> employees = employeeService.getAllEmployees();
        Employee currentEmployee = session.isAdmin()
                ? (employees.isEmpty() ? null : employees.getFirst())
                : employeeRepository.findById(session.employeeId()).orElse(null);
        List<Employee> selectableEmployees = session.isAdmin()
                ? employees
                : currentEmployee == null ? List.of() : List.of(currentEmployee);

        ComboBox<Employee> employeeBox = new ComboBox<>(FXCollections.observableArrayList(selectableEmployees));
        employeeBox.setCellFactory(list -> employeeListCell());
        employeeBox.setButtonCell(employeeListCell());
        employeeBox.setValue(currentEmployee);
        employeeBox.setDisable(!session.isAdmin());
        styleComboBox(employeeBox);

        ComboBox<Integer> monthBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        monthBox.setValue(LocalDate.now().getMonthValue());
        styleComboBox(monthBox);

        ComboBox<Integer> yearBox = new ComboBox<>();
        styleComboBox(yearBox);

        Runnable refreshPayrollYears = () -> {
            Employee selectedEmployee = employeeBox.getValue();
            String employeeId = selectedEmployee == null ? null : selectedEmployee.getId();
            List<Integer> availableYears = attendanceService.getPayrollEligibleYears(session.isAdmin() ? employeeId : session.employeeId());
            Integer currentSelection = yearBox.getValue();
            yearBox.setItems(FXCollections.observableArrayList(availableYears));
            if (currentSelection != null && availableYears.contains(currentSelection)) {
                yearBox.setValue(currentSelection);
            } else {
                yearBox.setValue(availableYears.getFirst());
            }
        };
        refreshPayrollYears.run();
        employeeBox.setOnAction(event -> refreshPayrollYears.run());

        Label infoValue = buildValueLabel("Select an employee and period to generate a payroll summary.");
        Label periodValue = buildValueLabel("-");
        Label hoursValue = buildValueLabel("0.00 hrs");
        Label employmentValue = buildValueLabel("-");
        Label basicSalaryValue = buildValueLabel(formatCurrency(0));
        Label rateValue = buildValueLabel(formatCurrency(0));
        Label basePayValue = buildValueLabel(formatCurrency(0));
        Label allowanceValue = buildValueLabel(formatCurrency(0));
        Label grossPayValue = buildValueLabel(formatCurrency(0));
        Label sssValue = buildValueLabel(formatCurrency(0));
        Label philhealthValue = buildValueLabel(formatCurrency(0));
        Label pagibigValue = buildValueLabel(formatCurrency(0));
        Label taxValue = buildValueLabel(formatCurrency(0));
        Label deductionsValue = buildValueLabel(formatCurrency(0));
        Label netPayValue = new Label(formatCurrency(0));
        netPayValue.getStyleClass().add("net-pay-value");

        TextArea auditArea = new TextArea("Payroll calculations will appear here after computation.");
        auditArea.setEditable(false);
        auditArea.setWrapText(true);
        auditArea.getStyleClass().add("detail-text-area");

        Runnable resetPayrollDisplay = () -> {
            infoValue.setText("Select an employee and period to generate a payroll summary.");
            periodValue.setText("-");
            hoursValue.setText("0.00 hrs");
            employmentValue.setText("-");
            basicSalaryValue.setText(formatCurrency(0));
            rateValue.setText(formatCurrency(0));
            basePayValue.setText(formatCurrency(0));
            allowanceValue.setText(formatCurrency(0));
            grossPayValue.setText(formatCurrency(0));
            sssValue.setText(formatCurrency(0));
            philhealthValue.setText(formatCurrency(0));
            pagibigValue.setText(formatCurrency(0));
            taxValue.setText(formatCurrency(0));
            deductionsValue.setText(formatCurrency(0));
            netPayValue.setText(formatCurrency(0));
            auditArea.setText("Payroll calculations will appear here after computation.");
        };

        Button computeButton = actionButton("Compute Payroll", "primary-button", () -> {
            Employee employee = employeeBox.getValue();
            if (employee == null) {
                showError("Missing employee", "Select an employee to continue.");
                return;
            }
            List<AttendanceRecord> payrollRecords = attendanceService.getAttendanceByEmployeeMonthYear(employee.getId(), monthBox.getValue(), yearBox.getValue());
            double hours = payrollRecords.stream().mapToDouble(AttendanceRecord::hoursWorked).sum();
            PayrollService.PayrollComputation payroll = payrollService.computePayroll(employee, hours);

            infoValue.setText(employee.getId() + " | " + employee.getFullName());
            periodValue.setText(monthName(monthBox.getValue()) + " " + yearBox.getValue());
            hoursValue.setText(String.format("%.2f hrs", hours));
            employmentValue.setText(employee.getEmploymentCategoryLabel());
            basicSalaryValue.setText(formatCurrency(payroll.basicSalary()));
            rateValue.setText(formatCurrency(payroll.hourlyRate()) + " / hr");
            basePayValue.setText(formatCurrency(payroll.basePay()));
            allowanceValue.setText(formatCurrency(payroll.allowancePay()));
            grossPayValue.setText(formatCurrency(payroll.grossPay()));
            sssValue.setText(formatCurrency(payroll.sssDeduction()));
            philhealthValue.setText(formatCurrency(payroll.philhealthDeduction()));
            pagibigValue.setText(formatCurrency(payroll.pagibigDeduction()));
            taxValue.setText(formatCurrency(payroll.taxDeduction()));
            deductionsValue.setText(formatCurrency(payroll.totalDeductions()));
            netPayValue.setText(formatCurrency(payroll.netPay()));
            if (payrollRecords.isEmpty()) {
                auditArea.setText("""
                        No attendance records were found for %s during %s %d.
                        Payroll values below are based on 0.00 worked hours for the selected period.
                        Select a different month or year to compute using available attendance data.
                        """.formatted(
                        employee.getFullName(),
                        monthName(monthBox.getValue()),
                        yearBox.getValue()));
                return;
            }
            auditArea.setText("""
                    Employee: %s
                    Payroll Period: %s %d
                    Hours Worked: %.2f
                    Hourly Rate: %s
                    Gross Pay: %s
                    SSS Deduction: %s
                    PhilHealth Deduction: %s
                    Pag-IBIG Deduction: %s
                    Tax Deduction: %s
                    Total Deductions: %s
                    Net Pay: %s
                    """.formatted(
                    employee.getFullName(),
                    monthName(monthBox.getValue()),
                    yearBox.getValue(),
                    hours,
                    formatCurrency(payroll.hourlyRate()),
                    formatCurrency(payroll.grossPay()),
                    formatCurrency(payroll.sssDeduction()),
                    formatCurrency(payroll.philhealthDeduction()),
                    formatCurrency(payroll.pagibigDeduction()),
                    formatCurrency(payroll.taxDeduction()),
                    formatCurrency(payroll.totalDeductions()),
                    formatCurrency(payroll.netPay())));
        });

        Runnable refreshPayrollView = () -> {
            refreshPayrollYears.run();
            resetPayrollDisplay.run();
        };

        GridPane formGrid = new GridPane();
        formGrid.setHgap(16);
        formGrid.setVgap(14);
        formGrid.add(buildFieldBox("Employee", employeeBox), 0, 0);
        formGrid.add(buildFieldBox("Month", monthBox), 1, 0);
        formGrid.add(buildFieldBox("Year", yearBox), 2, 0);
        HBox actionRow = new HBox(10, computeButton, actionButton("Refresh", "ghost-button", refreshPayrollView));
        formGrid.add(actionRow, 3, 0);
        GridPane.setHgrow(formGrid.getChildren().getFirst(), Priority.ALWAYS);
        computeButton.setMaxWidth(Double.MAX_VALUE);

        HBox summaryRow = new HBox(18,
                buildInfoCard("Employee Information",
                        buildDetailPair("Employee", infoValue),
                        buildDetailPair("Period", periodValue),
                        buildDetailPair("Employment", employmentValue),
                        buildDetailPair("Hours Worked", hoursValue)),
                buildInfoCard("Earnings",
                        buildDetailPair("Basic Salary", basicSalaryValue),
                        buildDetailPair("Hourly Rate", rateValue),
                        buildDetailPair("Base Pay", basePayValue),
                        buildDetailPair("Allowances", allowanceValue),
                        buildDetailPair("Gross Pay", grossPayValue)),
                buildInfoCard("Deductions",
                        buildDetailPair("SSS", sssValue),
                        buildDetailPair("PhilHealth", philhealthValue),
                        buildDetailPair("Pag-IBIG", pagibigValue),
                        buildDetailPair("Withholding Tax", taxValue),
                        buildDetailPair("Total Deductions", deductionsValue)),
                buildNetPayCard(netPayValue));
        summaryRow.setAlignment(Pos.TOP_LEFT);
        summaryRow.setFillHeight(true);
        summaryRow.getChildren().forEach(child -> HBox.setHgrow(child, Priority.ALWAYS));

        VBox content = new VBox(18,
                buildCard("Payroll Form", "Choose an employee and payroll period to generate a structured summary.", formGrid),
                summaryRow,
                buildCard("Computation Log", "Text summary for audit, export, or presentation notes.", auditArea));

        return buildModulePage(
                session.isAdmin() ? "Payroll Computation" : "Payroll Summary",
                "Generate a polished payslip-style summary with clearly grouped earnings, deductions, and net pay.",
                null,
                content);
    }

    private Node buildAttendancePane(boolean adminView) {
        TableView<AttendanceRecord> table = new TableView<>(FXCollections.observableArrayList(
                adminView ? attendanceService.getAllRecords() : attendanceService.getRecordsForEmployee(session.employeeId())));
        configureTable(table);
        table.getColumns().add(column("Employee #", AttendanceRecord::employeeId));
        table.getColumns().add(column("Employee", record -> record.firstName() + " " + record.lastName()));
        table.getColumns().add(column("Date", AttendanceRecord::dateText));
        table.getColumns().add(column("Clock In", AttendanceRecord::logInText));
        table.getColumns().add(column("Clock Out", AttendanceRecord::logOutText));
        TableColumn<AttendanceRecord, String> hoursColumn = column("Hours", record -> String.format("%.2f", record.hoursWorked()));
        hoursColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        table.getColumns().add(hoursColumn);

        ComboBox<String> employeeFilter = new ComboBox<>();
        ComboBox<String> monthFilter = new ComboBox<>(FXCollections.observableArrayList(
                "All Months", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"));
        monthFilter.setValue("All Months");
        styleComboBox(monthFilter);
        ComboBox<Integer> yearFilter = new ComboBox<>();
        styleComboBox(yearFilter);

        Runnable refreshYearOptions = () -> {
            String employeeId = adminView && employeeFilter.getValue() != null && !"All Employees".equals(employeeFilter.getValue())
                    ? employeeFilter.getValue().split("\\|")[0].trim()
                    : (adminView ? null : session.employeeId());
            List<Integer> years = employeeId == null
                    ? attendanceService.getAvailableAttendanceYears()
                    : attendanceService.getAvailableAttendanceYearsForEmployee(employeeId);
            Integer currentSelection = yearFilter.getValue();
            yearFilter.setItems(FXCollections.observableArrayList(years));
            if (years.isEmpty()) {
                yearFilter.setValue(null);
            } else if (currentSelection != null && years.contains(currentSelection)) {
                yearFilter.setValue(currentSelection);
            } else {
                yearFilter.setValue(years.getFirst());
            }
        };

        if (adminView) {
            List<String> employeeOptions = new ArrayList<>();
            employeeOptions.add("All Employees");
            employeeOptions.addAll(employeeService.getAllEmployees().stream()
                    .map(employee -> employee.getId() + " | " + employee.getFullName())
                    .toList());
            employeeFilter.setItems(FXCollections.observableArrayList(employeeOptions));
            employeeFilter.setValue("All Employees");
            styleComboBox(employeeFilter);
        }
        refreshYearOptions.run();

        Label totalRecordsValue = buildSummaryValueLabel("0");
        Label todaysCountValue = buildSummaryValueLabel("0");
        Label averageHoursValue = buildSummaryValueLabel("0.00");

        Runnable applyFilters = () -> {
            List<AttendanceRecord> records = adminView ? attendanceService.getAllRecords() : attendanceService.getRecordsForEmployee(session.employeeId());
            String employeeChoice = adminView ? employeeFilter.getValue() : "All Employees";
            String monthChoice = monthFilter.getValue();
            Integer yearChoice = yearFilter.getValue();
            List<AttendanceRecord> filtered = records.stream()
                    .filter(record -> {
                        if (!adminView || employeeChoice == null || "All Employees".equals(employeeChoice)) {
                            return true;
                        }
                        return employeeChoice.startsWith(record.employeeId() + " ");
                    })
                    .filter(record -> {
                        if (monthChoice == null || "All Months".equals(monthChoice) || record.date() == null) {
                            return true;
                        }
                        return monthName(record.date().getMonthValue()).equals(monthChoice);
                    })
                    .filter(record -> yearChoice == null || record.yearValue() == yearChoice)
                    .toList();
            if (filtered.isEmpty()) {
                table.setPlaceholder(buildEmptyState(
                        "No attendance records found",
                        "Try a different employee, month, or year filter to view matching attendance data."));
            } else {
                table.setPlaceholder(buildEmptyState("No data available", "The selected module does not have matching records yet."));
            }
            table.setItems(FXCollections.observableArrayList(filtered));
            long todaysCount = filtered.stream().filter(record -> LocalDate.now().equals(record.date())).count();
            double averageHours = filtered.stream().filter(AttendanceRecord::isComplete).mapToDouble(AttendanceRecord::hoursWorked).average().orElse(0.0);
            totalRecordsValue.setText(String.valueOf(filtered.size()));
            todaysCountValue.setText(String.valueOf(todaysCount));
            averageHoursValue.setText(String.format("%.2f", averageHours));
        };

        HBox toolbar = buildToolbar();
        if (adminView) {
            toolbar.getChildren().add(buildFieldBox("Employee", employeeFilter));
            employeeFilter.setOnAction(event -> {
                refreshYearOptions.run();
                applyFilters.run();
            });
        }
        toolbar.getChildren().add(buildFieldBox("Month", monthFilter));
        monthFilter.setOnAction(event -> applyFilters.run());
        toolbar.getChildren().add(buildFieldBox("Year", yearFilter));
        yearFilter.setOnAction(event -> applyFilters.run());
        toolbar.getChildren().add(buildToolbarSpacer());

        if (!adminView) {
            Employee employee = employeeRepository.findById(session.employeeId()).orElse(null);
            Button clockInButton = actionButton("Clock In", "primary-button", () -> handleClockAction(employee, table, true, () -> {
                refreshYearOptions.run();
                applyFilters.run();
            }));
            Button clockOutButton = actionButton("Clock Out", "secondary-button", () -> handleClockAction(employee, table, false, () -> {
                refreshYearOptions.run();
                applyFilters.run();
            }));
            toolbar.getChildren().addAll(clockInButton, clockOutButton);
        }

        Button refreshButton = actionButton("Refresh", "ghost-button", () -> {
            table.setItems(FXCollections.observableArrayList(adminView ? attendanceService.getAllRecords() : attendanceService.getRecordsForEmployee(session.employeeId())));
            refreshYearOptions.run();
            applyFilters.run();
        });
        toolbar.getChildren().add(refreshButton);

        Node summaryStrip = buildSummaryStrip(List.of(
                buildSummaryCard("Total Records", totalRecordsValue, "attendance entries loaded", "summary-card accent-blue"),
                buildSummaryCard("Today's Attendance", todaysCountValue, "records dated " + LocalDate.now(), "summary-card"),
                buildSummaryCard("Average Hours", averageHoursValue, "completed shifts on file", "summary-card")
        ));

        applyFilters.run();

        return buildModulePage(
                adminView ? "Attendance Management" : "Attendance",
                adminView
                        ? "Review daily logs, filter attendance data, and keep records easy to scan during demonstrations."
                        : "Monitor your time records and quickly record today's attendance actions.",
                summaryStrip,
                buildCard("Attendance Records", "Attendance data is presented in a cleaner table with filters and clearer time values.", new VBox(14, toolbar, buildTableContainer(table))));
    }

    private Node buildLeavePane(boolean adminView) {
        ObservableList<LeaveRequest> allRequests = FXCollections.observableArrayList(
                adminView ? leaveService.getAllRequests() : leaveService.getRequestsForEmployee(session.employeeId()));

        TableView<LeaveRequest> table = new TableView<>(allRequests);
        configureTable(table);
        table.getColumns().add(column("Request ID", LeaveRequest::requestId));
        table.getColumns().add(column("Employee #", LeaveRequest::employeeId));
        table.getColumns().add(column("Employee", LeaveRequest::employeeName));
        table.getColumns().add(column("Type", LeaveRequest::leaveType));
        table.getColumns().add(column("Start", request -> request.startDate() == null ? "" : request.startDate().toString()));
        table.getColumns().add(column("End", request -> request.endDate() == null ? "" : request.endDate().toString()));
        TableColumn<LeaveRequest, String> statusColumn = column("Status", LeaveRequest::status);
        statusColumn.setCellFactory(col -> statusBadgeCell());
        table.getColumns().add(statusColumn);
        table.setPlaceholder(buildEmptyState(
                adminView ? "No leave requests found" : "No leave requests yet",
                adminView ? "When employees submit leave requests, they will appear here." : "Your submitted leave requests will appear here once filed."));

        Label totalRequestsValue = buildSummaryValueLabel(String.valueOf(allRequests.size()));
        Label pendingValue = buildSummaryValueLabel(String.valueOf(countLeaveStatus(allRequests, "Pending")));
        Label approvedValue = buildSummaryValueLabel(String.valueOf(countLeaveStatus(allRequests, "Approved")));
        Label rejectedValue = buildSummaryValueLabel(String.valueOf(countLeaveStatus(allRequests, "Rejected")));
        Node summaryStrip = buildSummaryStrip(List.of(
                buildSummaryCard("Total Requests", totalRequestsValue, "leave requests on file", "summary-card accent-blue"),
                buildSummaryCard("Pending", pendingValue, "awaiting review", "summary-card accent-orange"),
                buildSummaryCard("Approved", approvedValue, "successfully processed", "summary-card accent-green"),
                buildSummaryCard("Rejected", rejectedValue, "requests declined", "summary-card accent-red")
        ));

        VBox content = new VBox(18);

        if (adminView) {
            ComboBox<String> filterBox = new ComboBox<>(FXCollections.observableArrayList("All Statuses", "Pending", "Approved", "Rejected"));
            filterBox.setValue("All Statuses");
            styleComboBox(filterBox);

            ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList(LeaveService.VALID_STATUSES));
            statusBox.setValue("Pending");
            styleComboBox(statusBox);

            TextField remarksField = new TextField();
            remarksField.setPromptText("Enter review remarks");
            styleTextInput(remarksField);

            Label statusLabel = buildStatusLabel("Select a request, choose a decision, and confirm the update.", "status-info");

            Runnable refreshSummaryCards = () -> {
                List<LeaveRequest> requests = leaveService.getAllRequests();
                totalRequestsValue.setText(String.valueOf(requests.size()));
                pendingValue.setText(String.valueOf(countLeaveStatus(requests, "Pending")));
                approvedValue.setText(String.valueOf(countLeaveStatus(requests, "Approved")));
                rejectedValue.setText(String.valueOf(countLeaveStatus(requests, "Rejected")));
            };

            Runnable refreshAdminLeave = () -> {
                List<LeaveRequest> requests = leaveService.getAllRequests();
                String filter = filterBox.getValue();
                List<LeaveRequest> filtered = requests.stream()
                        .filter(request -> filter == null || "All Statuses".equals(filter) || filter.equalsIgnoreCase(request.status()))
                        .toList();
                table.setItems(FXCollections.observableArrayList(filtered));
                refreshSummaryCards.run();
            };

            filterBox.setOnAction(event -> refreshAdminLeave.run());
            table.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> {
                if (selected == null) {
                    statusLabel.setText("Select a request, choose a decision, and confirm the update.");
                    statusLabel.getStyleClass().setAll("status-message", "status-info");
                    return;
                }
                statusBox.setValue(selected.status());
                remarksField.setText(selected.remarks() == null ? "" : selected.remarks());
                statusLabel.setText("Ready to update " + selected.requestId() + " to " + statusBox.getValue() + ".");
                statusLabel.getStyleClass().setAll("status-message", "status-info");
            });
            statusBox.setOnAction(event -> {
                LeaveRequest selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    statusLabel.setText("Ready to update " + selected.requestId() + " to " + statusBox.getValue() + ".");
                    statusLabel.getStyleClass().setAll("status-message", "status-info");
                }
            });

            Button updateButton = actionButton("Update Status", "primary-button", () -> {
                LeaveRequest selected = table.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showError("No selection", "Select a leave request to update.");
                    return;
                }
                String decision = statusBox.getValue();
                if (decision == null || decision.isBlank()) {
                    showError("Missing decision", "Choose a decision before updating.");
                    return;
                }
                String remarks = remarksField.getText().trim();
                if ("Rejected".equalsIgnoreCase(decision) && remarks.isBlank()) {
                    showError("Missing remarks", "Please provide remarks when rejecting a request.");
                    return;
                }
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Update Leave Status");
                confirm.setHeaderText("Confirm leave decision");
                confirm.setContentText("Change request " + selected.requestId() + " status to " + decision + "?");
                styleAlert(confirm);
                Optional<ButtonType> decisionChoice = confirm.showAndWait();
                if (decisionChoice.isEmpty() || decisionChoice.get() != ButtonType.OK) {
                    return;
                }
                List<String> errors = leaveService.updateLeaveStatus(selected.requestId(), decision, session.username(), remarks);
                if (!errors.isEmpty()) {
                    showError("Leave Error", String.join("\n", errors));
                    return;
                }
                remarksField.clear();
                refreshAdminLeave.run();
                table.getSelectionModel().clearSelection();
                statusBox.setValue("Pending");
                statusLabel.setText("Leave request updated to " + decision + " successfully.");
                statusLabel.getStyleClass().setAll("status-message", "status-success");
            });

            Button refreshButton = actionButton("Refresh", "ghost-button", refreshAdminLeave);
            HBox toolbar = buildToolbar(
                    buildFieldBox("Filter", filterBox),
                    buildFieldBox("Decision", statusBox),
                    buildFieldBox("Remarks", remarksField),
                    buildToolbarSpacer(),
                    updateButton,
                    refreshButton
            );

            content.getChildren().add(buildCard("Review Queue", "Filter requests, add remarks, and approve or reject selected items.", new VBox(14, toolbar, statusLabel, buildTableContainer(table))));
        } else {
            ComboBox<String> leaveTypeBox = new ComboBox<>(FXCollections.observableArrayList("Vacation", "Sick", "Emergency", "Other"));
            leaveTypeBox.setValue("Vacation");
            styleComboBox(leaveTypeBox);

            DatePicker startPicker = new DatePicker();
            DatePicker endPicker = new DatePicker();
            styleDatePicker(startPicker);
            styleDatePicker(endPicker);

            TextField reasonField = new TextField();
            reasonField.setPromptText("Reason for leave");
            styleTextInput(reasonField);

            Label statusLabel = buildStatusLabel("Complete the form to submit a leave request.", "status-info");

            Runnable refreshEmployeeLeave = () -> {
                List<LeaveRequest> requests = leaveService.getRequestsForEmployee(session.employeeId());
                table.setItems(FXCollections.observableArrayList(requests));
                totalRequestsValue.setText(String.valueOf(requests.size()));
                pendingValue.setText(String.valueOf(countLeaveStatus(requests, "Pending")));
                approvedValue.setText(String.valueOf(countLeaveStatus(requests, "Approved")));
                rejectedValue.setText(String.valueOf(countLeaveStatus(requests, "Rejected")));
            };

            Button submitButton = actionButton("Submit Leave Request", "primary-button", () -> {
                Employee employee = employeeRepository.findById(session.employeeId()).orElse(null);
                if (employee == null) {
                    showError("Missing employee", "No linked employee found.");
                    return;
                }
                List<String> errors = leaveService.submitLeave(employee, leaveTypeBox.getValue(), startPicker.getValue(), endPicker.getValue(), reasonField.getText().trim());
                if (!errors.isEmpty()) {
                    showError("Leave Error", String.join("\n", errors));
                    return;
                }
                reasonField.clear();
                startPicker.setValue(null);
                endPicker.setValue(null);
                refreshEmployeeLeave.run();
                statusLabel.setText("Leave request submitted successfully.");
                statusLabel.getStyleClass().setAll("status-message", "status-success");
            });
            Button refreshButton = actionButton("Refresh", "ghost-button", () -> {
                refreshEmployeeLeave.run();
                statusLabel.setText("Leave requests reloaded successfully.");
                statusLabel.getStyleClass().setAll("status-message", "status-info");
            });

            HBox toolbar = buildToolbar(
                    buildFieldBox("Leave Type", leaveTypeBox),
                    buildFieldBox("Start Date", startPicker),
                    buildFieldBox("End Date", endPicker),
                    buildFieldBox("Reason", reasonField),
                    buildToolbarSpacer(),
                    submitButton,
                    refreshButton
            );

            content.getChildren().add(buildCard("Submit Leave Request", "Prepare a complete request with dates and a short reason before submission.", new VBox(14, toolbar, statusLabel)));
            content.getChildren().add(buildCard("Request History", "Track the status of your previous leave requests in a cleaner table.", buildTableContainer(table)));
        }

        return buildModulePage(
                adminView ? "Leave Management" : "Leave Requests",
                adminView
                        ? "Review employee leave requests with better status visibility, summaries, and clearer empty states."
                        : "Submit and monitor leave requests using a more guided, presentation-ready layout.",
                summaryStrip,
                content);
    }

    private Node buildAccountPane() {
        TableView<UserAccount> table = new TableView<>(FXCollections.observableArrayList(userRepository.findAll()));
        configureTable(table);
        table.getColumns().add(column("Username", UserAccount::username));
        table.getColumns().add(column("Role", UserAccount::roleName));
        table.getColumns().add(column("Employee #", account -> account.employeeId() == null ? "" : account.employeeId()));

        TextArea details = new TextArea("Use the actions above to review account coverage and generate missing employee logins.");
        details.setEditable(false);
        details.setWrapText(true);
        details.getStyleClass().add("detail-text-area");

        Label statusLabel = buildStatusLabel("Account coverage can be reviewed from this panel.", "status-info");

        Runnable refreshAccounts = () -> table.setItems(FXCollections.observableArrayList(userRepository.findAll()));

        Button findMissingButton = actionButton("Find Missing Accounts", "secondary-button", () -> {
            List<Employee> missing = authenticationService.findEmployeesMissingAccounts();
            details.setText(missing.isEmpty()
                    ? "All employees have linked user accounts."
                    : "Employees missing accounts:\n\n" + missing.stream()
                    .map(employee -> employee.getId() + " | " + employee.getFullName() + " | " + employee.getDepartment())
                    .collect(Collectors.joining("\n")));
            statusLabel.setText(missing.isEmpty() ? "Account coverage is complete." : "Missing accounts detected. Review the details panel.");
            statusLabel.getStyleClass().setAll("status-message", missing.isEmpty() ? "status-success" : "status-warning");
        });

        Button generateButton = actionButton("Generate Default Accounts", "primary-button", () -> {
            List<UserAccount> created = authenticationService.generateDefaultAccountsForMissingEmployees("changeme");
            refreshAccounts.run();
            details.setText(created.isEmpty()
                    ? "No new accounts were generated."
                    : "Generated accounts:\n\n" + created.stream()
                    .map(account -> account.username() + " / " + account.password())
                    .collect(Collectors.joining("\n")));
            statusLabel.setText(created.isEmpty() ? "No new accounts were needed." : "Default accounts generated successfully.");
            statusLabel.getStyleClass().setAll("status-message", created.isEmpty() ? "status-info" : "status-success");
        });

        Button refreshButton = actionButton("Refresh", "ghost-button", refreshAccounts);

        Node summaryStrip = buildSummaryStrip(List.of(
                buildSummaryCard("Total Accounts", String.valueOf(userRepository.findAll().size()), "user accounts available", "summary-card accent-blue"),
                buildSummaryCard("Missing Logins", String.valueOf(authenticationService.findEmployeesMissingAccounts().size()), "employees still without access", "summary-card accent-orange"),
                buildSummaryCard("Admin Accounts", String.valueOf(userRepository.findAll().stream().filter(UserAccount::isAdmin).count()), "administrator profiles", "summary-card")
        ));

        HBox toolbar = buildToolbar(findMissingButton, generateButton, buildToolbarSpacer(), refreshButton);

        HBox contentRow = new HBox(18,
                buildCard("Account Directory", "Review usernames, roles, and linked employee IDs.", new VBox(14, toolbar, statusLabel, buildTableContainer(table))),
                buildCard("Status & Results", "Operational messages, missing account findings, and generated credentials appear here.", details));
        contentRow.getChildren().forEach(child -> HBox.setHgrow(child, Priority.ALWAYS));

        return buildModulePage(
                "Account Management",
                "Review account coverage, identify missing employee logins, and generate default credentials from a more useful control panel.",
                summaryStrip,
                contentRow);
    }

    private Node buildReportsPane() {
        List<Employee> employees = employeeService.getAllEmployees();
        List<AttendanceRecord> attendance = attendanceService.getAllRecords();
        List<LeaveRequest> leaveRequests = leaveService.getAllRequests();
        List<Employee> missingAccounts = authenticationService.findEmployeesMissingAccounts();

        Node heroCards = buildSummaryStrip(List.of(
                buildSummaryCard("Total Employees", String.valueOf(employees.size()), "records in the employee master list", "summary-card accent-blue"),
                buildSummaryCard("Attendance Records", String.valueOf(attendance.size()), "entries in attendance history", "summary-card"),
                buildSummaryCard("Leave Requests", String.valueOf(leaveRequests.size()), "requests currently tracked", "summary-card"),
                buildSummaryCard("Missing Logins", String.valueOf(missingAccounts.size()), "employees still without accounts", "summary-card accent-orange")
        ));

        PieChart leaveChart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("Pending", countLeaveStatus(leaveRequests, "Pending")),
                new PieChart.Data("Approved", countLeaveStatus(leaveRequests, "Approved")),
                new PieChart.Data("Rejected", countLeaveStatus(leaveRequests, "Rejected"))));
        leaveChart.setTitle("Leave Status Distribution");
        leaveChart.setLegendVisible(true);
        leaveChart.getStyleClass().add("dashboard-chart");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> attendanceChart = new BarChart<>(xAxis, yAxis);
        attendanceChart.setTitle("Attendance Records per Month");
        attendanceChart.setLegendVisible(false);
        attendanceChart.setAnimated(false);
        attendanceChart.getStyleClass().add("dashboard-chart");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Long> monthlyCounts = buildAttendanceCountsByMonth(attendance, LocalDate.now().getYear());
        monthlyCounts.forEach((month, count) -> series.getData().add(new XYChart.Data<>(month, count)));
        attendanceChart.getData().add(series);

        GridPane dataStatus = new GridPane();
        dataStatus.setHgap(14);
        dataStatus.setVgap(14);
        dataStatus.add(buildStatusTile("Leave CSV Status", leaveRequests.isEmpty() ? "Ready for submissions" : "Loaded with " + leaveRequests.size() + " request(s)", "tile-orange"), 0, 0);
        dataStatus.add(buildStatusTile("Attendance CSV Status", attendance.isEmpty() ? "No attendance records found" : "Loaded with " + attendance.size() + " record(s)", "tile-blue"), 1, 0);
        dataStatus.add(buildStatusTile("Account Coverage", missingAccounts.isEmpty() ? "Complete" : missingAccounts.size() + " employee(s) missing login", missingAccounts.isEmpty() ? "tile-green" : "tile-red"), 2, 0);

        VBox insights = new VBox(14,
                buildInsightLine("Largest Department Coverage", mostCommonDepartment(employees)),
                buildInsightLine("Average Recorded Shift", String.format("%.2f hours", attendance.stream().filter(AttendanceRecord::isComplete).mapToDouble(AttendanceRecord::hoursWorked).average().orElse(0.0))),
                buildInsightLine("Pending Leave Workload", countLeaveStatus(leaveRequests, "Pending") + " request(s) waiting for action"),
                buildInsightLine("Presentation Readiness", "Dashboard cards summarize key operational counts at a glance."));

        HBox charts = new HBox(18,
                buildCard("Operational Insights", "High-level indicators for quick administrative review.", insights),
                buildCard("Leave Status Chart", "A simple visual snapshot of request approvals, rejections, and pending items.", leaveChart),
                buildCard("Attendance Totals", "Monthly attendance record counts for the current year.", attendanceChart));
        charts.setAlignment(Pos.TOP_LEFT);
        charts.getChildren().forEach(child -> HBox.setHgrow(child, Priority.ALWAYS));

        VBox body = new VBox(18,
                heroCards,
                buildCard("System Data Status", "Current repository and account coverage conditions are summarized below.", dataStatus),
                charts);

        return buildModulePage(
                "Reports / Summaries",
                "Executive-style overview of staffing, attendance, leave activity, and data readiness for demo presentations.",
                null,
                body);
    }

    private Node buildEmployeeProfilePane() {
        Employee employee = employeeRepository.findById(session.employeeId()).orElse(null);
        if (employee == null) {
            return buildModulePage("Profile", "Employee profile information is shown here when linked to an account.", null,
                    buildCard("Profile", "No linked employee record was found for this account.", buildEmptyState("No employee profile found", "Please contact the administrator to link this account.")));
        }

        Label employeeIdValue = buildSummaryValueLabel(employee.getId());
        Label departmentValue = buildSummaryValueLabel(employee.getDepartment());
        Label positionValue = buildSummaryValueLabel(employee.getPosition());
        Node summaryStrip = buildSummaryStrip(List.of(
                buildSummaryCard("Employee #", employeeIdValue, "linked profile ID", "summary-card accent-blue"),
                buildSummaryCard("Department", departmentValue, "current department assignment", "summary-card"),
                buildSummaryCard("Position", positionValue, "assigned role in MotorPH", "summary-card")
        ));

        GridPane details = new GridPane();
        details.setHgap(14);
        details.setVgap(14);
        Label fullNameValue = buildValueLabel(employee.getFullName());
        Label birthdayValue = buildValueLabel(employee.getBirthdate());
        Label phoneValue = buildValueLabel(employee.getPhoneNumber());
        Label addressValue = buildValueLabel(employee.getAddress());
        Label sssValue = buildValueLabel(employee.getSssNo());
        Label philhealthValue = buildValueLabel(employee.getPhilhealthNo());
        Label tinValue = buildValueLabel(employee.getTinNo());
        Label pagibigValue = buildValueLabel(employee.getPagibigNo());
        Label statusValue = buildValueLabel(employee.getStatus());
        addDetail(details, 0, 0, "Full Name", fullNameValue);
        addDetail(details, 1, 0, "Birthday", birthdayValue);
        addDetail(details, 2, 0, "Phone", phoneValue);
        addDetail(details, 0, 1, "Address", addressValue);
        addDetail(details, 1, 1, "SSS Number", sssValue);
        addDetail(details, 2, 1, "PhilHealth", philhealthValue);
        addDetail(details, 0, 2, "TIN", tinValue);
        addDetail(details, 1, 2, "Pag-IBIG", pagibigValue);
        addDetail(details, 2, 2, "Status", statusValue);

        Runnable refreshProfile = () -> employeeRepository.findById(session.employeeId()).ifPresent(current -> {
            employeeIdValue.setText(current.getId());
            departmentValue.setText(current.getDepartment());
            positionValue.setText(current.getPosition());
            fullNameValue.setText(current.getFullName());
            birthdayValue.setText(current.getBirthdate());
            phoneValue.setText(current.getPhoneNumber());
            addressValue.setText(current.getAddress());
            sssValue.setText(current.getSssNo());
            philhealthValue.setText(current.getPhilhealthNo());
            tinValue.setText(current.getTinNo());
            pagibigValue.setText(current.getPagibigNo());
            statusValue.setText(current.getStatus());
        });

        VBox content = new VBox(14,
                buildToolbar(buildToolbarSpacer(), actionButton("Refresh", "ghost-button", refreshProfile)),
                details);

        return buildModulePage(
                "Profile",
                "Review your current employee details in a cleaner, presentation-ready profile view.",
                summaryStrip,
                buildCard("Employee Details", "This profile groups your primary personal and government-related information.", content));
    }

    private Node buildEmployeeAccountInfo() {
        UserAccount account = userRepository.findByUsername(session.username()).orElse(session);
        Label usernameSummary = buildSummaryValueLabel(account.username());
        Label roleSummary = buildSummaryValueLabel(account.roleName());
        Label linkedSummary = buildSummaryValueLabel(account.employeeId() == null ? "Not linked" : account.employeeId());
        Node summaryStrip = buildSummaryStrip(List.of(
                buildSummaryCard("Username", usernameSummary, "current sign-in account", "summary-card accent-blue"),
                buildSummaryCard("Role", roleSummary, "assigned access level", "summary-card"),
                buildSummaryCard("Linked Employee", linkedSummary, "employee profile connection", "summary-card")
        ));

        HBox usernameLine = buildInsightLine("Username", account.username());
        HBox roleLine = buildInsightLine("Role", account.roleName());
        HBox linkedLine = buildInsightLine("Linked Employee #", account.employeeId() == null ? "Not linked" : account.employeeId());
        HBox passwordLine = buildInsightLine("Password Storage Note", "Passwords are currently stored in plain text in Users.csv.");

        Runnable refreshAccountInfo = () -> {
            UserAccount current = userRepository.findByUsername(session.username()).orElse(session);
            usernameSummary.setText(current.username());
            roleSummary.setText(current.roleName());
            linkedSummary.setText(current.employeeId() == null ? "Not linked" : current.employeeId());
            replaceInsightValue(usernameLine, current.username());
            replaceInsightValue(roleLine, current.roleName());
            replaceInsightValue(linkedLine, current.employeeId() == null ? "Not linked" : current.employeeId());
        };

        VBox body = new VBox(12,
                buildToolbar(buildToolbarSpacer(), actionButton("Refresh", "ghost-button", refreshAccountInfo)),
                usernameLine,
                roleLine,
                linkedLine,
                passwordLine);

        return buildModulePage(
                "Account Info",
                "Account information is grouped here for quick reference during demos or user walkthroughs.",
                summaryStrip,
                buildCard("Account Details", "This section summarizes the current login profile and its linked employee record.", body));
    }

    private void handleClockAction(Employee employee, TableView<AttendanceRecord> table, boolean clockIn, Runnable afterRefresh) {
        if (employee == null) {
            showError("Missing employee", "No linked employee found.");
            return;
        }
        List<String> errors = clockIn
                ? attendanceService.clockIn(employee, LocalDate.now(), LocalTime.now().withSecond(0).withNano(0))
                : attendanceService.clockOut(employee, LocalDate.now(), LocalTime.now().withSecond(0).withNano(0));
        if (!errors.isEmpty()) {
            showError("Attendance Error", String.join("\n", errors));
            return;
        }
        table.setItems(FXCollections.observableArrayList(attendanceService.getRecordsForEmployee(session.employeeId())));
        if (afterRefresh != null) {
            afterRefresh.run();
        }
    }

    private TableView<Employee> employeeTable(ObservableList<Employee> items) {
        TableView<Employee> table = new TableView<>(items);
        configureTable(table);
        table.getColumns().add(column("Employee #", Employee::getId));
        table.getColumns().add(column("Last Name", Employee::getLastName));
        table.getColumns().add(column("First Name", Employee::getFirstName));
        table.getColumns().add(column("Position", Employee::getPosition));
        table.getColumns().add(column("Department", Employee::getDepartment));
        return table;
    }

    private <T> TableColumn<T, String> column(String title, Function<T, String> mapper) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(mapper.apply(cell.getValue())));
        return column;
    }

    private <T> void configureTable(TableView<T> table) {
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(buildEmptyState("No data available", "The selected module does not have matching records yet."));
        table.setFixedCellSize(42);
    }

    private <T> TableCell<T, String> statusBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().addAll("status-badge", badgeStyleForStatus(item));
                setGraphic(badge);
                setText(null);
            }
        };
    }

    private String badgeStyleForStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ENGLISH);
        return switch (normalized) {
            case "approved", "success", "complete", "completed" -> "badge-success";
            case "pending", "warning" -> "badge-warning";
            case "rejected", "delete", "error" -> "badge-danger";
            default -> "badge-info";
        };
    }

    private ListCell<Employee> employeeListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getId() + " - " + item.getFullName());
            }
        };
    }

    private Tab tab(String title, Node node) {
        Tab tab = new Tab(title, node);
        tab.setClosable(false);
        return tab;
    }

    private Scene createScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        String stylesheet = getClass().getResource("/styles.css") == null ? null : getClass().getResource("/styles.css").toExternalForm();
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet);
        }
        return scene;
    }

    private ScrollPane buildModulePage(String title, String subtitle, Node topContent, Node bodyContent) {
        VBox content = new VBox(18);
        content.getStyleClass().add("page-content");
        content.setPadding(new Insets(24));
        content.getChildren().add(buildSectionHeader(title, subtitle));
        if (topContent != null) {
            content.getChildren().add(topContent);
        }
        content.getChildren().add(bodyContent);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("page-scroll");
        return scrollPane;
    }

    private VBox buildSectionHeader(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("page-subtitle");
        subtitleLabel.setWrapText(true);
        return new VBox(6, titleLabel, subtitleLabel);
    }

    private HBox buildToolbar(Node... nodes) {
        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("action-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getChildren().addAll(nodes);
        return toolbar;
    }

    private Region buildToolbarSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private HBox buildSummaryStrip(List<Node> cards) {
        HBox strip = new HBox(16);
        strip.getChildren().addAll(cards);
        cards.forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));
        return strip;
    }

    private VBox buildSummaryCard(String title, String value, String subtitle, String styleClass) {
        return buildSummaryCard(title, buildSummaryValueLabel(value), subtitle, styleClass);
    }

    private VBox buildSummaryCard(String title, Label valueLabel, String subtitle, String styleClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("summary-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("summary-subtitle");
        subtitleLabel.setWrapText(true);
        VBox card = new VBox(10, titleLabel, valueLabel, subtitleLabel);
        card.getStyleClass().addAll(styleClass.split(" "));
        card.setPadding(new Insets(18));
        return card;
    }

    private Label buildSummaryValueLabel(String value) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("summary-value");
        return valueLabel;
    }

    private VBox buildCard(String title, String subtitle, Node content) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("card-subtitle");
        subtitleLabel.setWrapText(true);
        VBox card = new VBox(16, new VBox(4, titleLabel, subtitleLabel), content);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(20));
        VBox.setVgrow(content, Priority.ALWAYS);
        return card;
    }

    private VBox buildInfoCard(String title, Node... details) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("mini-card-title");
        VBox card = new VBox(12, titleLabel);
        card.getChildren().addAll(details);
        card.getStyleClass().addAll("panel-card", "info-card");
        card.setPadding(new Insets(18));
        return card;
    }

    private VBox buildNetPayCard(Label netPayValue) {
        Label title = new Label("Net Pay Summary");
        title.getStyleClass().add("mini-card-title");
        Label helper = new Label("Highlighted total after all deductions.");
        helper.getStyleClass().add("card-subtitle");
        VBox box = new VBox(10, title, helper, netPayValue);
        box.getStyleClass().addAll("panel-card", "net-pay-card");
        box.setPadding(new Insets(22));
        return box;
    }

    private VBox buildDetailPair(String label, Label value) {
        Label key = new Label(label);
        key.getStyleClass().add("detail-label");
        VBox box = new VBox(4, key, value);
        box.getStyleClass().add("detail-pair");
        return box;
    }

    private Label buildValueLabel(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("detail-value");
        label.setWrapText(true);
        return label;
    }

    private HBox buildFieldBox(String label, Node field) {
        Label title = new Label(label);
        title.getStyleClass().add("field-label");
        VBox wrapper = new VBox(6, title, field);
        wrapper.setFillWidth(true);
        HBox box = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        box.getStyleClass().add("field-box");
        return box;
    }

    private StackPane buildTableContainer(TableView<?> table) {
        StackPane wrapper = new StackPane(table);
        wrapper.getStyleClass().add("table-shell");
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private VBox buildEmptyState(String title, String description) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("empty-state-subtitle");
        descriptionLabel.setWrapText(true);
        VBox box = new VBox(8, titleLabel, descriptionLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32));
        box.getStyleClass().add("empty-state");
        return box;
    }

    private Label buildStatusLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("status-message", styleClass);
        label.setWrapText(true);
        return label;
    }

    private VBox buildStatusTile(String title, String value, String toneClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("status-tile-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("status-tile-value");
        VBox box = new VBox(8, titleLabel, valueLabel);
        box.getStyleClass().addAll("status-tile", toneClass);
        box.setPadding(new Insets(18));
        return box;
    }

    private HBox buildInsightLine(String label, String value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("insight-label");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("insight-value");
        valueNode.setWrapText(true);
        HBox row = new HBox(12, labelNode, valueNode);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("insight-row");
        return row;
    }

    private void addDetail(GridPane grid, int column, int row, String label, String value) {
        VBox detail = buildDetailPair(label, buildValueLabel(value == null || value.isBlank() ? "Not provided" : value));
        detail.getStyleClass().add("detail-card");
        grid.add(detail, column, row);
    }

    private void addDetail(GridPane grid, int column, int row, String label, Label valueLabel) {
        VBox detail = buildDetailPair(label, valueLabel);
        detail.getStyleClass().add("detail-card");
        grid.add(detail, column, row);
    }

    private void replaceInsightValue(HBox row, String value) {
        if (row.getChildren().size() > 1 && row.getChildren().get(1) instanceof Label valueLabel) {
            valueLabel.setText(value);
        }
    }

    private Button actionButton(String text, String variant, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("app-button", variant);
        button.setOnAction(event -> action.run());
        return button;
    }

    private void styleTextInput(TextField field) {
        field.getStyleClass().add("app-text-field");
    }

    private void styleComboBox(ComboBox<?> comboBox) {
        comboBox.getStyleClass().add("app-combo-box");
        comboBox.setMaxWidth(Double.MAX_VALUE);
    }

    private void styleDatePicker(DatePicker picker) {
        picker.getStyleClass().add("app-date-picker");
        picker.setMaxWidth(Double.MAX_VALUE);
    }

    private void showInlineMessage(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().setAll("status-message", styleClass);
        label.setVisible(true);
        label.setManaged(true);
    }

    private long distinctDepartments(List<Employee> employees) {
        return employees.stream()
                .map(Employee::getDepartment)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .count();
    }

    private long countLeaveStatus(List<LeaveRequest> requests, String status) {
        return requests.stream().filter(request -> status.equalsIgnoreCase(request.status())).count();
    }

    private Map<String, Long> buildAttendanceCountsByMonth(List<AttendanceRecord> records, int year) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            int currentMonth = month;
            String name = Month.of(currentMonth).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            long count = records.stream()
                    .filter(record -> record.date() != null && record.date().getYear() == year && record.date().getMonthValue() == currentMonth)
                    .count();
            counts.put(name, count);
        }
        return counts;
    }

    private String mostCommonDepartment(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> (entry.getKey() == null || entry.getKey().isBlank() ? "Unassigned" : entry.getKey()) + " (" + entry.getValue() + ")")
                .orElse("No department data");
    }

    private String monthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.ENGLISH, "PHP %,.2f", amount);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        String stylesheet = getClass().getResource("/styles.css") == null ? null : getClass().getResource("/styles.css").toExternalForm();
        if (stylesheet != null) {
            dialogPane.getStylesheets().add(stylesheet);
        }
        dialogPane.getStyleClass().add("app-dialog");
    }
}
