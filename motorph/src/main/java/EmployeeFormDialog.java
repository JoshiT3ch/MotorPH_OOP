public final class EmployeeFormDialog {

    private EmployeeFormDialog() {
    }

    public static EmployeeFormData show(javafx.stage.Stage owner, String title, EmployeeFormData initialData) {
        return EmployeeFormData.show(owner, title, initialData);
    }
}
