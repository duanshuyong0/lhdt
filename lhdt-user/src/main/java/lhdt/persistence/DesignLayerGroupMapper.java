package lhdt.persistence;

import lhdt.domain.extrusionmodel.DesignLayerGroup;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignLayerGroupMapper {

	/**
     * 디자인 레이어 그룹 목록
     * @return
     */
    List<DesignLayerGroup> getListDesignLayerGroup();

    /**
     * 디자인 레이어 정보 조회
     * @param designLayerGroup
     * @return
     */
    DesignLayerGroup getDesignLayerGroup(DesignLayerGroup designLayerGroup);
}
