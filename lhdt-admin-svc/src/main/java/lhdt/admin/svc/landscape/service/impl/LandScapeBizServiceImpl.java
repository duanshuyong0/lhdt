package lhdt.admin.svc.landscape.service.impl;

import lhdt.admin.svc.file.domain.FileInfo;
import lhdt.admin.svc.file.persistence.FileInfoRepository;
import lhdt.admin.svc.file.service.FileInfoService;
import lhdt.admin.svc.landscape.domain.LandScapeDiff;
import lhdt.admin.svc.landscape.domain.LandScapeDiffDTO.LandScapeDiffDefault;
import lhdt.admin.svc.landscape.model.LandScapeDiffParam;
import lhdt.admin.svc.landscape.persistence.LandScapeDiffRepository;
import lhdt.admin.svc.landscape.service.LandScapeBizService;
import lhdt.admin.svc.landscape.service.LandScapeDiffGroupService;
import lhdt.admin.svc.landscape.service.LandScapeDiffService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

/**
 * 경관분석비즈니스 로직에 대한 세부 기능 구현 클래스
 */
@Service
@RequiredArgsConstructor
public class LandScapeBizServiceImpl implements LandScapeBizService {
    private final LandScapeDiffService landScapeDiffService;
    private final FileInfoService fileInfoService;
    private final LandScapeDiffGroupService landScapeDiffGroupService;

    /**
     * 파일정보 및 경관비교 정보를 등록합니다
     * @param fileInfos
     * @param landScapeDiff
     * @return
     */
    @Override
    @Transactional
    public LandScapeDiff registDiffAndFileInfo(List<FileInfo> fileInfos, LandScapeDiffParam landScapeDiff) {
        var id = landScapeDiff.getLandScapeDiffGroupId();
        var name = landScapeDiff.getLandscapeName();
        var cameraStat = landScapeDiff.getCaptureCameraState();
        var obj = new LandScapeDiff();
        obj.setLsDiffName(name);
        obj.setCaptureCameraState(cameraStat);
        var group = this.landScapeDiffGroupService.findById(id);
        obj.setLandScapeDiffGroup(group);
        var fileResult = this.fileInfoService.regist(fileInfos.get(0));
        obj.setLsDiffImgInfo(fileResult);
        var result = this.landScapeDiffService.regist(obj);
        return result;
    }
}
