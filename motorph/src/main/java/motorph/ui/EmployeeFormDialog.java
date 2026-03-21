package motorph.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import motorph.model.EmployeeFormData;
import motorph.service.ValidationService;
import motorph.util.InputValidators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class EmployeeFormDialog {

    private EmployeeFormDialog() {
    }

    public static EmployeeFormData show(Window owner, String title, EmployeeFormData initialData, ValidationService validationService) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setMinWidth(760);
        dialog.setMinHeight(720);

        TextField idField = createTextField(initialData == null ? "" : initialData.id(), "Enter employee number");
        TextField firstNameField = createTextField(initialData == null ? "" : initialData.firstName(), "Enter first name");
        TextField lastNameField = createTextField(initialData == null ? "" : initialData.lastName(), "Enter last name");
        TextField birthdateField = createTextField(initialData == null ? "" : initialData.birthdate(), "MM/DD/YYYY");
        TextField addressField = createTextField(initialData == null ? "" : initialData.address(), "Enter complete address");
        TextField phoneField = createTextField(initialData == null ? "" : initialData.phoneNumber(), "09XX-XXX-XXXX or 09123456789");
        TextField sssField = createTextField(initialData == null ? "" : initialData.sssNumber(), "XX-XXXXXXX-X");
        TextField philhealthField = createTextField(initialData == null ? "" : initialData.philhealthNumber(), "12 digits");
        TextField tinField = createTextField(initialData == null ? "" : initialData.tinNumber(), "XXX-XXX-XXX-XXX");
        TextField pagibigField = createTextField(initialData == null ? "" : initialData.pagibigNumber(), "12 digits");
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(EmployeeFormData.FULL_TIME, EmployeeFormData.CONTRACT));
        typeBox.setValue(initialData == null ? EmployeeFormData.FULL_TIME : initialData.employmentType());
        typeBox.getStyleClass().add("app-combo-box");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.setTooltip(new Tooltip("Select the employee's engagement type."));
        TextField positionField = createTextField(initialData == null ? "" : initialData.position(), "Enter job title");
        TextField departmentField = createTextField(initialData == null ? "" : initialData.department(), "Enter department name");
        TextField basicSalaryField = createMoneyField(initialData == null ? "" : formatMoney(initialData.basicSalary()), "Enter amount");
        TextField riceField = createMoneyField(initialData == null ? "" : formatMoney(initialData.riceSubsidy()), "Enter amount");
        TextField phoneAllowanceField = createMoneyField(initialData == null ? "" : formatMoney(initialData.phoneAllowance()), "Enter amount");
        TextField clothingField = createMoneyField(initialData == null ? "" : formatMoney(initialData.clothingAllowance()), "Enter amount");
        TextField grossSemiMonthlyField = createReadOnlyField(initialData == null ? "" : formatMoney(initialData.grossSemiMonthlyRate()));
        TextField hourlyRateField = createMoneyField(initialData == null ? "" : formatMoney(initialData.hourlyRate()), "Enter amount");

        applyTextFilter(idField, change -> change.getControlNewText().matches("[0-9]*") ? change : null);
        applyTextFilter(birthdateField, change -> change.getControlNewText().matches("[0-9/]*") ? change : null);
        applyTextFilter(phoneField, change -> change.getControlNewText().matches("[0-9-]*") ? change : null);
        applyTextFilter(sssField, change -> change.getControlNewText().matches("[0-9-]*") ? change : null);
        applyTextFilter(philhealthField, change -> change.getControlNewText().matches("[0-9]*") ? change : null);
        applyTextFilter(tinField, change -> change.getControlNewText().matches("[0-9-]*") ? change : null);
        applyTextFilter(pagibigField, change -> change.getControlNewText().matches("[0-9]*") ? change : null);

        Label summaryLabel = new Label();
        summaryLabel.getStyleClass().addAll("form-error-summary", "status-message", "status-error");
        summaryLabel.setWrapText(true);
        summaryLabel.setVisible(false);
        summaryLabel.setManaged(false);

        Map<String, FieldBinding> bindings = new LinkedHashMap<>();

        VBox personalSection = buildSectionCard(
                "Personal Information",
                "Complete the employee's identity and contact details before proceeding to government and compensation data.",
                bindings,
                buildFieldCell(bindings, "Employee #", idField, "Required. Use the official employee number.", true,
                        () -> validateEmployeeId(idField.getText())),
                buildFieldCell(bindings, "First Name", firstNameField, "Letters, spaces, and hyphen are allowed.", true,
                        () -> validateName(firstNameField.getText(), "First name")),
                buildFieldCell(bindings, "Last Name", lastNameField, "Letters, spaces, and hyphen are allowed.", true,
                        () -> validateName(lastNameField.getText(), "Last name")),
                buildFieldCell(bindings, "Birthday", birthdateField, "Example: 10/17/1983", true,
                        () -> validateBirthday(birthdateField.getText())),
                buildFieldCell(bindings, "Address", addressField, "Enter the complete current address.", false,
                        () -> null),
                buildFieldCell(bindings, "Phone Number", phoneField, "Example: 0917-123-4567", false,
                        () -> validatePhone(phoneField.getText()))
        );

        VBox governmentSection = buildSectionCard(
                "Government Information",
                "Use standard MotorPH formatting for government identifiers to reduce encoding corrections later.",
                bindings,
                buildFieldCell(bindings, "SSS Number", sssField, "Example: 44-4506057-3", false,
                        () -> validateSss(sssField.getText())),
                buildFieldCell(bindings, "PhilHealth Number", philhealthField, "Example: 123456789012", false,
                        () -> validatePhilhealth(philhealthField.getText())),
                buildFieldCell(bindings, "TIN Number", tinField, "Example: 123-456-789-000", false,
                        () -> validateTin(tinField.getText())),
                buildFieldCell(bindings, "Pag-IBIG Number", pagibigField, "Example: 123456789012", false,
                        () -> validatePagibig(pagibigField.getText()))
        );

        VBox employmentSection = buildSectionCard(
                "Employment Information",
                "Assign the employee to the correct engagement type, position, and department for payroll grouping.",
                bindings,
                buildFieldCell(bindings, "Employment Type", typeBox, "Choose Full Time or Contract.", true,
                        () -> typeBox.getValue() == null ? "Select an employment type." : null),
                buildFieldCell(bindings, "Position", positionField, "Example: HR Manager or Software Developer", false,
                        () -> null),
                buildFieldCell(bindings, "Department", departmentField, "Example: Human Resources or IT", false,
                        () -> null)
        );

        Runnable updateGrossPreview = () -> grossSemiMonthlyField.setText(formatMoney(deriveGrossSemiMonthlyRate(basicSalaryField.getText(), typeBox.getValue())));
        basicSalaryField.textProperty().addListener((obs, oldValue, newValue) -> updateGrossPreview.run());
        typeBox.valueProperty().addListener((obs, oldValue, newValue) -> updateGrossPreview.run());
        updateGrossPreview.run();

        VBox compensationSection = buildSectionCard(
                "Compensation Information",
                "Enter monthly and allowance amounts carefully. The semi-monthly rate is shown as a derived preview.",
                bindings,
                buildFieldCell(bindings, "Basic Salary", basicSalaryField, "Example: 45000.00", false,
                        () -> validateMoney(basicSalaryField.getText(), "Basic salary")),
                buildFieldCell(bindings, "Rice Subsidy", riceField, "Example: 1500.00", false,
                        () -> validateMoney(riceField.getText(), "Rice subsidy")),
                buildFieldCell(bindings, "Phone Allowance", phoneAllowanceField, "Example: 1000.00", false,
                        () -> validateMoney(phoneAllowanceField.getText(), "Phone allowance")),
                buildFieldCell(bindings, "Clothing Allowance", clothingField, "Example: 1000.00", false,
                        () -> validateMoney(clothingField.getText(), "Clothing allowance")),
                buildFieldCell(bindings, "Gross Semi-Monthly Rate", grossSemiMonthlyField, "Example: 22500.00", false,
                        () -> null),
                buildFieldCell(bindings, "Hourly Rate", hourlyRateField, "Example: 535.71", false,
                        () -> validateMoney(hourlyRateField.getText(), "Hourly rate"))
        );

        VBox formContent = new VBox(18, personalSection, governmentSection, employmentSection, compensationSection);
        formContent.getStyleClass().add("form-content");

        ScrollPane scrollPane = new ScrollPane(formContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("form-scroll-pane");

        bindings.values().forEach(binding -> bindRealtimeValidation(binding));

        Button resetButton = new Button("Reset");
        resetButton.getStyleClass().addAll("app-button", "ghost-button");
        resetButton.setOnAction(event -> {
            idField.setText("");
            firstNameField.setText("");
            lastNameField.setText("");
            birthdateField.setText("");
            addressField.setText("");
            phoneField.setText("");
            sssField.setText("");
            philhealthField.setText("");
            tinField.setText("");
            pagibigField.setText("");
            typeBox.setValue(EmployeeFormData.FULL_TIME);
            positionField.setText("");
            departmentField.setText("");
            basicSalaryField.setText("");
            riceField.setText("");
            phoneAllowanceField.setText("");
            clothingField.setText("");
            hourlyRateField.setText("");
            updateGrossPreview.run();
            clearSummary(summaryLabel);
            bindings.values().forEach(EmployeeFormDialog::clearFieldError);
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("app-button", "secondary-button");
        cancelButton.setOnAction(event -> dialog.close());

        Button saveButton = new Button("Save Employee");
        saveButton.getStyleClass().addAll("app-button", "primary-button");

        final EmployeeFormData[] result = {null};
        saveButton.setOnAction(event -> {
            clearSummary(summaryLabel);
            Map<String, String> localErrors = validateBindings(bindings);
            if (!localErrors.isEmpty()) {
                showSummary(summaryLabel, localErrors);
                return;
            }

            try {
                EmployeeFormData formData = new EmployeeFormData(
                        idField.getText().trim(),
                        lastNameField.getText().trim(),
                        firstNameField.getText().trim(),
                        birthdateField.getText().trim(),
                        addressField.getText().trim(),
                        phoneField.getText().trim(),
                        sssField.getText().trim(),
                        philhealthField.getText().trim(),
                        tinField.getText().trim(),
                        pagibigField.getText().trim(),
                        typeBox.getValue(),
                        positionField.getText().trim(),
                        departmentField.getText().trim(),
                        parseAmount(basicSalaryField.getText()),
                        parseAmount(riceField.getText()),
                        parseAmount(phoneAllowanceField.getText()),
                        parseAmount(clothingField.getText()),
                        parseAmount(hourlyRateField.getText())
                );

                List<String> serviceErrors = validationService.validateEmployee(formData, List.of(), initialData != null);
                if (!serviceErrors.isEmpty()) {
                    showSummary(summaryLabel, serviceErrors);
                    return;
                }

                result[0] = formData;
                dialog.close();
            } catch (NumberFormatException exception) {
                showSummary(summaryLabel, List.of("Salary and allowance fields must be valid non-negative numbers."));
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, resetButton, spacer, cancelButton, saveButton);
        actions.getStyleClass().add("dialog-actions");

        Label formTitle = new Label(title);
        formTitle.getStyleClass().add("card-title");
        Label formSubtitle = new Label("Fill out the employee record using the guided format below. Required fields are marked clearly and validated beside each field.");
        formSubtitle.getStyleClass().add("card-subtitle");
        formSubtitle.setWrapText(true);

        VBox shell = new VBox(16, new VBox(4, formTitle, formSubtitle), summaryLabel, scrollPane, actions);
        shell.getStyleClass().addAll("panel-card", "dialog-shell");
        shell.setPadding(new Insets(24));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        StackPane root = new StackPane(shell);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(18));

        Scene scene = new Scene(root, 840, 780);
        String stylesheet = EmployeeFormDialog.class.getResource("/styles.css") == null ? null : EmployeeFormDialog.class.getResource("/styles.css").toExternalForm();
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet);
        }
        dialog.setScene(scene);
        dialog.showAndWait();
        return result[0];
    }

    private static VBox buildSectionCard(String title, String subtitle, Map<String, FieldBinding> bindings, FormField... fields) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("form-section-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("form-section-subtitle");
        subtitleLabel.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(16);

        for (int index = 0; index < fields.length; index++) {
            FormField field = fields[index];
            int column = index % 2;
            int row = index / 2;
            grid.add(field.container(), column, row);
            GridPane.setHgrow(field.container(), Priority.ALWAYS);
        }

        VBox section = new VBox(14, new VBox(4, titleLabel, subtitleLabel), grid);
        section.getStyleClass().add("form-section-card");
        section.setPadding(new Insets(18));
        return section;
    }

    private static FormField buildFieldCell(Map<String, FieldBinding> bindings, String labelText, Control control, String helperText,
                                            boolean required, Supplier<String> validator) {
        Label label = new Label(required ? labelText + " *" : labelText);
        label.getStyleClass().add("field-label");

        Label helper = new Label(helperText);
        helper.getStyleClass().add("field-helper-text");
        helper.setWrapText(true);

        Label error = new Label();
        error.getStyleClass().add("field-error-text");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        VBox box = new VBox(6, label, control, helper, error);
        box.getStyleClass().add("form-field-block");
        box.setFillWidth(true);
        VBox.setVgrow(control, Priority.NEVER);

        bindings.put(labelText, new FieldBinding(labelText, control, error, validator));
        return new FormField(box);
    }

    private static TextField createTextField(String value, String promptText) {
        TextField field = new TextField(value == null ? "" : value);
        field.setPromptText(promptText);
        field.getStyleClass().add("app-text-field");
        field.setMaxWidth(Double.MAX_VALUE);
        field.setTooltip(new Tooltip(promptText));
        return field;
    }

    private static TextField createMoneyField(String value, String promptText) {
        TextField field = createTextField(value, promptText);
        applyTextFilter(field, change -> change.getControlNewText().matches("\\d*(\\.\\d{0,2})?") ? change : null);
        return field;
    }

    private static TextField createReadOnlyField(String value) {
        TextField field = createTextField(value, "Calculated automatically");
        field.setEditable(false);
        field.setFocusTraversable(false);
        field.getStyleClass().add("readonly-field");
        return field;
    }

    private static void applyTextFilter(TextField field, UnaryOperator<TextFormatter.Change> filter) {
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private static void bindRealtimeValidation(FieldBinding binding) {
        if (binding.control() instanceof TextField textField) {
            textField.textProperty().addListener((obs, oldValue, newValue) -> validateBinding(binding));
        } else if (binding.control() instanceof ComboBox<?> comboBox) {
            comboBox.valueProperty().addListener((obs, oldValue, newValue) -> validateBinding(binding));
        }
    }

    private static Map<String, String> validateBindings(Map<String, FieldBinding> bindings) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldBinding binding : bindings.values()) {
            String error = validateBinding(binding);
            if (error != null) {
                errors.put(binding.label(), error);
            }
        }
        return errors;
    }

    private static String validateBinding(FieldBinding binding) {
        String error = binding.validator().get();
        if (error == null || error.isBlank()) {
            clearFieldError(binding);
            return null;
        }
        setFieldError(binding, error);
        return error;
    }

    private static void setFieldError(FieldBinding binding, String message) {
        binding.errorLabel().setText(message);
        binding.errorLabel().setVisible(true);
        binding.errorLabel().setManaged(true);
        if (!binding.control().getStyleClass().contains("invalid-field")) {
            binding.control().getStyleClass().add("invalid-field");
        }
    }

    private static void clearFieldError(FieldBinding binding) {
        binding.errorLabel().setText("");
        binding.errorLabel().setVisible(false);
        binding.errorLabel().setManaged(false);
        binding.control().getStyleClass().remove("invalid-field");
    }

    private static void showSummary(Label summaryLabel, Map<String, String> errors) {
        List<String> lines = new ArrayList<>();
        lines.add("Please correct the highlighted fields:");
        errors.forEach((field, message) -> lines.add("- " + field + ": " + message));
        showSummary(summaryLabel, lines);
    }

    private static void showSummary(Label summaryLabel, List<String> messages) {
        summaryLabel.setText(String.join("\n", messages));
        summaryLabel.setVisible(true);
        summaryLabel.setManaged(true);
    }

    private static void clearSummary(Label summaryLabel) {
        summaryLabel.setText("");
        summaryLabel.setVisible(false);
        summaryLabel.setManaged(false);
    }

    private static String validateEmployeeId(String value) {
        if (InputValidators.isBlank(value)) {
            return "Employee number is required.";
        }
        return value.trim().matches("\\d+") ? null : "Use digits only for the employee number.";
    }

    private static String validateName(String value, String fieldName) {
        if (InputValidators.isBlank(value)) {
            return fieldName + " is required.";
        }
        return InputValidators.isValidName(value) ? null : "Use letters, spaces, apostrophe, or hyphen only.";
    }

    private static String validateBirthday(String value) {
        if (InputValidators.isBlank(value)) {
            return "Birthday is required.";
        }
        return InputValidators.isValidBirthday(value) ? null : "Use MM/DD/YYYY format.";
    }

    private static String validatePhone(String value) {
        if (InputValidators.isBlank(value)) {
            return null;
        }
        return InputValidators.isValidPhone(value) ? null : "Use 09XX-XXX-XXXX or 11-digit mobile number.";
    }

    private static String validateSss(String value) {
        if (InputValidators.isBlank(value)) {
            return null;
        }
        return InputValidators.isValidSssNumber(value) ? null : "Use XX-XXXXXXX-X format.";
    }

    private static String validatePhilhealth(String value) {
        if (InputValidators.isBlank(value)) {
            return null;
        }
        return InputValidators.isValidPhilhealthNumber(value) ? null : "Use a 12-digit PhilHealth number.";
    }

    private static String validateTin(String value) {
        if (InputValidators.isBlank(value)) {
            return null;
        }
        return InputValidators.isValidTinNumber(value) ? null : "Use XXX-XXX-XXX-XXX format.";
    }

    private static String validatePagibig(String value) {
        if (InputValidators.isBlank(value)) {
            return null;
        }
        return InputValidators.isValidPagibigNumber(value) ? null : "Use a 12-digit Pag-IBIG number.";
    }

    private static String validateMoney(String value, String fieldName) {
        if (InputValidators.isBlank(value)) {
            return null;
        }
        return InputValidators.isValidMoney(value) ? null : fieldName + " must be a valid non-negative amount.";
    }

    private static String formatMoney(double value) {
        return value <= 0.0 ? "" : String.format("%.2f", value);
    }

    private static double deriveGrossSemiMonthlyRate(String basicSalaryText, String employmentType) {
        double basicSalary = parseAmount(basicSalaryText);
        return EmployeeFormData.CONTRACT.equalsIgnoreCase(employmentType) ? 0.0 : basicSalary / 2.0;
    }

    private static double parseAmount(String value) {
        String normalized = value == null ? "" : value.replace(",", "").trim();
        return normalized.isEmpty() ? 0.0 : Double.parseDouble(normalized);
    }

    private record FormField(VBox container) {
    }

    private record FieldBinding(String label, Control control, Label errorLabel, Supplier<String> validator) {
    }
}
