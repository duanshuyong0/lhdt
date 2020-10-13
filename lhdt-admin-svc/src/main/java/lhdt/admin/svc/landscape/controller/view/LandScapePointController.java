package lhdt.admin.svc.landscape.controller.view;

import lhdt.admin.svc.cityplanning.service.CPDistricInfoService;
import lhdt.admin.svc.cityplanning.service.CPLocalInfoService;
import lhdt.admin.svc.landscape.domain.LandScapePoint;
import lhdt.admin.svc.landscape.model.LandScapeRegistParam;
import lhdt.admin.svc.landscape.service.LandScapePointService;
import lhdt.admin.svc.landscape.type.LSPointActionType;
import lhdt.admin.svc.landscape.type.LandScapeAnalsType;
import lhdt.cmmn.misc.CmmnPageSize;
import lhdt.cmmn.misc.CmmnPaginator;
import lhdt.cmmn.misc.CmmnPaginatorInfo;
import lhdt.cmmn.misc.CmmnUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 조망점 관리 가시화 페이지에 대한 정보를 제공합니다
 */
@Slf4j
@Controller
@RequestMapping("/ls-point")
public class LandScapePointController {
    @Autowired
    private LandScapePointService landScapeService;
    @Autowired
    private CPDistricInfoService cpDistricInfoService;
    @Autowired
    private CPLocalInfoService cpLocalInfoService;

    /**
     * 경관비교점 페이지를 생성합니다
     * @param landscape_page
     * @param model
     * @return
     */
    @GetMapping()
    public String viewLsPoint(
            @RequestParam(value = "landScapePage", defaultValue = "1") Integer landscape_page,
            Model model) {
        Page<LandScapePoint> cpLocalInfoPage = landScapeService
                .findAllPgByStartPg(landscape_page -1, CmmnPageSize.NOTICE.getContent());
        model.addAttribute("landScapePage", cpLocalInfoPage);

        CmmnPaginatorInfo cpLocalPageNav = CmmnPaginator.getPaginatorMap(cpLocalInfoPage, CmmnPageSize.NOTICE);
        model.addAttribute("landScapePageInfo", cpLocalPageNav);

        return "/landscape-point/index";
    }

    /**
     * 경관점 등록 페이지로 이동합니다
     * @param model
     * @return
     */
    @GetMapping("/regist")
    public String registLsPoint(Model model) {
        var apara2 = CmmnUtils.getEnum2Map(LandScapeAnalsType.class, 0);
        model.addAttribute("landScapeAnalsType", apara2);
        return "/landscape-point/edit";
    }

    /**
     * 경관점 비교 페이지로 이동합니다
     * @param id
     * @param model
     * @return
     */
    @GetMapping("/edit/{id}")
    public String editLsPoint(@PathVariable(value = "id") Long id, Model model) {
        LandScapePoint localInfo = landScapeService.findById(id);
        localInfo.setLSPointActionType(LSPointActionType.EDIT);
        model.addAttribute("landScapePointInfo", localInfo);

//        var landDistric = cpDistricInfoService.findAll();
//        landDistric.stream()
//                .map(p -> CPFullNameAndId.builder()
//                        .id(p.getId())
//                        .cpFullName(new CPFullName(p.getCpLocalInfo().getLocalName(), p.getDistrictName()))
//                        .build()).collect(Collectors.toList());
//
//        model.addAttribute("cpFullNameAndId", landDistric);
        return "/landscape-point/edit";
    }

    /**
     * 경관점 조회 페이지로 이동합니다
     * @param id
     * @param model
     * @return
     */
    @GetMapping("/content/{id}")
    public String viewLsPoint(@PathVariable(value = "id") Long id, Model model) {
        LandScapePoint localInfo = landScapeService.findById(id);
        localInfo.setLSPointActionType(LSPointActionType.CONTENT);
        model.addAttribute("landScapePointInfo", localInfo);

        return "/landscape-point/edit";
    }

    /**
     * 정관점을 등록합니다
     * @param model
     * @param landScapeRegistParam
     * @return
     */
    @PostMapping("/edit")
    public String registLsPoint(Model model, LandScapeRegistParam landScapeRegistParam) {
        System.out.println(landScapeRegistParam.toString());
        LandScapePoint landScapePoint = new LandScapePoint();
        landScapePoint.setLandScapePointName(landScapeRegistParam.getLandScapeAnalsName());
        if (landScapeRegistParam.getLandScapeAnalsType() == LandScapeAnalsType.점) {
            Point start_p = new Point(landScapeRegistParam.getStartPosX(), landScapeRegistParam.getStartPosY());
            landScapePoint.setStartLandScapePos(start_p);
        } else {
            Point start_p = new Point(landScapeRegistParam.getStartPosX(), landScapeRegistParam.getStartPosY());
            landScapePoint.setStartLandScapePos(start_p);
            Point end_p = new Point(landScapeRegistParam.getEndPosX(), landScapeRegistParam.getEndPosY());
            landScapePoint.setEndLandScapePos(end_p);
        }
        landScapePoint.setLandScapePointType(landScapeRegistParam.getLandScapeAnalsType());
        var localInfo = landScapeService.regist(landScapePoint);
        model.addAttribute("localInfo", localInfo);

        return "redirect:/ls-point";
    }

    /**
     * 경관점을 수정합니다
     * @param model
     * @param landScapeRegistParam
     * @return
     */
    @PutMapping("/edit")
    public String editLsPoint(Model model, LandScapeRegistParam landScapeRegistParam) {
        LandScapePoint landScapePoint = new LandScapePoint();
        landScapePoint.setLandScapePointName(landScapeRegistParam.getLandScapeAnalsName());
        if (landScapeRegistParam.getLandScapeAnalsType() == LandScapeAnalsType.점) {
            Point start_p = new Point(landScapeRegistParam.getStartPosX(), landScapeRegistParam.getStartPosY());
            landScapePoint.setStartLandScapePos(start_p);
        } else {
            Point start_p = new Point(landScapeRegistParam.getStartPosX(), landScapeRegistParam.getStartPosY());
            landScapePoint.setStartLandScapePos(start_p);
            Point end_p = new Point(landScapeRegistParam.getEndPosX(), landScapeRegistParam.getEndPosY());
            landScapePoint.setEndLandScapePos(end_p);
        }
        var localInfo = landScapeService.update(landScapeRegistParam.getId(), landScapePoint);
        model.addAttribute("localInfo", localInfo);

        return "redirect:/ls-point";
    }

}
