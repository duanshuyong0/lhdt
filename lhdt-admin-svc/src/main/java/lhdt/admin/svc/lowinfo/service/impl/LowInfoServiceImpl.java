package lhdt.admin.svc.lowinfo.service.impl;

import lhdt.admin.svc.common.AdminSvcServiceImpl;
import lhdt.admin.svc.lowinfo.domain.LowInfo;
import lhdt.admin.svc.lowinfo.persistence.LowInfoMapper;
import lhdt.admin.svc.lowinfo.persistence.LowInfoRepository;
import lhdt.admin.svc.lowinfo.service.LowInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class LowInfoServiceImpl
        extends AdminSvcServiceImpl<LowInfoRepository, LowInfoMapper, LowInfo, Long> implements LowInfoService {
    @Autowired
    private LowInfoRepository jpaRepo;
    @Autowired
    private LowInfoMapper mapper;

    @PostConstruct
    private void init() {
        super.set(jpaRepo, mapper, new LowInfo());
    }
}
