package lhdt.admin.svc.landscape.controller.view;

import lhdt.admin.svc.file.domain.FileInfo;
import lhdt.admin.svc.file.service.FileInfoService;
import lhdt.admin.svc.landscape.domain.LandScapeDiff;
import lhdt.admin.svc.landscape.domain.LandScapeDiffGroup;
import lhdt.admin.svc.landscape.service.LandScapeDiffGroupService;
import lhdt.admin.svc.landscape.service.LandScapeDiffService;
import lhdt.ds.common.misc.DSPageSize;
import lhdt.ds.common.misc.DSPaginator;
import lhdt.ds.common.misc.DSPaginatorInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Controller
@RequestMapping("/ls-diff")
public class LandScapeDiffController {
    @Autowired
    private LandScapeDiffService landScapeDiffService;

    @GetMapping()
    public String getNoticePage(
            @RequestParam(value = "lsDiffPage", defaultValue = "1") Integer landscape_page,
            Model model) {
        Page<LandScapeDiff> cpLocalInfoPage = landScapeDiffService
                .findAllPgByStartPg(landscape_page -1, DSPageSize.NOTICE.getContent());
        model.addAttribute("lsDiffPage", cpLocalInfoPage);

        DSPaginatorInfo cpLocalPageNav = DSPaginator.getPaginatorMap(cpLocalInfoPage, DSPageSize.NOTICE);
        model.addAttribute("lsDiffPageInfo", cpLocalPageNav);

        return "landscape-diff/index";
    }
    @GetMapping(
            value = "/img/{id}",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public byte[] getImageWithMediaType(@RequestParam(value = "id") Integer id) throws IOException{
        var lsDiff = landScapeDiffService.findById(Long.valueOf(id));
        String fileName = lsDiff.getLsDiffImgInfo().getFileName();
        String filePath = lsDiff.getLsDiffImgInfo().getFilePath();
        String fileExt = lsDiff.getLsDiffImgInfo().getFileExtention();
        String fullPath = filePath + "/" + fileName + "." + fileExt;
        InputStream in = getClass().getResourceAsStream(fullPath);
        return IOUtils.toByteArray(in);
    }
}
