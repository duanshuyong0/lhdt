package lhdt.admin.svc.lowinfo.service.impl;

import lhdt.admin.svc.common.AdminSvcServiceImpl;
import lhdt.admin.svc.lowinfo.domain.LowInfoDet;
import lhdt.admin.svc.lowinfo.persistence.LowInfoDetMapper;
import lhdt.admin.svc.lowinfo.persistence.LowInfoDetRepository;
import lhdt.admin.svc.lowinfo.service.LowInfoDetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service("lowInfoDetService")
public class LowInfoDetServiceImpl
        extends AdminSvcServiceImpl<LowInfoDetRepository, LowInfoDetMapper, LowInfoDet, Long> implements LowInfoDetService {
    @Autowired
    private LowInfoDetRepository jpaRepo;
    @Autowired
    private LowInfoDetMapper mapper;

    @PostConstruct
    private void init() {
        super.set(jpaRepo, mapper, new LowInfoDet());
    }
}
