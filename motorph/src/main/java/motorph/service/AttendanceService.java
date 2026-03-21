package motorph.service;

import motorph.model.AttendanceRecord;
import motorph.model.Employee;
import motorph.repository.AttendanceRepository;
import motorph.repository.EmployeeRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.IntStream;

public class AttendanceService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final ValidationService validationService;

    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository, ValidationService validationService) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.validationService = validationService;
    }

    public List<AttendanceRecord> getAllRecords() {
        return new ArrayList<>(attendanceRepository.findAll());
    }

    public List<AttendanceRecord> getRecordsForEmployee(String employeeId) {
        return getAllRecords().stream()
                .filter(record -> record.employeeId().equals(employeeId))
                .sorted(Comparator.comparing(AttendanceRecord::date, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public double calculateTotalHoursForEmployee(String employeeId, int month, int year) {
        return getAttendanceByEmployeeMonthYear(employeeId, month, year).stream()
                .mapToDouble(AttendanceRecord::hoursWorked)
                .sum();
    }

    public List<AttendanceRecord> getAttendanceByEmployeeMonthYear(String employeeId, int month, int year) {
        return getRecordsForEmployee(employeeId).stream()
                .filter(record -> record.date() != null && record.monthValue() == month && record.yearValue() == year)
                .toList();
    }

    public List<AttendanceRecord> filterAttendanceRecords(String employeeId, Integer month, Integer year) {
        return (employeeId == null || employeeId.isBlank() ? getAllRecords() : getRecordsForEmployee(employeeId)).stream()
                .filter(record -> month == null || record.monthValue() == month)
                .filter(record -> year == null || record.yearValue() == year)
                .toList();
    }

    public List<Integer> getAvailableAttendanceYears() {
        NavigableSet<Integer> years = new TreeSet<>();
        getAllRecords().stream()
                .map(AttendanceRecord::date)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .forEach(years::add);
        return years.descendingSet().stream().toList();
    }

    public List<Integer> getAvailableAttendanceYearsForEmployee(String employeeId) {
        NavigableSet<Integer> years = new TreeSet<>();
        getRecordsForEmployee(employeeId).stream()
                .map(AttendanceRecord::date)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .forEach(years::add);
        return years.descendingSet().stream().toList();
    }

    public List<Integer> getPayrollEligibleYears(String employeeId) {
        NavigableSet<Integer> years = new TreeSet<>();
        years.addAll(getAvailableAttendanceYears());
        if (employeeId != null && !employeeId.isBlank()) {
            years.addAll(getAvailableAttendanceYearsForEmployee(employeeId));
        }
        years.add(LocalDate.now().getYear());
        if (years.isEmpty()) {
            return List.of(LocalDate.now().getYear());
        }
        int minYear = years.first();
        int maxYear = years.last();
        return IntStream.rangeClosed(minYear, maxYear)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    public List<String> clockIn(Employee employee, LocalDate date, LocalTime time) {
        List<AttendanceRecord> records = getAllRecords();
        boolean existingOpenRecord = records.stream().anyMatch(record ->
                record.belongsTo(employee.getId())
                        && date.equals(record.date())
                        && record.hasOpenLog());
        if (existingOpenRecord) {
            return List.of("There is already an open clock in record for this employee and date.");
        }
        AttendanceRecord newRecord = new AttendanceRecord(
                employee.getId(),
                employee.getLastName(),
                employee.getFirstName(),
                DATE_FORMAT.format(date),
                TIME_FORMAT.format(time),
                "",
                date,
                time,
                null);
        List<String> errors = validationService.validateAttendance(newRecord, employeeRepository.findAll());
        if (!errors.isEmpty()) {
            return errors;
        }
        records.add(newRecord);
        attendanceRepository.saveAll(records);
        return List.of();
    }

    public List<String> clockOut(Employee employee, LocalDate date, LocalTime time) {
        List<AttendanceRecord> records = getAllRecords();
        Optional<AttendanceRecord> existingRecord = records.stream()
                .filter(record -> record.belongsTo(employee.getId()) && date.equals(record.date()) && record.hasOpenLog())
                .findFirst();
        if (existingRecord.isEmpty()) {
            return List.of("No open clock in record found for the selected date.");
        }
        AttendanceRecord updatedRecord = existingRecord.get().withLogOut(time, TIME_FORMAT.format(time));
        List<String> errors = validationService.validateAttendance(updatedRecord, employeeRepository.findAll());
        if (!errors.isEmpty()) {
            return errors;
        }
        int index = records.indexOf(existingRecord.get());
        records.set(index, updatedRecord);
        attendanceRepository.saveAll(records);
        return List.of();
    }
}
