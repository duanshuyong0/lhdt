package lhdt.admin.svc.cityplanning.persistence;

import lhdt.admin.svc.cityplanning.domain.CPReportDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CPReportDetailRepository extends JpaRepository<CPReportDetail, Long> {
}