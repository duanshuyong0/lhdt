package lhdt.admin.svc.cityplanning.persistence;

import lhdt.admin.svc.cityplanning.domain.CPLocalInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CPLocalInfoRepository extends JpaRepository<CPLocalInfo, Long> {
    CPLocalInfo findByLocalName(String localName);
    CPLocalInfo findOneById(Long id);
}
