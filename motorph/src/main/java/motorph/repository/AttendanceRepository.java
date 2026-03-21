package motorph.repository;

import motorph.model.AttendanceRecord;

import java.util.List;

public interface AttendanceRepository {
    List<AttendanceRecord> findAll();
    void saveAll(List<AttendanceRecord> records);
}
