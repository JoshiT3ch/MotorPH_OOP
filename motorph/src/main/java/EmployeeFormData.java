import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;


public class EmployeeFormData {
    public String id;
    public String lastName;
    public String firstName;
    public String birthdate;
    public String address;
    public String phoneNumber;
    public String sssNumber;
    public String philhealthNumber;
    public String pagibigNumber;
    public String position;
    public String department;
    public double basicSalary;
    public double riceSubsidy;
    public double phoneAllowance;
    public double clothingAllowance;
    public double hourlyRate;

    public EmployeeFormData(String id, String lastName, String firstName, String birthdate, String address,
                           String phoneNumber, String sssNumber, String philhealthNumber, String pagibigNumber,
                           String position, String department, double basicSalary, double riceSubsidy,
                           double phoneAllowance, double clothingAllowance, double hourlyRate) {
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
        this.birthdate = birthdate;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.sssNumber = sssNumber;
        this.philhealthNumber = philhealthNumber;
        this.pagibigNumber = pagibigNumber;
        this.position = position;
        this.department = department;
        this.basicSalary = basicSalary;
        this.riceSubsidy = riceSubsidy;
        this.phoneAllowance = phoneAllowance;
        this.clothingAllowance = clothingAllowance;
        this.hourlyRate = hourlyRate;
    }

    public static EmployeeFormData show(Window owner, String title, EmployeeFormData initialData) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setWidth(550);
        dialog.setHeight(550);
        dialog.setResizable(false);

        // Create form fields
        TextField tfId = new TextField();
        TextField tfLastName = new TextField();
        TextField tfFirstName = new TextField();
        TextField tfBirthdate = new TextField();
        TextField tfAddress = new TextField();
        TextField tfPhone = new TextField();
        TextField tfSss = new TextField();
        TextField tfPhilhealth = new TextField();
        TextField tfPagibig = new TextField();
        TextField tfPosition = new TextField();
        TextField tfDepartment = new TextField();
        TextField tfBasicSalary = new TextField();
        TextField tfRiceSubsidy = new TextField();
        TextField tfPhoneAllowance = new TextField();
        TextField tfClothingAllowance = new TextField();
        TextField tfHourlyRate = new TextField();

        // Pre-fill if editing
        if (initialData != null) {
            tfId.setText(initialData.id != null ? initialData.id : "");
            tfLastName.setText(initialData.lastName != null ? initialData.lastName : "");
            tfFirstName.setText(initialData.firstName != null ? initialData.firstName : "");
            tfBirthdate.setText(initialData.birthdate != null ? initialData.birthdate : "");
            tfAddress.setText(initialData.address != null ? initialData.address : "");
            tfPhone.setText(initialData.phoneNumber != null ? initialData.phoneNumber : "");
            tfSss.setText(initialData.sssNumber != null ? initialData.sssNumber : "");
            tfPhilhealth.setText(initialData.philhealthNumber != null ? initialData.philhealthNumber : "");
            tfPagibig.setText(initialData.pagibigNumber != null ? initialData.pagibigNumber : "");
            tfPosition.setText(initialData.position != null ? initialData.position : "");
            tfDepartment.setText(initialData.department != null ? initialData.department : "");
            tfBasicSalary.setText(String.valueOf(initialData.basicSalary));
            tfRiceSubsidy.setText(String.valueOf(initialData.riceSubsidy));
            tfPhoneAllowance.setText(String.valueOf(initialData.phoneAllowance));
            tfClothingAllowance.setText(String.valueOf(initialData.clothingAllowance));
            tfHourlyRate.setText(String.valueOf(initialData.hourlyRate));
            tfId.setDisable(true);
        }

        // Style text fields
        for (TextField tf : new TextField[]{tfId, tfLastName, tfFirstName, tfBirthdate, tfAddress, tfPhone, tfSss, tfPhilhealth, tfPagibig, tfPosition, tfDepartment, tfBasicSalary, tfRiceSubsidy, tfPhoneAllowance, tfClothingAllowance, tfHourlyRate}) {
            tf.setStyle("-fx-font-size: 12; -fx-padding: 8;");
            tf.setPrefWidth(320);
        }

        // Create TabPane with organized sections
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-font-size: 12;");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Personal Information
        Tab tabPersonal = new Tab("Personal Info", createPersonalGrid(tfId, tfFirstName, tfLastName, tfBirthdate, tfAddress, tfPhone));
        tabPersonal.setStyle("-fx-padding: 15;");

        // Tab 2: Government IDs
        Tab tabGovernment = new Tab("Government IDs", createGovernmentGrid(tfSss, tfPhilhealth, tfPagibig));
        tabGovernment.setStyle("-fx-padding: 15;");

        // Tab 3: Employment
        Tab tabEmployment = new Tab("Employment", createEmploymentGrid(tfPosition, tfDepartment));
        tabEmployment.setStyle("-fx-padding: 15;");

        // Tab 4: Compensation
        Tab tabCompensation = new Tab("Compensation", createCompensationGrid(tfBasicSalary, tfRiceSubsidy, tfPhoneAllowance, tfClothingAllowance, tfHourlyRate));
        tabCompensation.setStyle("-fx-padding: 15;");

        tabPane.getTabs().addAll(tabPersonal, tabGovernment, tabEmployment, tabCompensation);

        // Create buttons
        Button btnOk = new Button("Save");
        Button btnCancel = new Button("Cancel");
        styleButton(btnOk, "#2563eb");
        styleButton(btnCancel, "#ef4444");

        final EmployeeFormData[] result = {null};

        btnOk.setOnAction(e -> {
            if (tfId.getText().trim().isEmpty() || tfLastName.getText().trim().isEmpty() || tfFirstName.getText().trim().isEmpty()) {
                showErrorDialog("Validation Error", "Employee #, Last Name, and First Name are required.");
                return;
            }
            try {
                result[0] = new EmployeeFormData(
                        tfId.getText().trim(),
                        tfLastName.getText().trim(),
                        tfFirstName.getText().trim(),
                        tfBirthdate.getText().trim(),
                        tfAddress.getText().trim(),
                        tfPhone.getText().trim(),
                        tfSss.getText().trim(),
                        tfPhilhealth.getText().trim(),
                        tfPagibig.getText().trim(),
                        tfPosition.getText().trim(),
                        tfDepartment.getText().trim(),
                        parseDouble(tfBasicSalary.getText()),
                        parseDouble(tfRiceSubsidy.getText()),
                        parseDouble(tfPhoneAllowance.getText()),
                        parseDouble(tfClothingAllowance.getText()),
                        parseDouble(tfHourlyRate.getText())
                );
                dialog.close();
            } catch (NumberFormatException ex) {
                showErrorDialog("Validation Error", "Salary fields must contain valid numbers.");
            }
        });

        btnCancel.setOnAction(e -> dialog.close());

        HBox buttonPanel = new HBox(15);
        buttonPanel.setStyle("-fx-alignment: center; -fx-padding: 20;");
        buttonPanel.getChildren().addAll(btnOk, btnCancel);

        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(15));
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");
        mainLayout.getChildren().addAll(tabPane, buttonPanel);

        Scene scene = new Scene(mainLayout);
        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }

    private static GridPane createPersonalGrid(TextField tfId, TextField tfFirstName, TextField tfLastName, TextField tfBirthdate, TextField tfAddress, TextField tfPhone) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8;");

        addFormField(grid, 0, "Employee #", tfId);
        addFormField(grid, 1, "First Name", tfFirstName);
        addFormField(grid, 2, "Last Name", tfLastName);
        addFormField(grid, 3, "Date of Birth", tfBirthdate);
        addFormField(grid, 4, "Address", tfAddress);
        addFormField(grid, 5, "Phone Number", tfPhone);

        return grid;
    }

    private static GridPane createGovernmentGrid(TextField tfSss, TextField tfPhilhealth, TextField tfPagibig) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8;");

        addFormField(grid, 0, "SSS Number", tfSss);
        addFormField(grid, 1, "PhilHealth Number", tfPhilhealth);
        addFormField(grid, 2, "PAG-IBIG Number", tfPagibig);

        return grid;
    }

    private static GridPane createEmploymentGrid(TextField tfPosition, TextField tfDepartment) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8;");

        addFormField(grid, 0, "Position", tfPosition);
        addFormField(grid, 1, "Department", tfDepartment);

        return grid;
    }

    private static GridPane createCompensationGrid(TextField tfBasicSalary, TextField tfRiceSubsidy, TextField tfPhoneAllowance, TextField tfClothingAllowance, TextField tfHourlyRate) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8;");

        addFormField(grid, 0, "Basic Salary", tfBasicSalary);
        addFormField(grid, 1, "Rice Subsidy", tfRiceSubsidy);
        addFormField(grid, 2, "Phone Allowance", tfPhoneAllowance);
        addFormField(grid, 3, "Clothing Allowance", tfClothingAllowance);
        addFormField(grid, 4, "Hourly Rate", tfHourlyRate);

        return grid;
    }

    private static void addFormField(GridPane grid, int row, String labelText, TextField textField) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: 600; -fx-text-fill: #1f2937; -fx-font-size: 12;");
        grid.add(label, 0, row);
        grid.add(textField, 1, row);
        GridPane.setHgrow(textField, javafx.scene.layout.Priority.ALWAYS);
    }

    private static void styleButton(Button btn, String colorHex) {
        btn.setStyle("-fx-padding: 10 30; -fx-font-size: 13; -fx-font-weight: 700; -fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        btn.setMinWidth(120);
    }

    private static double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Employee toEmployee() {
        // by default create a full-time employee; other forms can be added later
        return new FullTimeEmployee(
                this.id,
                this.lastName,
                this.firstName,
                this.birthdate,
                this.address,
                this.phoneNumber,
                this.sssNumber,
                this.philhealthNumber,
                "", // TIN Number
                this.pagibigNumber,
                "fulltime", // Status placeholder
                this.position,
                "", // Supervisor
                this.department,
                this.basicSalary,
                this.riceSubsidy,
                this.phoneAllowance,
                this.clothingAllowance,
                this.basicSalary / 2.0,
                this.hourlyRate
        );
    }

    public static EmployeeFormData from(Employee emp) {
        if (emp == null) return null;
        return new EmployeeFormData(
                emp.getId(),
                emp.getLastName(),
                emp.getFirstName(),
                emp.getBirthdate(),
                emp.getAddress(),
                emp.getPhoneNumber(),
                emp.getSssNo(),
                emp.getPhilhealthNo(),
                emp.getPagibigNo(),
                emp.getPosition(),
                emp.getDepartment(),
                emp.getBasicSalary(),
                emp.getRiceSubsidy(),
                emp.getPhoneAllowance(),
                emp.getClothingAllowance(),
                emp.getHourlyRate()
        );
    }
}
