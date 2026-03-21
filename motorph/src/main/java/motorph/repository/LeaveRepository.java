package motorph.repository;

import motorph.model.LeaveRequest;

import java.util.List;

public interface LeaveRepository {
    List<LeaveRequest> findAll();
    void saveAll(List<LeaveRequest> requests);
}
