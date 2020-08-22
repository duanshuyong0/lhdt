package lhdt.api;

import lhdt.common.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DesignLayerGroupAPIControllerTests extends BaseControllerTest {

    @Test
    @DisplayName("DesignLayerGroup 목록 조회 하기")
    public void getDesignLayerGroup() throws Exception {
        this.mockMvc.perform(get("/api/design-layer-groups"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.designLayerGroups[0]._links.self").exists())
                .andExpect(jsonPath("_links.self").exists())
                .andExpect(jsonPath("_links.profile").exists())
                .andDo(document("design-layer-group-list"));
    }

    @Test
    @DisplayName("DesignLayerGroup 단일 조회 하기")
    public void getDesignLayerGroupById() throws Exception {
        this.mockMvc.perform(get("/api/design-layer-groups/{id}", 1))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("designLayerGroupId").exists())
                .andExpect(jsonPath("_links.self").exists())
                .andExpect(jsonPath("_links.profile").exists())
                .andDo(document("design-layer-group-get",
                        relaxedResponseFields(
                                fieldWithPath("designLayerGroupId").description("design layer 그룹 고유번호"),
                                fieldWithPath("designLayerGroupName").description("design layer 그룹명"),
                                fieldWithPath("userId").description("아이디"),
                                fieldWithPath("ancestor").description("조상"),
                                fieldWithPath("parent").description("부모"),
                                fieldWithPath("parentName").description("부모명"),
                                fieldWithPath("depth").description("깊이"),
                                fieldWithPath("viewOrder").description("나열 순서"),
                                fieldWithPath("children").description("자식 존재 유무"),
                                fieldWithPath("available").description("사용 유무"),
                                fieldWithPath("description").description("설명"),
                                fieldWithPath("updateDate").description("수정일"),
                                fieldWithPath("insertDate").description("등록일"),
                                fieldWithPath("designLayerList").description("자식 desing 레이어 목록")
                        )
                ));
    }
}