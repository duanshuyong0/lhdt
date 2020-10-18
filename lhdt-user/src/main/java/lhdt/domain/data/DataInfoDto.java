package lhdt.domain.data;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.server.core.Relation;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data 정보
 * @author Cheon JeongDae
 *
 */
@ToString(callSuper = true)
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Relation(collectionRelation = "dataInfos")
public class DataInfoDto implements Serializable {

	private static final long serialVersionUID = -5333401848770831812L;

	// 사용자명
	private String userId;
	// 수정자 아이디
	private String updateUserId;
	// 고유번호
	private Long dataId;
	// Data Group 고유번호
	private Integer dataGroupId;
	// converter job 고유번호
	private Long converterJobId;
	// Data Group 이름
	private String dataGroupName;
	// admin : 관리자용 데이터 그룹, user : 일반 사용자용 데이터 그룹
	private String dataGroupTarget;
	// data group key
	private String dataGroupKey;
	// smart
	private Boolean tiling;
	
	// data 고유 식별번호
	private String dataKey;
	// data 고유 식별번호
	private String oldDataKey;
	// data 이름
	private String dataName;
	// 데이터 타입(중복). 3ds,obj,dae,collada,ifc,las,citygml,indoorgml,etc
	private String dataType;
	// common : 공통, public : 공개, private : 개인, group : 그룹
	private String sharing;
	// 부모 고유번호
	private Long parent;
	// 부모 이름(화면 표시용)
	private String parentName;
	
	// origin : latitude, longitude, height 를 origin에 맟춤. boundingboxcenter : latitude, longitude, height 를 boundingboxcenter에 맟춤.
	private String mappingType;

	// POINT(위도, 경도). 공간 검색 속도 때문에 altitude는 분리
	private String location;
	// 높이
	private BigDecimal altitude;
	// heading
	private BigDecimal heading;
	// pitch
	private BigDecimal pitch;
	// roll
	private BigDecimal roll;
	
	// 조상
	private Integer childrenAncestor;
	// 부모
	private Integer childrenParent;
	// 깊이
	private Integer childrenDepth;
	// 순서
	private Integer childrenViewOrder;
	
	// 기본 정보
	private String metainfo;
	// data 상태. processing : 변환중, use : 사용중, unused : 사용중지(관리자), delete : 삭제(비표시)
	private String status;
	// 라벨. data_name과 다른 이름으로 style을 활용하기 위함. 줄바꿈 \n
	private String label;
	// 라벨 템플릿. 데이터에 적용할 라벨 템플릿 타입을 저장
	private String labelTemplate;
	// 속성 존재 유무. true : 존재, false : 존재하지 않음(기본값)
	private Boolean attributeExist;
	// object 속성 존재 유무. true : 존재, false : 존재하지 않음(기본값)
	private Boolean objectAttributeExist;
	// 설명
	private String description;
	// 수정일
	@DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updateDate;
	// 등록일
	@DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private LocalDateTime insertDate;
}
