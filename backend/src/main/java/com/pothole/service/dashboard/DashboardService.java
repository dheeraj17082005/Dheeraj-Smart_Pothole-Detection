package com.pothole.service.dashboard;

import com.pothole.dto.dashboard.DashboardStatsResponse;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.repository.PotholeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final PotholeRepository potholeRepository;

    public DashboardService(PotholeRepository potholeRepository) {
        this.potholeRepository = potholeRepository;
    }

    public DashboardStatsResponse getDashboardStats() {
        long total = potholeRepository.count();
        long reported = potholeRepository.countByStatus(PotholeStatus.REPORTED);
        long acknowledged = potholeRepository.countByStatus(PotholeStatus.ACKNOWLEDGED);
        long inProgress = potholeRepository.countByStatus(PotholeStatus.IN_PROGRESS);
        long resolved = potholeRepository.countByStatus(PotholeStatus.RESOLVED);
        long highSeverity = potholeRepository.countBySeverityClass(SeverityClass.HIGH);

        return new DashboardStatsResponse(
                total,
                reported,
                acknowledged,
                inProgress,
                resolved,
                highSeverity
        );
    }
}
