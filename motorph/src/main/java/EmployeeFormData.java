import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

public record EmployeeFormData(
        String id,
        String lastName,
        String firstName,
        String birthdate,
        String address,
        String phoneNumber,
        String sssNumber,
        String philhealthNumber,
        String tinNumber,
        String pagibigNumber,
        String employmentType,
        String position,
        String department,
        double basicSalary,
        double riceSubsidy,
        double phoneAllowance,
        double clothingAllowance,
        double hourlyRate
) {

    public static final String FULL_TIME = "Full Time";
    public static final String CONTRACT = "Contract";

    public static EmployeeFormData show(Window owner, String title, EmployeeFormData initialData) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setWidth(560);
        dialog.setHeight(610);
        dialog.setResizable(false);

        TextField tfId = createTextField();
        TextField tfLastName = createTextField();
        TextField tfFirstName = createTextField();
        TextField tfBirthdate = createTextField();
        TextField tfAddress = createTextField();
        TextField tfPhone = createTextField();
        TextField tfSss = createTextField();
        TextField tfPhilhealth = createTextField();
        TextField tfTin = createTextField();
        TextField tfPagibig = createTextField();
        ComboBox<String> employmentTypeBox = new ComboBox<>(FXCollections.observableArrayList(FULL_TIME, CONTRACT));
        employmentTypeBox.setMaxWidth(Double.MAX_VALUE);
        employmentTypeBox.setValue(FULL_TIME);
        TextField tfPosition = createTextField();
        TextField tfDepartment = createTextField();
        TextField tfBasicSalary = createTextField();
        TextField tfRiceSubsidy = createTextField();
        TextField tfPhoneAllowance = createTextField();
        TextField tfClothingAllowance = createTextField();
        TextField tfHourlyRate = createTextField();

        if (initialData != null) {
            tfId.setText(initialData.id());
            tfLastName.setText(initialData.lastName());
            tfFirstName.setText(initialData.firstName());
            tfBirthdate.setText(initialData.birthdate());
            tfAddress.setText(initialData.address());
            tfPhone.setText(initialData.phoneNumber());
            tfSss.setText(initialData.sssNumber());
            tfPhilhealth.setText(initialData.philhealthNumber());
            tfTin.setText(initialData.tinNumber());
            tfPagibig.setText(initialData.pagibigNumber());
            employmentTypeBox.setValue(initialData.employmentType());
            tfPosition.setText(initialData.position());
            tfDepartment.setText(initialData.department());
            tfBasicSalary.setText(String.valueOf(initialData.basicSalary()));
            tfRiceSubsidy.setText(String.valueOf(initialData.riceSubsidy()));
            tfPhoneAllowance.setText(String.valueOf(initialData.phoneAllowance()));
            tfClothingAllowance.setText(String.valueOf(initialData.clothingAllowance()));
            tfHourlyRate.setText(String.valueOf(initialData.hourlyRate()));
            tfId.setDisable(true);
        }

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
                new Tab("Personal Info", createPersonalGrid(tfId, tfFirstName, tfLastName, tfBirthdate, tfAddress, tfPhone)),
                new Tab("Government IDs", createGovernmentGrid(tfSss, tfPhilhealth, tfTin, tfPagibig)),
                new Tab("Employment", createEmploymentGrid(employmentTypeBox, tfPosition, tfDepartment)),
                new Tab("Compensation", createCompensationGrid(tfBasicSalary, tfRiceSubsidy, tfPhoneAllowance, tfClothingAllowance, tfHourlyRate))
        );

        Button btnOk = new Button("Save");
        Button btnCancel = new Button("Cancel");
        styleButton(btnOk, "#2563eb");
        styleButton(btnCancel, "#ef4444");

        final EmployeeFormData[] result = {null};
        btnOk.setOnAction(e -> {
            List<String> errors = validateForm(
                    tfId, tfLastName, tfFirstName, tfBirthdate, tfAddress, tfPhone,
                    tfSss, tfPhilhealth, tfTin, tfPagibig, employmentTypeBox,
                    tfPosition, tfDepartment, tfBasicSalary, tfRiceSubsidy, tfPhoneAllowance,
                    tfClothingAllowance, tfHourlyRate);
            if (!errors.isEmpty()) {
                showErrorDialog("Validation Error", String.join("\n", errors));
                return;
            }

            result[0] = new EmployeeFormData(
                    tfId.getText().trim(),
                    tfLastName.getText().trim(),
                    tfFirstName.getText().trim(),
                    tfBirthdate.getText().trim(),
                    tfAddress.getText().trim(),
                    tfPhone.getText().trim(),
                    tfSss.getText().trim(),
                    tfPhilhealth.getText().trim(),
                    tfTin.getText().trim(),
                    tfPagibig.getText().trim(),
                    employmentTypeBox.getValue(),
                    tfPosition.getText().trim(),
                    tfDepartment.getText().trim(),
                    parseAmount(tfBasicSalary.getText().trim()),
                    parseAmount(tfRiceSubsidy.getText().trim()),
                    parseAmount(tfPhoneAllowance.getText().trim()),
                    parseAmount(tfClothingAllowance.getText().trim()),
                    parseAmount(tfHourlyRate.getText().trim())
            );
            dialog.close();
        });
        btnCancel.setOnAction(e -> dialog.close());

        HBox buttonPanel = new HBox(15, btnOk, btnCancel);
        buttonPanel.setStyle("-fx-alignment: center; -fx-padding: 20;");

        VBox mainLayout = new VBox(15, tabPane, buttonPanel);
        mainLayout.setPadding(new Insets(15));
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");

        dialog.setScene(new Scene(mainLayout));
        dialog.showAndWait();
        return result[0];
    }

    public Employee toEmployee() {
        String normalizedEmploymentType = normalizeEmploymentType(employmentType);
        double computedHourlyRate = hourlyRate > 0.0 ? hourlyRate : deriveHourlyRate(basicSalary, normalizedEmploymentType);
        double grossSemiMonthlyRate = "contract".equals(normalizedEmploymentType)
                ? 0.0
                : (basicSalary / 2.0);

        if ("contract".equals(normalizedEmploymentType)) {
            return new ContractEmployee(
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
                    normalizedEmploymentType,
                    position,
                    "",
                    department,
                    basicSalary,
                    riceSubsidy,
                    phoneAllowance,
                    clothingAllowance,
                    grossSemiMonthlyRate,
                    computedHourlyRate
            );
        }

        return new FullTimeEmployee(
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
                normalizedEmploymentType,
                position,
                "",
                department,
                basicSalary,
                riceSubsidy,
                phoneAllowance,
                clothingAllowance,
                grossSemiMonthlyRate,
                computedHourlyRate
        );
    }

    public static EmployeeFormData from(Employee emp) {
        if (emp == null) {
            return null;
        }
        return new EmployeeFormData(
                emp.getId(),
                emp.getLastName(),
                emp.getFirstName(),
                emp.getBirthdate(),
                emp.getAddress(),
                emp.getPhoneNumber(),
                emp.getSssNo(),
                emp.getPhilhealthNo(),
                emp.getTinNo(),
                emp.getPagibigNo(),
                displayEmploymentType(emp.getEmploymentType()),
                emp.getPosition(),
                emp.getDepartment(),
                emp.getBasicSalary(),
                emp.getRiceSubsidy(),
                emp.getPhoneAllowance(),
                emp.getClothingAllowance(),
                emp.getHourlyRate()
        );
    }

    public static String normalizeEmploymentType(String rawValue) {
        return CONTRACT.equalsIgnoreCase(rawValue) || "contract".equalsIgnoreCase(rawValue) ? "contract" : "fulltime";
    }

    private static String displayEmploymentType(String rawValue) {
        return "contract".equalsIgnoreCase(rawValue) ? CONTRACT : FULL_TIME;
    }

    private static GridPane createPersonalGrid(TextField tfId, TextField tfFirstName, TextField tfLastName, TextField tfBirthdate, TextField tfAddress, TextField tfPhone) {
        GridPane grid = createGrid();
        addFormField(grid, 0, "Employee #", tfId);
        addFormField(grid, 1, "First Name", tfFirstName);
        addFormField(grid, 2, "Last Name", tfLastName);
        addFormField(grid, 3, "Birthdate (YYYY-MM-DD)", tfBirthdate);
        addFormField(grid, 4, "Address", tfAddress);
        addFormField(grid, 5, "Phone Number", tfPhone);
        return grid;
    }

    private static GridPane createGovernmentGrid(TextField tfSss, TextField tfPhilhealth, TextField tfTin, TextField tfPagibig) {
        GridPane grid = createGrid();
        addFormField(grid, 0, "SSS Number", tfSss);
        addFormField(grid, 1, "PhilHealth Number", tfPhilhealth);
        addFormField(grid, 2, "TIN Number", tfTin);
        addFormField(grid, 3, "PAG-IBIG Number", tfPagibig);
        return grid;
    }

    private static GridPane createEmploymentGrid(ComboBox<String> employmentTypeBox, TextField tfPosition, TextField tfDepartment) {
        GridPane grid = createGrid();
        addFormField(grid, 0, "Employment Type", employmentTypeBox);
        addFormField(grid, 1, "Position", tfPosition);
        addFormField(grid, 2, "Department", tfDepartment);
        return grid;
    }

    private static GridPane createCompensationGrid(TextField tfBasicSalary, TextField tfRiceSubsidy, TextField tfPhoneAllowance, TextField tfClothingAllowance, TextField tfHourlyRate) {
        GridPane grid = createGrid();
        addFormField(grid, 0, "Basic Salary", tfBasicSalary);
        addFormField(grid, 1, "Rice Subsidy", tfRiceSubsidy);
        addFormField(grid, 2, "Phone Allowance", tfPhoneAllowance);
        addFormField(grid, 3, "Clothing Allowance", tfClothingAllowance);
        addFormField(grid, 4, "Hourly Rate", tfHourlyRate);
        return grid;
    }

    private static GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8;");
        return grid;
    }

    private static void addFormField(GridPane grid, int row, String labelText, javafx.scene.Node field) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: 600; -fx-text-fill: #1f2937; -fx-font-size: 12;");
        grid.add(label, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static TextField createTextField() {
        TextField field = new TextField();
        field.setStyle("-fx-font-size: 12; -fx-padding: 8;");
        field.setPrefWidth(320);
        return field;
    }

    private static void styleButton(Button btn, String colorHex) {
        btn.setStyle("-fx-padding: 10 30; -fx-font-size: 13; -fx-font-weight: 700; -fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        btn.setMinWidth(120);
    }

    private static List<String> validateForm(
            TextField tfId,
            TextField tfLastName,
            TextField tfFirstName,
            TextField tfBirthdate,
            TextField tfAddress,
            TextField tfPhone,
            TextField tfSss,
            TextField tfPhilhealth,
            TextField tfTin,
            TextField tfPagibig,
            ComboBox<String> employmentTypeBox,
            TextField tfPosition,
            TextField tfDepartment,
            TextField tfBasicSalary,
            TextField tfRiceSubsidy,
            TextField tfPhoneAllowance,
            TextField tfClothingAllowance,
            TextField tfHourlyRate
    ) {
        List<String> errors = new ArrayList<>();
        validateRequired(tfId, "Employee # is required.", errors);
        validateRequired(tfLastName, "Last Name is required.", errors);
        validateRequired(tfFirstName, "First Name is required.", errors);
        validateRequired(tfBirthdate, "Birthdate is required.", errors);
        validateRequired(tfAddress, "Address is required.", errors);
        validateRequired(tfPhone, "Phone Number is required.", errors);
        validateRequired(tfSss, "SSS Number is required.", errors);
        validateRequired(tfPhilhealth, "PhilHealth Number is required.", errors);
        validateRequired(tfTin, "TIN Number is required.", errors);
        validateRequired(tfPagibig, "PAG-IBIG Number is required.", errors);
        validateRequired(tfPosition, "Position is required.", errors);
        validateRequired(tfDepartment, "Department is required.", errors);

        if (employmentTypeBox.getValue() == null || employmentTypeBox.getValue().isBlank()) {
            errors.add("Employment Type is required.");
        }

        validateAmount(tfBasicSalary, "Basic Salary", true, errors);
        validateAmount(tfRiceSubsidy, "Rice Subsidy", false, errors);
        validateAmount(tfPhoneAllowance, "Phone Allowance", false, errors);
        validateAmount(tfClothingAllowance, "Clothing Allowance", false, errors);
        validateAmount(tfHourlyRate, "Hourly Rate", false, errors);

        if (!tfBirthdate.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            errors.add("Birthdate must use YYYY-MM-DD format.");
        }
        if (!tfPhone.getText().trim().matches("[0-9+\\-() ]{7,20}")) {
            errors.add("Phone Number contains invalid characters.");
        }
        if (!tfSss.getText().trim().matches("[0-9-]{8,20}")) {
            errors.add("SSS Number must contain digits and dashes only.");
        }
        if (!tfPhilhealth.getText().trim().matches("[0-9-]{8,20}")) {
            errors.add("PhilHealth Number must contain digits and dashes only.");
        }
        if (!tfTin.getText().trim().matches("[0-9-]{8,20}")) {
            errors.add("TIN Number must contain digits and dashes only.");
        }
        if (!tfPagibig.getText().trim().matches("[0-9-]{8,20}")) {
            errors.add("PAG-IBIG Number must contain digits and dashes only.");
        }

        String normalizedEmploymentType = normalizeEmploymentType(employmentTypeBox.getValue());
        if ("contract".equals(normalizedEmploymentType) && parseAmount(tfHourlyRate.getText().trim()) <= 0.0) {
            errors.add("Hourly Rate is required for contract employees.");
        }

        return errors;
    }

    private static void validateRequired(TextField field, String message, List<String> errors) {
        if (field.getText().trim().isEmpty()) {
            errors.add(message);
        }
    }

    private static void validateAmount(TextField field, String label, boolean required, List<String> errors) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            if (required) {
                errors.add(label + " is required.");
            }
            return;
        }
        try {
            if (Double.parseDouble(value.replace(",", "")) < 0.0) {
                errors.add(label + " cannot be negative.");
            }
        } catch (NumberFormatException ex) {
            errors.add(label + " must be a valid number.");
        }
    }

    private static double parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        return Double.parseDouble(value.replace(",", "").trim());
    }

    private static double deriveHourlyRate(double basicSalary, String employmentType) {
        if ("contract".equals(employmentType)) {
            return 0.0;
        }
        return basicSalary <= 0.0 ? 0.0 : basicSalary / 168.0;
    }

    private static void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
