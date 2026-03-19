import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardScene {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private static final double WIDE_LAYOUT_WIDTH = 1180;
    private static final double MEDIUM_LAYOUT_WIDTH = 860;

    private final Stage stage;
    private final ObservableList<Employee> employees;
    private final FilteredList<Employee> filteredEmployees;
    private final ObjectProperty<Employee> selectedEmployee = new SimpleObjectProperty<>();
    private Theme activeTheme = Theme.LIGHT;

    private Scene scene;
    private BorderPane root;
    private GridPane summaryGrid;
    private GridPane detailGrid;
    private VBox detailLeftColumn;
    private VBox detailRightColumn;
    private ComboBox<Employee> employeeSelector;
    private TableView<Employee> employeeTable;
    private final List<Node> summaryCards = new ArrayList<>();

    private Label payrollValueLabel;
    private Label benefitsValueLabel;
    private Label teamValueLabel;
    private Label hoursValueLabel;

    private Label salaryValueLabel;
    private Label grossValueLabel;
    private Label deductionsValueLabel;
    private Label netValueLabel;

    private Label riceValueLabel;
    private Label phoneValueLabel;
    private Label clothingValueLabel;
    private Label hourlyRateValueLabel;

    private Label employeeNameLabel;
    private Label employeeTitleLabel;
    private Label employeeDepartmentLabel;
    private Label employeeStatusLabel;
    private Label employeeIdLabel;
    private Label employeeAddressLabel;
    private Label employeePhoneLabel;
    private Label employeeHoursLabel;

    public DashboardScene(Stage stage, ObservableList<Employee> employees) {
        this.stage = stage;
        this.employees = employees;
        this.filteredEmployees = new FilteredList<>(employees, employee -> true);

        if (!employees.isEmpty()) {
            selectedEmployee.set(employees.get(0));
        }
    }

    public Scene createScene() {
        if (scene != null) {
            return scene;
        }

        root = new BorderPane();
        root.getStyleClass().addAll("root-shell", "theme-host");
        root.setMinSize(980, 720);
        root.setLeft(buildNavigation());
        root.setTop(buildHeader());
        root.setCenter(buildDashboardContent());

        scene = new Scene(root, 1440, 900);
        scene.widthProperty().addListener((observable, oldValue, newValue) -> refreshResponsiveLayout(newValue.doubleValue()));
        setTheme(activeTheme);
        refreshSummary();
        refreshEmployeeCards(selectedEmployee.get());
        refreshResponsiveLayout(scene.getWidth());

        return scene;
    }

    public void setTheme(Theme theme) {
        activeTheme = theme;
        applyTheme(theme);
        if (root != null) {
            root.getStyleClass().removeAll("theme-light", "theme-dark");
            root.getStyleClass().add(theme == Theme.DARK ? "theme-dark" : "theme-light");
        }
        stage.setUserData(theme);
    }

    public void toggleTheme() {
        setTheme(activeTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT);
    }

    private void applyTheme(Theme theme) {
        if (scene == null) {
            return;
        }

        String baseCss = Main.resolveStylesheet("/styles.css");
        String themeCss = Main.resolveStylesheet(theme.stylesheetPath());
        List<String> stylesheets = new ArrayList<>();
        if (baseCss != null) {
            stylesheets.add(baseCss);
        }
        if (themeCss != null) {
            stylesheets.add(themeCss);
        }
        scene.getStylesheets().setAll(stylesheets);
    }

    private VBox buildNavigation() {
        VBox navigation = new VBox(20);
        navigation.getStyleClass().add("nav-bar");
        navigation.setPadding(new Insets(28, 20, 28, 20));
        navigation.setPrefWidth(264);
        navigation.setMinWidth(232);

        HBox brand = new HBox(14, buildLogoNode(), buildBrandCopy());
        brand.setAlignment(Pos.CENTER_LEFT);

        Label helperTitle = new Label("Navigation");
        helperTitle.getStyleClass().add("section-caption");

        VBox navigationLinks = new VBox(10,
                createNavButton("Dashboard", true),
                createNavButton("Employees", false),
                createNavButton("Payroll", false),
                createNavButton("Benefits", false),
                createNavButton("Reports", false)
        );

        VBox helperCard = new VBox(10);
        helperCard.getStyleClass().addAll("surface-card", "info-panel", "dashboard-card");
        helperCard.setPadding(new Insets(18));

        Label helperHeading = new Label("Payroll health");
        helperHeading.getStyleClass().add("panel-title");

        Label helperBody = new Label("Track compensation, benefits, and staffing insights from one responsive dashboard.");
        helperBody.getStyleClass().add("muted-text");
        helperBody.setWrapText(true);

        helperCard.getChildren().addAll(helperHeading, helperBody);
        installHoverMotion(helperCard, 4, 1.01);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        navigation.getChildren().addAll(brand, helperTitle, navigationLinks, helperCard, spacer);
        return navigation;
    }

    private VBox buildBrandCopy() {
        Label title = new Label("MotorPH");
        title.getStyleClass().add("brand-title");

        Label subtitle = new Label("Payroll Command Center");
        subtitle.getStyleClass().add("brand-subtitle");

        VBox box = new VBox(2, title, subtitle);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Region buildLogoNode() {
        ImageView logo = loadImageView(42);
        if (logo != null) {
            StackPane container = new StackPane(logo);
            container.getStyleClass().add("brand-logo");
            return container;
        }

        Label fallback = new Label("M");
        fallback.getStyleClass().addAll("brand-logo", "brand-logo-text");
        fallback.setMinSize(46, 46);
        fallback.setPrefSize(46, 46);
        fallback.setAlignment(Pos.CENTER);
        return fallback;
    }

    private HBox buildHeader() {
        Label heading = new Label("Payroll Dashboard");
        heading.getStyleClass().add("page-title");

        Label subtitle = new Label("Theme-aware payroll overview with cleaner panels, reusable cards, and responsive spacing.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox titleBlock = new VBox(6, heading, subtitle);
        titleBlock.setAlignment(Pos.CENTER_LEFT);

        Button exportButton = new Button("Export Payroll");
        exportButton.getStyleClass().addAll("action-button", "secondary-button");
        installHoverMotion(exportButton, 2, 1.01);

        ToggleButton themeToggle = new ToggleButton("Dark mode");
        themeToggle.getStyleClass().add("theme-toggle");
        themeToggle.setSelected(activeTheme == Theme.DARK);
        themeToggle.selectedProperty().addListener((observable, oldValue, selected) -> {
            themeToggle.setText(selected ? "Light mode" : "Dark mode");
            setTheme(selected ? Theme.DARK : Theme.LIGHT);
        });
        themeToggle.setText(activeTheme == Theme.DARK ? "Light mode" : "Dark mode");
        installHoverMotion(themeToggle, 2, 1.01);

        HBox actions = new HBox(12, exportButton, themeToggle);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(18, titleBlock, spacer, actions);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(26, 28, 18, 28));
        return header;
    }

    private ScrollPane buildDashboardContent() {
        VBox content = new VBox(24);
        content.getStyleClass().add("dashboard-content");
        content.setPadding(new Insets(0, 28, 28, 28));
        content.getChildren().addAll(
                buildHeroSection(),
                buildSummarySection(),
                buildDetailGrid()
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("dashboard-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scrollPane;
    }

    private VBox buildHeroSection() {
        Label eyebrow = new Label("Overview");
        eyebrow.getStyleClass().add("hero-eyebrow");

        employeeNameLabel = new Label();
        employeeNameLabel.getStyleClass().add("hero-title");

        employeeTitleLabel = new Label();
        employeeTitleLabel.getStyleClass().add("hero-subtitle");
        employeeTitleLabel.setWrapText(true);

        VBox textBlock = new VBox(8, eyebrow, employeeNameLabel, employeeTitleLabel);
        textBlock.setAlignment(Pos.CENTER_LEFT);
        textBlock.setMaxWidth(Double.MAX_VALUE);

        employeeSelector = new ComboBox<>(employees);
        employeeSelector.getStyleClass().add("compact-combo");
        employeeSelector.setMaxWidth(Double.MAX_VALUE);
        employeeSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Employee employee) {
                return employee == null ? "" : employee.getFullName() + " | " + employee.getId();
            }

            @Override
            public Employee fromString(String string) {
                return null;
            }
        });
        employeeSelector.getSelectionModel().select(selectedEmployee.get());
        employeeSelector.valueProperty().addListener((observable, oldValue, employee) -> {
            if (employee != null && employee != selectedEmployee.get()) {
                selectedEmployee.set(employee);
                refreshEmployeeCards(employee);
                if (employeeTable != null) {
                    employeeTable.getSelectionModel().select(employee);
                    employeeTable.scrollTo(employee);
                }
            }
        });

        TextField searchField = new TextField();
        searchField.getStyleClass().add("search-field");
        searchField.setPromptText("Search by name, department, or employee ID");
        searchField.textProperty().addListener((observable, oldValue, filter) -> applyFilter(filter));

        VBox controls = new VBox(12, employeeSelector, searchField);
        controls.getStyleClass().add("hero-controls");
        controls.setPrefWidth(360);
        controls.setMinWidth(280);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        HBox hero = new HBox(24, textBlock, spacer, controls);
        hero.getStyleClass().addAll("hero-card", "dashboard-card");
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(26));
        installHoverMotion(hero, 5, 1.005);
        return new VBox(hero);
    }

    private VBox buildSummarySection() {
        summaryGrid = new GridPane();
        summaryGrid.getStyleClass().add("summary-grid");
        summaryGrid.setHgap(18);
        summaryGrid.setVgap(18);

        payrollValueLabel = new Label();
        benefitsValueLabel = new Label();
        teamValueLabel = new Label();
        hoursValueLabel = new Label();

        summaryCards.clear();
        summaryCards.add(createMetricCard("Net payroll", payrollValueLabel, "Current payroll run"));
        summaryCards.add(createMetricCard("Benefits budget", benefitsValueLabel, "Allowance totals"));
        summaryCards.add(createMetricCard("Team members", teamValueLabel, "Employees loaded"));
        summaryCards.add(createMetricCard("Average hours", hoursValueLabel, "Logged working hours"));

        VBox container = new VBox(summaryGrid);
        container.getStyleClass().add("summary-section");
        return container;
    }

    private GridPane buildDetailGrid() {
        detailGrid = new GridPane();
        detailGrid.getStyleClass().add("detail-grid");
        detailGrid.setHgap(18);
        detailGrid.setVgap(18);

        detailLeftColumn = new VBox(18, buildSalaryCard(), buildBenefitsCard(), buildProfileCard());
        detailRightColumn = new VBox(18, buildEmployeeTableCard());
        detailLeftColumn.setFillWidth(true);
        detailRightColumn.setFillWidth(true);

        GridPane.setHgrow(detailLeftColumn, Priority.ALWAYS);
        GridPane.setHgrow(detailRightColumn, Priority.ALWAYS);
        GridPane.setVgrow(detailRightColumn, Priority.ALWAYS);

        return detailGrid;
    }

    private VBox buildSalaryCard() {
        salaryValueLabel = new Label();
        grossValueLabel = new Label();
        deductionsValueLabel = new Label();
        netValueLabel = new Label();

        GridPane breakdown = createTwoColumnMetrics();
        breakdown.add(createDetailMetric("Monthly salary", salaryValueLabel), 0, 0);
        breakdown.add(createDetailMetric("Gross pay", grossValueLabel), 1, 0);
        breakdown.add(createDetailMetric("Deductions", deductionsValueLabel), 0, 1);
        breakdown.add(createDetailMetric("Net pay", netValueLabel), 1, 1);

        VBox card = createCardContainer("Salary Snapshot", "Reusable summary card for pay totals.");
        card.getChildren().add(breakdown);
        return card;
    }

    private VBox buildBenefitsCard() {
        riceValueLabel = new Label();
        phoneValueLabel = new Label();
        clothingValueLabel = new Label();
        hourlyRateValueLabel = new Label();

        GridPane benefitsGrid = createTwoColumnMetrics();
        benefitsGrid.add(createDetailMetric("Rice subsidy", riceValueLabel), 0, 0);
        benefitsGrid.add(createDetailMetric("Phone allowance", phoneValueLabel), 1, 0);
        benefitsGrid.add(createDetailMetric("Clothing allowance", clothingValueLabel), 0, 1);
        benefitsGrid.add(createDetailMetric("Hourly rate", hourlyRateValueLabel), 1, 1);

        VBox card = createCardContainer("Benefits", "Reusable allowance card with consistent spacing and hover state.");
        card.getChildren().add(benefitsGrid);
        return card;
    }

    private VBox buildProfileCard() {
        employeeDepartmentLabel = new Label();
        employeeStatusLabel = new Label();
        employeeIdLabel = new Label();
        employeeAddressLabel = new Label();
        employeePhoneLabel = new Label();
        employeeHoursLabel = new Label();

        GridPane profileGrid = createTwoColumnMetrics();
        profileGrid.add(createDetailMetric("Department", employeeDepartmentLabel), 0, 0);
        profileGrid.add(createDetailMetric("Employment type", employeeStatusLabel), 1, 0);
        profileGrid.add(createDetailMetric("Employee ID", employeeIdLabel), 0, 1);
        profileGrid.add(createDetailMetric("Hours worked", employeeHoursLabel), 1, 1);

        Separator separator = new Separator();

        VBox details = new VBox(12,
                createSimpleLine("Address", employeeAddressLabel),
                createSimpleLine("Phone", employeePhoneLabel)
        );

        VBox card = createCardContainer("Employee Profile", "Selected employee details update automatically.");
        card.getChildren().addAll(profileGrid, separator, details);
        return card;
    }

    private VBox buildEmployeeTableCard() {
        employeeTable = new TableView<>(filteredEmployees);
        employeeTable.getStyleClass().add("employee-table");
        employeeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        employeeTable.setMinHeight(420);
        VBox.setVgrow(employeeTable, Priority.ALWAYS);

        TableColumn<Employee, String> idColumn = new TableColumn<>("Employee ID");
        idColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));

        TableColumn<Employee, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));

        TableColumn<Employee, String> departmentColumn = new TableColumn<>("Department");
        departmentColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDepartment()));

        TableColumn<Employee, String> positionColumn = new TableColumn<>("Position");
        positionColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPosition()));

        TableColumn<Employee, String> netPayColumn = new TableColumn<>("Net Pay");
        netPayColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                money(cell.getValue().calculateNetSalary())
        ));

        employeeTable.getColumns().addAll(idColumn, nameColumn, departmentColumn, positionColumn, netPayColumn);
        employeeTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, employee) -> {
            if (employee != null) {
                selectedEmployee.set(employee);
                refreshEmployeeCards(employee);
                if (employeeSelector != null && employeeSelector.getValue() != employee) {
                    employeeSelector.getSelectionModel().select(employee);
                }
            }
        });

        if (selectedEmployee.get() != null) {
            employeeTable.getSelectionModel().select(selectedEmployee.get());
        }

        VBox card = createCardContainer("Employee Directory", "Responsive table and cards stay aligned as the stage grows.");
        VBox.setVgrow(card, Priority.ALWAYS);
        card.getChildren().add(employeeTable);
        return card;
    }

    private GridPane createTwoColumnMetrics() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        ColumnConstraints columnOne = new ColumnConstraints();
        columnOne.setPercentWidth(50);
        columnOne.setHgrow(Priority.ALWAYS);

        ColumnConstraints columnTwo = new ColumnConstraints();
        columnTwo.setPercentWidth(50);
        columnTwo.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().setAll(columnOne, columnTwo);
        return grid;
    }

    private VBox createMetricCard(String title, Label valueLabel, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");

        valueLabel.getStyleClass().add("metric-value");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("metric-subtitle");
        subtitleLabel.setWrapText(true);

        VBox card = new VBox(10, titleLabel, valueLabel, subtitleLabel);
        card.getStyleClass().addAll("surface-card", "metric-card", "dashboard-card");
        card.setPadding(new Insets(18));
        card.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(card, Priority.ALWAYS);
        installHoverMotion(card, 4, 1.01);
        return card;
    }

    private VBox createCardContainer(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("panel-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("muted-text");
        subtitleLabel.setWrapText(true);

        VBox card = new VBox(18, titleLabel, subtitleLabel);
        card.getStyleClass().addAll("surface-card", "content-card", "dashboard-card");
        card.setPadding(new Insets(20));
        card.setMaxWidth(Double.MAX_VALUE);
        installHoverMotion(card, 4, 1.008);
        return card;
    }

    private VBox createDetailMetric(String title, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("detail-title");

        valueLabel.getStyleClass().add("detail-value");
        valueLabel.setWrapText(true);

        VBox box = new VBox(6, titleLabel, valueLabel);
        box.getStyleClass().add("detail-block");
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox createSimpleLine(String title, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("detail-title");

        valueLabel.getStyleClass().add("detail-value");
        valueLabel.setWrapText(true);

        VBox line = new VBox(4, titleLabel, valueLabel);
        line.getStyleClass().add("simple-detail-line");
        return line;
    }

    private Button createNavButton(String text, boolean active) {
        Button button = new Button(text);
        button.getStyleClass().addAll("nav-button", active ? "nav-button-active" : "nav-button-muted");
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setContentDisplay(ContentDisplay.LEFT);
        installHoverMotion(button, 2, 1.01);
        return button;
    }

    private void applyFilter(String filterText) {
        String normalized = filterText == null ? "" : filterText.trim().toLowerCase();
        filteredEmployees.setPredicate(employee -> {
            if (normalized.isEmpty()) {
                return true;
            }

            return employee.getFullName().toLowerCase().contains(normalized)
                    || employee.getId().toLowerCase().contains(normalized)
                    || employee.getDepartment().toLowerCase().contains(normalized)
                    || employee.getPosition().toLowerCase().contains(normalized);
        });
    }

    private void refreshSummary() {
        double totalNetPay = employees.stream()
                .mapToDouble(Employee::calculateNetSalary)
                .sum();

        double totalBenefits = employees.stream()
                .mapToDouble(employee -> employee.getRiceSubsidy() + employee.getPhoneAllowance() + employee.getClothingAllowance())
                .sum();

        double averageHours = employees.stream()
                .mapToDouble(Employee::getHoursWorked)
                .average()
                .orElse(0.0);

        payrollValueLabel.setText(money(totalNetPay));
        benefitsValueLabel.setText(money(totalBenefits));
        teamValueLabel.setText(Integer.toString(employees.size()));
        hoursValueLabel.setText(String.format(Locale.ENGLISH, "%.1f hrs", averageHours));
    }

    private void refreshEmployeeCards(Employee employee) {
        if (employee == null) {
            return;
        }

        employeeNameLabel.setText(employee.getFullName());
        employeeTitleLabel.setText(employee.getPosition() + " | " + employee.getDepartment());

        salaryValueLabel.setText(money(employee.getBasicSalary()));
        grossValueLabel.setText(money(employee.calculateGrossPay(employee.getHoursWorked())));
        deductionsValueLabel.setText(money(employee.calculateDeductions(employee.getHoursWorked())));
        netValueLabel.setText(money(employee.calculateNetSalary(employee.getHoursWorked())));

        riceValueLabel.setText(money(employee.getRiceSubsidy()));
        phoneValueLabel.setText(money(employee.getPhoneAllowance()));
        clothingValueLabel.setText(money(employee.getClothingAllowance()));
        hourlyRateValueLabel.setText(money(employee.getHourlyRate()));

        employeeDepartmentLabel.setText(emptyFallback(employee.getDepartment()));
        employeeStatusLabel.setText(emptyFallback(employee.getEmploymentType()));
        employeeIdLabel.setText(employee.getId());
        employeeAddressLabel.setText(emptyFallback(employee.getAddress()));
        employeePhoneLabel.setText(emptyFallback(employee.getPhoneNumber()));
        employeeHoursLabel.setText(String.format(Locale.ENGLISH, "%.1f hrs", employee.getHoursWorked()));
    }

    private void refreshResponsiveLayout(double width) {
        refreshSummaryLayout(width);
        refreshDetailLayout(width);
    }

    private void refreshSummaryLayout(double width) {
        if (summaryGrid == null) {
            return;
        }

        summaryGrid.getChildren().clear();
        summaryGrid.getColumnConstraints().clear();
        summaryGrid.getRowConstraints().clear();

        int columns = width >= WIDE_LAYOUT_WIDTH ? 4 : (width >= MEDIUM_LAYOUT_WIDTH ? 2 : 1);
        for (int index = 0; index < summaryCards.size(); index++) {
            Node card = summaryCards.get(index);
            int column = index % columns;
            int row = index / columns;
            summaryGrid.add(card, column, row);
        }

        for (int column = 0; column < columns; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / columns);
            constraints.setHgrow(Priority.ALWAYS);
            summaryGrid.getColumnConstraints().add(constraints);
        }
    }

    private void refreshDetailLayout(double width) {
        if (detailGrid == null) {
            return;
        }

        detailGrid.getChildren().clear();
        detailGrid.getColumnConstraints().clear();
        detailGrid.getRowConstraints().clear();

        if (width >= WIDE_LAYOUT_WIDTH) {
            ColumnConstraints left = new ColumnConstraints();
            left.setPercentWidth(40);
            left.setHgrow(Priority.ALWAYS);

            ColumnConstraints right = new ColumnConstraints();
            right.setPercentWidth(60);
            right.setHgrow(Priority.ALWAYS);

            detailGrid.getColumnConstraints().setAll(left, right);
            detailGrid.add(detailLeftColumn, 0, 0);
            detailGrid.add(detailRightColumn, 1, 0);
        } else {
            ColumnConstraints single = new ColumnConstraints();
            single.setPercentWidth(100);
            single.setHgrow(Priority.ALWAYS);
            detailGrid.getColumnConstraints().setAll(single);

            RowConstraints top = new RowConstraints();
            top.setVgrow(Priority.NEVER);

            RowConstraints bottom = new RowConstraints();
            bottom.setVgrow(Priority.ALWAYS);

            detailGrid.getRowConstraints().setAll(top, bottom);
            detailGrid.add(detailLeftColumn, 0, 0);
            detailGrid.add(detailRightColumn, 0, 1);
        }
    }

    private void installHoverMotion(Node node, double liftAmount, double scaleAmount) {
        TranslateTransition translateIn = new TranslateTransition(Duration.millis(180), node);
        translateIn.setToY(-liftAmount);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(180), node);
        scaleIn.setToX(scaleAmount);
        scaleIn.setToY(scaleAmount);

        TranslateTransition translateOut = new TranslateTransition(Duration.millis(180), node);
        translateOut.setToY(0);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(180), node);
        scaleOut.setToX(1);
        scaleOut.setToY(1);

        node.setOnMouseEntered(event -> new ParallelTransition(translateIn, scaleIn).playFromStart());
        node.setOnMouseExited(event -> new ParallelTransition(translateOut, scaleOut).playFromStart());
    }

    private static String money(double value) {
        return CURRENCY_FORMAT.format(value);
    }

    private static String emptyFallback(String value) {
        return value == null || value.isBlank() ? "Not available" : value;
    }

    private ImageView loadImageView(double fitWidth) {
        String[] resourceCandidates = {"/images/motorPH_logo.png", "/motorPH_logo.png"};
        for (String resourcePath : resourceCandidates) {
            try {
                String stylesheet = Main.resolveStylesheet(resourcePath);
                if (stylesheet != null) {
                    Image image = new Image(stylesheet);
                    if (!image.isError()) {
                        return createImageView(image, fitWidth);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        try {
            Path fallbackPath = Main.resolveDataPath("motorPH_logo.png");
            if (Files.exists(fallbackPath)) {
                Image image = new Image(fallbackPath.toUri().toString());
                if (!image.isError()) {
                    return createImageView(image, fitWidth);
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private ImageView createImageView(Image image, double fitWidth) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(fitWidth);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        return imageView;
    }

    public enum Theme {
        LIGHT("/light-theme.css"),
        DARK("/dark-theme.css");

        private final String stylesheetPath;

        Theme(String stylesheetPath) {
            this.stylesheetPath = stylesheetPath;
        }

        public String stylesheetPath() {
            return stylesheetPath;
        }
    }
}
