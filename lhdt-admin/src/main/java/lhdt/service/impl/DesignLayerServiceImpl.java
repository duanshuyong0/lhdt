package lhdt.service.impl;

import lhdt.config.PropertiesConfig;
import lhdt.domain.ShapeFileExt;
import lhdt.domain.extrusionmodel.*;
import lhdt.domain.layer.LayerFileInfo;
import lhdt.domain.policy.GeoPolicy;
import lhdt.geospatial.LayerStyleParser;
import lhdt.geospatial.Ogr2OgrExecute;
import lhdt.persistence.DesignLayerFileInfoMapper;
import lhdt.persistence.DesignLayerMapper;
import lhdt.security.Crypt;
import lhdt.service.DesignLayerService;
import lhdt.service.GeoPolicyService;
import lhdt.support.LogMessageSupport;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.*;

/**
 * 여기서는 Geoserver Rest API 결과를 가지고 파싱 하기 때문에 RestTemplate을 커스트마이징하면 안됨
 * TODO DesignLayerFileInfoMapper 는 서비스를 호출해서 해야 하는데... 귀찮아서
 * @author Cheon JeongDae
 *
 */
@Slf4j
@Service
public class DesignLayerServiceImpl implements DesignLayerService {

	@Value("${spring.datasource.url}")
	private String url;
	@Value("${spring.datasource.username}")
	private String username;
	@Value("${spring.datasource.password}")
	private String password;

    @Autowired
    private GeoPolicyService geoPolicyService;

    @Autowired
    private PropertiesConfig propertiesConfig;

    @Autowired
    private DesignLayerMapper designLayerMapper;
    @Autowired
    private DesignLayerFileInfoMapper designLayerFileInfoMapper;

    @Autowired
    private ModelMapper modelMapper;

    /**
	 * Design Layer 총 건수
	 * @param designLayer
	 * @return
	 */
    @Transactional(readOnly=true)
	public Long getDesignLayerTotalCount(DesignLayer designLayer) {
    	return designLayerMapper.getDesignLayerTotalCount(designLayer);
    }
    
    /**
    * design layer 목록
    * @return
    */
    @Transactional(readOnly=true)
    public List<DesignLayer> getListDesignLayer(DesignLayer designLayer) {
        return designLayerMapper.getListDesignLayer(designLayer);
    }
    
    /**
     * geoserver design layer 목록 조회
     */
    @Transactional(readOnly=true)
    public String getListGeoserverDesignLayer(GeoPolicy geoPolicy) {
    	String geoserverDesignLayerJson = null;
    	try {
			RestTemplate restTemplate = new RestTemplate();
			
			HttpHeaders headers = new HttpHeaders();
			// 클라이언트가 서버에 어떤 형식(MediaType)으로 달라는 요청을 할 수 있는데 이게 Accpet 헤더를 뜻함.
			List<MediaType> acceptList = new ArrayList<>();
			acceptList.add(MediaType.ALL);
			headers.setAccept(acceptList);
			// 클라이언트가 request에 실어 보내는 데이타(body)의 형식(MediaType)를 표현
			headers.setContentType(MediaType.TEXT_XML);
			// geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding 
			headers.add("Authorization", "Basic " + Base64.getEncoder().
					encodeToString((geoPolicy.getGeoserverUser() + ":" + geoPolicy.getGeoserverPassword()).getBytes()));
			
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			//Add the String Message converter
			messageConverters.add(new StringHttpMessageConverter());
			//Add the message converters to the restTemplate
			restTemplate.setMessageConverters(messageConverters);
		    
			HttpEntity<String> entity = new HttpEntity<>(headers);
			
			String url = geoPolicy.getGeoserverDataUrl() + "/rest/workspaces/" + geoPolicy.getGeoserverDataWorkspace()+ "/layers";
			ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
			log.info("-------- statusCode = {}, body = {}", response.getStatusCodeValue(), response.getBody());
            geoserverDesignLayerJson = response.getBody().toString();
		
    	} catch(RestClientException e) {
            LogMessageSupport.printMessage(e, "@@@ RestClientException. message = {}", e.getMessage());
    	} catch(RuntimeException e) {
    	    LogMessageSupport.printMessage(e, "@@@ RuntimeException. message = {}", e.getMessage());
		} catch(Exception e) {
    	    LogMessageSupport.printMessage(e, "@@@ Exception. message = {}", e.getMessage());
		}
    	
    	return geoserverDesignLayerJson;
    }

    /**
    * design layer 정보 취득
    * @param designLayerId
    * @return
    */
    @Transactional(readOnly=true)
    public DesignLayer getDesignLayer(Long designLayerId) {
        return designLayerMapper.getDesignLayer(designLayerId);
    }

    /**
     * 디자인 레이어 key 중복 확인
     * @param designLayerKey
     * @return
     */
    @Transactional(readOnly=true)
    public Boolean isDesignLayerKeyDuplication(String designLayerKey) {
        //return designLayerMapper.isDesignLayerKeyDuplication(designLayerKey);
        GeoPolicy geoPolicy = geoPolicyService.getGeoPolicy();
        HttpStatus httpStatus = getDesignLayerStatus(geoPolicy, designLayerKey);
        return HttpStatus.NOT_FOUND != httpStatus;
    }

    /**
    * design 레이어 테이블의 컬럼 타입이 어떤 geometry 타입인지를 구함
    * @param designLayerKey
    * @return
    */
    @Transactional(readOnly=true)
    public String getGeometryType(String designLayerKey) {
        return designLayerMapper.getGeometryType(designLayerKey);
    }

    /**
     *
     * @param designLayerKey
     * @return
     */
    @Transactional(readOnly = true)
    public String getDesignLayerColumn(String designLayerKey) {
    	return designLayerMapper.getDesignLayerColumn(designLayerKey);
    }

    /**
    * design 레이어 등록
    * @param designLayer
    * @return
    */
    @Transactional
    public Map<String, Object> insertDesignLayer(DesignLayer designLayer, List<DesignLayerFileInfo> designLayerFileInfoList) {
    	Map<String, Object> designLayerFileInfoTeamMap = new HashMap<>();

        // design layer 정보 수정
        designLayerMapper.insertDesignLayer(designLayer);
        // shape 파일이 있을 경우
        if(!designLayerFileInfoList.isEmpty()) {
            String shapeFileName = null;
            String shapeEncoding = null;
            Long designLayerId = designLayer.getDesignLayerId();
            String userId = designLayer.getUserId();

            Long designLayerFileInfoTeamId = 0L;
            List<Long> designLayerFileInfoTeamIdList = new ArrayList<>();
            for(DesignLayerFileInfo designLayerFileInfo : designLayerFileInfoList) {
                designLayerFileInfo.setDesignLayerId(designLayerId);
                designLayerFileInfo.setUserId(userId);
                designLayerFileInfo.setEnableYn("Y");

                designLayerFileInfoMapper.insertDesignLayerFileInfoMapper(designLayerFileInfo);
                designLayerFileInfoTeamIdList.add(designLayerFileInfo.getDesignLayerFileInfoId());

                if(LayerFileInfo.SHAPE_EXTENSION.equals(designLayerFileInfo.getFileExt().toLowerCase())) {
                    designLayerFileInfoTeamId = designLayerFileInfo.getDesignLayerFileInfoId();
                    shapeFileName = designLayerFileInfo.getFilePath() + designLayerFileInfo.getFileRealName();
                    shapeEncoding = designLayerFileInfo.getShapeEncoding();
                }
            }
            log.info("---- shapeFileName = {}", shapeFileName);

            Integer fileVersion = designLayerFileInfoMapper.getMaxFileVersion(designLayerId);
            if(fileVersion == null) fileVersion = 0;
            fileVersion = fileVersion + 1;
            designLayerFileInfoTeamMap.put("fileVersion", fileVersion);
            designLayerFileInfoTeamMap.put("shapeFileName", shapeFileName);
            designLayerFileInfoTeamMap.put("shapeEncoding", shapeEncoding);
            designLayerFileInfoTeamMap.put("designLayerFileInfoTeamId", designLayerFileInfoTeamId);
            designLayerFileInfoTeamMap.put("designLayerFileInfoTeamIdList", designLayerFileInfoTeamIdList);
            designLayerFileInfoTeamMap.put("designLayerId", designLayerId);
            log.info("+++ designLayerFileInfoTeamMap = {}", designLayerFileInfoTeamMap);
            designLayerFileInfoMapper.updateDesignLayerFileInfoTeam(designLayerFileInfoTeamMap);
        }

        return designLayerFileInfoTeamMap;
    }

    /**
    * shape 파일을 이용한 design layer 정보 수정
    * @param designLayer
    * @param isDesignLayerFileInfoExist
    * @param designLayerFileInfoList
    * @return
    * @throws Exception
    */
    @Transactional
    public Map<String, Object> updateDesignLayer(DesignLayer designLayer, boolean isDesignLayerFileInfoExist, List<DesignLayerFileInfo> designLayerFileInfoList) {

        Map<String, Object> designLayerFileInfoTeamMap = new HashMap<>();

        // design layer 정보 수정
        designLayerMapper.updateDesignLayer(designLayer);

        // shape 파일이 있을 경우
        if(!designLayerFileInfoList.isEmpty()) {
            String shapeFileName = null;
            String shapeEncoding = null;
            Long designLayerId = designLayer.getDesignLayerId();
            String userId = designLayer.getUserId();
            String tableName = designLayer.getDesignLayerKey();

            if(isDesignLayerFileInfoExist) {
                // 모든 design_layer_file_info 의 shape 상태를 비활성화로 update 함
                designLayerFileInfoMapper.updateDesignLayerFileInfoAllDisabledByDesignLayerId(designLayerId);
                // 이 design 레이어의 지난 데이터를 비 활성화 상태로 update 함
                designLayerFileInfoMapper.updateShapePreDataDisable(tableName);
            }

            Long designLayerFileInfoTeamId = 0L;
            List<Long> designLayerFileInfoTeamIdList = new ArrayList<>();
            for(DesignLayerFileInfo designLayerFileInfo : designLayerFileInfoList) {
                designLayerFileInfo.setDesignLayerId(designLayerId);
                designLayerFileInfo.setUserId(userId);
                designLayerFileInfo.setEnableYn("Y");

                designLayerFileInfoMapper.insertDesignLayerFileInfoMapper(designLayerFileInfo);
                designLayerFileInfoTeamIdList.add(designLayerFileInfo.getDesignLayerFileInfoId());

                if(DesignLayerFileInfo.SHAPE_EXTENSION.equals(designLayerFileInfo.getFileExt().toLowerCase())) {
                    designLayerFileInfoTeamId = designLayerFileInfo.getDesignLayerFileInfoId();
                    shapeFileName = designLayerFileInfo.getFilePath() + designLayerFileInfo.getFileRealName();
                    shapeEncoding = designLayerFileInfo.getShapeEncoding();
                }
            }
            log.info("---- shapeFileName = {}", shapeFileName);

            Integer fileVersion = designLayerFileInfoMapper.getMaxFileVersion(designLayerId);
            if(fileVersion == null) fileVersion = 0;
            fileVersion = fileVersion + 1;
            designLayerFileInfoTeamMap.put("fileVersion", fileVersion);
            designLayerFileInfoTeamMap.put("shapeFileName", shapeFileName);
            designLayerFileInfoTeamMap.put("shapeEncoding", shapeEncoding);
            designLayerFileInfoTeamMap.put("layerFileInfoTeamId", designLayerFileInfoTeamId);
            designLayerFileInfoTeamMap.put("layerFileInfoTeamIdList", designLayerFileInfoTeamIdList);
            log.info("+++ designLayerFileInfoTeamMap = {}", designLayerFileInfoTeamMap);
            designLayerFileInfoMapper.updateDesignLayerFileInfoTeam(designLayerFileInfoTeamMap);
        }

        return designLayerFileInfoTeamMap;
    }


    /**
     * shapeInfo info insert
     * @param designLayer
     * @param shapeInfoList
     * @throws Exception
     */
    @Transactional
    public void insertShapeInfo(DesignLayer designLayer, List<DesignLayer> shapeInfoList) throws Exception {
        if(DesignLayer.DesignLayerType.LAND == DesignLayer.DesignLayerType.valueOf(designLayer.getDesignLayerGroupType().toUpperCase())) {
            shapeInfoList.forEach(f -> {
                f.setDesignLayerId(designLayer.getDesignLayerId());
                f.setDesignLayerGroupId(designLayer.getDesignLayerGroupId());
                f.setCoordinate(designLayer.getCoordinate().split(":")[1]);
                designLayerMapper.insertGeometryLand(modelMapper.map(f, DesignLayerLand.class));
            });
        } else if(DesignLayer.DesignLayerType.BUILDING == DesignLayer.DesignLayerType.valueOf(designLayer.getDesignLayerGroupType().toLowerCase())) {
            shapeInfoList.forEach(f -> {
                f.setDesignLayerId(designLayer.getDesignLayerId());
                f.setDesignLayerGroupId(designLayer.getDesignLayerGroupId());
                f.setCoordinate(designLayer.getCoordinate().split(":")[1]);
                designLayerMapper.insertGeometryBuilding(modelMapper.map(f, DesignLayerBuilding.class));
            });
        }
    }

    /**
     * shp파일 정보를 db 정보 기준으로 export
     */
    @Transactional
    public void exportOgr2Ogr(DesignLayerFileInfo designLayerFileInfo, DesignLayer designLayer) throws Exception {
        String tableName = designLayer.getDesignLayerKey();
        Integer versionId = designLayerFileInfo.getVersionId();
        String shpEncoding = designLayerFileInfo.getShapeEncoding();
        String exportPath = designLayerFileInfo.getFilePath() + designLayerFileInfo.getFileRealName()+ "." + ShapeFileExt.SHP.getValue();

        String osType = propertiesConfig.getOsType().toUpperCase();
        String ogr2ogrPort = propertiesConfig.getOgr2ogrPort();
        String ogr2ogrHost = propertiesConfig.getOgr2ogrHost();
        String dbName = Crypt.decrypt(url);
        dbName = dbName.substring(dbName.lastIndexOf("/") + 1);
        String driver = "PG:host="+ogr2ogrHost + " port=" + ogr2ogrPort+ " dbname=" + dbName + " user=" + Crypt.decrypt(username) + " password=" + Crypt.decrypt(password);
        GeoPolicy geoPolicy = geoPolicyService.getGeoPolicy();
        String designLayerSourceCoordinate = geoPolicy.getLayerSourceCoordinate();
        String designLayerTargetCoordinate = geoPolicy.getLayerTargetCoordinate();
        String designLayerColumn = getDesignLayerColumn(tableName);
        String sql = "SELECT "+ designLayer + ", null::text AS enable_yn, null::int AS version_id FROM " + tableName + " WHERE version_id=" + versionId;

        Ogr2OgrExecute ogr2OgrExecute = new Ogr2OgrExecute(osType, driver, shpEncoding, exportPath, sql, designLayerSourceCoordinate, designLayerTargetCoordinate);
        ogr2OgrExecute.export();
    }

    /**
    * design layer 를 이 shape 파일로 활성화
    * @param designLayerId
    * @param designLayerFileInfoTeamId
    * @return
    */
    @Transactional
    public int updateDesignLayerByDesignLayerFileInfoId(Long designLayerId, Long designLayerFileInfoTeamId, Long designLayerFileInfoId) {
        // design layer 정보 수정
        DesignLayer designLayer = designLayerMapper.getDesignLayer(designLayerId);
        String tableName = designLayer.getDesignLayerKey();

        // 모든 design_layer_file_info 의 shape 상태를 비활성화로 update 함
        designLayerFileInfoMapper.updateDesignLayerFileInfoAllDisabledByDesignLayerId(designLayerId);
        // shape table 모든 데이터를 비활성화 함
        designLayerFileInfoMapper.updateShapePreDataDisable(tableName);

        DesignLayerFileInfo designLayerFileInfo = new DesignLayerFileInfo();
        designLayerFileInfo.setDesignLayerId(designLayerId);
        designLayerFileInfo.setDesignLayerFileInfoTeamId(designLayerFileInfoTeamId);
        designLayerFileInfo.setEnableYn("Y");
        designLayerFileInfoMapper.updateDesignLayerFileInfoByTeamId(designLayerFileInfo);

        Integer fileVersion = designLayerFileInfoMapper.getDesignLayerShapeFileVersion(designLayerFileInfoId);
        Map<String, String> orgMap = new HashMap<>();
        orgMap.put("fileVersion", fileVersion.toString());
        orgMap.put("tableName", tableName);
        orgMap.put("enableYn", "Y");

        return designLayerFileInfoMapper.updateOgr2OgrStatus(orgMap);
    }

    /**
     * design 레이어 롤백 처리
     * @param designLayer
     * @param isDesignLayerFileInfoExist
     * @param designLayerFileInfo
     * @param deleteDesignLayerFileInfoTeamId
     */
	@Transactional
	public void rollbackDesignLayer(DesignLayer designLayer, boolean isDesignLayerFileInfoExist, DesignLayerFileInfo designLayerFileInfo, Long deleteDesignLayerFileInfoTeamId) {
		designLayerMapper.updateDesignLayer(designLayer);
		if (isDesignLayerFileInfoExist) {
            designLayerFileInfoMapper.deleteDesignLayerFileInfoByTeamId(deleteDesignLayerFileInfoTeamId);

			// 모든 design_layer_file_info 의 shape 상태를 비활성화로 update 함
            designLayerFileInfoMapper.updateDesignLayerFileInfoAllDisabledByDesignLayerId(designLayer.getDesignLayerId());
			// 이 design 레이어의 지난 데이터를 비 활성화 상태로 update 함
            designLayerFileInfoMapper.updateShapePreDataDisable(designLayer.getDesignLayerKey());

			// 이전 design 레이어 이력을 활성화
            designLayerFileInfoMapper.updateDesignLayerFileInfoByTeamId(designLayerFileInfo);
			// 이전 shape 데이터를 활성화
			Map<String, String> orgMap = new HashMap<>();
			orgMap.put("fileVersion", designLayerFileInfo.getVersionId().toString());
			orgMap.put("tableName", designLayer.getDesignLayerKey());
			orgMap.put("enableYn", "Y");
            designLayerFileInfoMapper.updateOgr2OgrStatus(orgMap);
		} else {
            designLayerFileInfoMapper.deleteDesignLayerFileInfo(designLayer.getDesignLayerId());
			// TODO shape 파일에도 이력이 있음 지워 줘야 하나?
		}
	}
	
	/**
	 * design 레이어 삭제
	 * 
	 * @param designLayerId
	 * @return
	 */
	@Transactional
	public int deleteDesignLayer(Long designLayerId) {
		// geoserver layer 삭제
		GeoPolicy geopolicy = geoPolicyService.getGeoPolicy();
		DesignLayer designLayer = designLayerMapper.getDesignLayer(designLayerId);
		// 업로드 파일 삭제
		List<String> designLayerFilePath = designLayerFileInfoMapper.getListDesignLayerFilePath(designLayerId);
        designLayerFilePath.forEach( path -> {
						File directory = new File(path);
				        if(directory.exists()) { directory.delete();}
					});
		if(!designLayerFilePath.isEmpty()) {
			// geoserver design layer 삭제
			deleteGeoserverDesignLayer(geopolicy, designLayer.getDesignLayerKey());
			// geoserver style 삭제
			deleteGeoserverLayerStyle(geopolicy, designLayer.getDesignLayerKey());
			// design_layer_file_info 히스토리 삭제
            designLayerFileInfoMapper.deleteDesignLayerFileInfo(designLayerId);
			// 공간정보 테이블 삭제
			String designLayerExists = designLayerMapper.isDesignLayerExists(designLayer.getDesignLayerKey());
			if(designLayerExists != null) {
				designLayerMapper.deleteDesignLayerTable(designLayer.getDesignLayerKey());
			}
		}

		// design 레이어 메타정보 삭제
		return designLayerMapper.deleteDesignLayer(designLayerId);
	}

    /**
     * 디자인 레이어 그룹 고유번호를 이용한 삭제
     * @param designLayerGroup
     * @return
     */
    @Transactional
    public int deleteDesignLayerByDesignLayerGroupId(DesignLayerGroup designLayerGroup) {
        // TODO geoserver layer 도 삭제해 줘야 함
        // design_layer detail 도 삭제해야 함
        return designLayerMapper.deleteDesignLayerByDesignLayerGroupId(designLayerGroup);
    }

	/**
    * design layer 가 등록 되어 있지 않은 경우 rest api 를 이용해서 design layer를 등록
    * @throws Exception
    */
    @Transactional
    public void registerDesignLayer(GeoPolicy geoPolicy, String designLayerKey) throws Exception {
        HttpStatus httpStatus = getDesignLayerStatus(geoPolicy, designLayerKey);
        if(HttpStatus.INTERNAL_SERVER_ERROR == httpStatus) {
            throw new Exception();
        }

        if(HttpStatus.OK == httpStatus) {
            log.info("designLayerKey = {} 는 이미 존재하는 design layer 입니다.", designLayerKey);
            // 이미 등록 되어 있음
        } else if(HttpStatus.NOT_FOUND == httpStatus) {
            // 신규 등록
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            // geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding
            headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString( (geoPolicy.getGeoserverUser() + ":" + geoPolicy.getGeoserverPassword()).getBytes()));

            // body
            String tableName = propertiesConfig.getDesignLayerLandTable();
            String xmlString = "<?xml version=\"1.0\" encoding=\"utf-8\"?><featureType><name>" + designLayerKey + "</name><nativeName>"+tableName+"</nativeName></featureType>";

            List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
            //Add the String Message converter
            messageConverters.add(new StringHttpMessageConverter());
            //Add the message converters to the restTemplate

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(messageConverters);

            HttpEntity<String> entity = new HttpEntity<>(xmlString, headers);
            String url = geoPolicy.getGeoserverDataUrl() + "/rest/workspaces/"
                    + geoPolicy.getGeoserverDataWorkspace() + "/datastores/" + geoPolicy.getGeoserverDataStore() + "/featuretypes?recalculate=nativebbox,latlonbbox";

            ResponseEntity<?> response = restTemplate.postForEntity(url, entity, String.class);

            //ResponseEntity<APIResult> responseEntity = restTemplate.exchange(url, HttpMethod.POST, request, APIResult.class);
            log.info("----------------------- response = {}", response);
//			log.info("----------------------- body = {}", response.getBody());

            // shape 파일이 없는 layer를 등록 하려고 하면 400 Bad Request가 나옴
        } else {
            throw new Exception("http status code = " + httpStatus.toString());
        }
    }
    
    /**
     * design 레이어의 스타일 정보를 수정
     * @param designLayer
     * @return
     */
     @Transactional
     public int updateDesignLayerStyle(DesignLayer designLayer) throws Exception {
    	 log.info("==============update design layer style");
         GeoPolicy geoPolicy = geoPolicyService.getGeoPolicy();
         DesignLayer dbDesignLayer = designLayerMapper.getDesignLayer(designLayer.getDesignLayerId());
         designLayer.setDesignLayerKey(dbDesignLayer.getDesignLayerKey());
         String xmlData = getLayerStyleFileData(designLayer.getDesignLayerId());
         HttpStatus httpStatus = getDesignLayerStatus(geoPolicy, designLayer.getDesignLayerKey());
         if(HttpStatus.NOT_FOUND == httpStatus) {
             return httpStatus.value();
         }
         httpStatus = getDesignLayerStyle(geoPolicy, designLayer.getDesignLayerKey());
         if(HttpStatus.INTERNAL_SERVER_ERROR.equals(httpStatus)) {
             throw new Exception();
         }

         if(HttpStatus.OK.equals(httpStatus)) {
             log.info("styleName = {} 는 이미 존재하는 designLayerStyle 입니다.", designLayer.getDesignLayerKey());
             // 이미 등록 되어 있음, update
         } else if(HttpStatus.NOT_FOUND.equals(httpStatus)) {
             // 신규 등록
             insertGeoserverDesignLayerStyle(geoPolicy, designLayer);
             // 기본 지오메트리타입 스타일 get
             xmlData = getLayerDefaultStyleFileData(designLayer.getGeometryType());
         } else {
             throw new Exception("http status code = " + httpStatus.toString());
         }

         LayerStyleParser layerStyleParser = new LayerStyleParser(
                 designLayer.getGeometryType(), designLayer.getLayerFillColor(), designLayer.getLayerAlphaStyle(), designLayer.getLayerLineColor(), designLayer.getLayerLineStyle(), xmlData.trim());
         layerStyleParser.updateLayerStyle();
         designLayer.setStyleFileContent(layerStyleParser.getStyleData());

         updateGeoserverLayerStyle(geoPolicy, designLayer);
         reloadGeoserverLayerStyle(geoPolicy, designLayer);
         
         return 0;

     }
     
	/**
	 * design 레이어가 존재 하는지를 검사
	 * 
	 * @param geopolicy
	 * @param designLayerKey
	 * @return
	 * @throws Exception
	 */
	private HttpStatus getDesignLayerStatus(GeoPolicy geopolicy, String designLayerKey) {
		HttpStatus httpStatus = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_XML);
			// geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding
			headers.add("Authorization", "Basic " + Base64.getEncoder()
					.encodeToString((geopolicy.getGeoserverUser() + ":" + geopolicy.getGeoserverPassword()).getBytes()));

			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			// Add the String Message converter
			messageConverters.add(new StringHttpMessageConverter());
			// Add the message converters to the restTemplate
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setMessageConverters(messageConverters);

			HttpEntity<String> entity = new HttpEntity<>(headers);
			String url = geopolicy.getGeoserverDataUrl() + "/rest/workspaces/" + geopolicy.getGeoserverDataWorkspace()
					+ "/datastores/" + geopolicy.getGeoserverDataStore() + "/featuretypes/" + designLayerKey;
			log.info("-------- url = {}", url);
			ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
			httpStatus = response.getStatusCode();
			log.info("-------- designLayerKey = {}, statusCode = {}, body = {}", designLayerKey, response.getStatusCodeValue(), response.getBody());
		} catch (RestClientException e) {
		    LogMessageSupport.printMessage(e, "-------- RestClientException message = {}", e.getMessage());
			String message = e.getMessage();
			if (message.indexOf("404") >= 0) {
				httpStatus = HttpStatus.NOT_FOUND;
			} else {
				httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
			}
		} catch (Exception e) {
		    LogMessageSupport.printMessage(e, "-------- exception message = {}", e.getMessage());
			String message = e.getMessage();
			if (message.indexOf("404") >= 0) {
				httpStatus = HttpStatus.NOT_FOUND;
			} else {
				httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
			}
		}

		return httpStatus;
	}
     
	/**
	 * design 레이어 스타일 정보 등록
	 * 
	 * @param geopolicy
	 * @param designLayer
	 * @throws Exception
	 */
	private void insertGeoserverDesignLayerStyle(GeoPolicy geopolicy, DesignLayer designLayer) throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		// 클라이언트가 서버에 어떤 형식(MediaType)으로 달라는 요청을 할 수 있는데 이게 Accpet 헤더를 뜻함.
		List<MediaType> acceptList = new ArrayList<>();
		acceptList.add(MediaType.ALL);
		headers.setAccept(acceptList);
		// 클라이언트가 request에 실어 보내는 데이타(body)의 형식(MediaType)를 표현
		headers.setContentType(MediaType.TEXT_XML);
		// geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding
		headers.add("Authorization", "Basic " + Base64.getEncoder()
				.encodeToString((geopolicy.getGeoserverUser() + ":" + geopolicy.getGeoserverPassword()).getBytes()));

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		// Add the String Message converter
		messageConverters.add(new StringHttpMessageConverter());
		// Add the message converters to the restTemplate
		restTemplate.setMessageConverters(messageConverters);

		HttpEntity<String> entity = new HttpEntity<>(getEmptyStyleFile(designLayer.getDesignLayerKey()), headers);

		String url = geopolicy.getGeoserverDataUrl() + "/rest/workspaces/" + geopolicy.getGeoserverDataWorkspace() + "/styles";
		ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
		log.info("-------- insertGeoserverDesignLayerStyle statusCode = {}, body = {}", response.getStatusCodeValue(), response.getBody());
	}
	
	/**
	 * 기존에 존재하는 스타일의 정보를 취득 
	 * @param geopolicy
	 * @param designLayerKey
	 * @return
	 */
    private HttpStatus getDesignLayerStyle(GeoPolicy geopolicy, String designLayerKey) {
        HttpStatus httpStatus = null;
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            // 클라이언트가 서버에 어떤 형식(MediaType)으로 달라는 요청을 할 수 있는데 이게 Accpet 헤더를 뜻함.
            List<MediaType> acceptList = new ArrayList<>();
            acceptList.add(MediaType.APPLICATION_JSON);
            headers.setAccept(acceptList);

            // 클라이언트가 request에 실어 보내는 데이타(body)의 형식(MediaType)를 표현
            headers.setContentType(MediaType.TEXT_XML);
            // geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding
            headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString( (geopolicy.getGeoserverUser() + ":" + geopolicy.getGeoserverPassword()).getBytes()));

            List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
            //Add the String Message converter
            messageConverters.add(new StringHttpMessageConverter());
            //Add the message converters to the restTemplate
            restTemplate.setMessageConverters(messageConverters);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = geopolicy.getGeoserverDataUrl() + "/rest/workspaces/" + geopolicy.getGeoserverDataWorkspace() + "/styles/" + designLayerKey;

            ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            httpStatus = response.getStatusCode();
            log.info("-------- getDesignLayerStyle styleName = {}, statusCode = {}, body = {}", designLayerKey, response.getStatusCodeValue(), response.getBody());
        } catch (RestClientException e) {
            LogMessageSupport.printMessage(e, "-------- RestClientException message = {}", e.getMessage());
			String message = e.getMessage();
			if (message.indexOf("404") >= 0) {
				httpStatus = HttpStatus.NOT_FOUND;
			} else {
				httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
			}
        } catch(Exception e) {
            LogMessageSupport.printMessage(e, "-------- exception message = {}", e.getMessage());
            String message = e.getMessage();
            if(message.indexOf("404") >= 0) {
                httpStatus = HttpStatus.NOT_FOUND;
            } else {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }

        log.info("########### getDesignLayerStyle end");
        return httpStatus;
    }
    
	/**
	 * 기본 레이어 스타일 파일을 취득
	 * 
	 * @param geometryType
	 * @return
	 */
	private String getLayerDefaultStyleFileData(String geometryType) {
		String layerStyleFileData = null;
		HttpStatus httpStatus = null;
		try {
			GeoPolicy geopolicy = geoPolicyService.getGeoPolicy();

			RestTemplate restTemplate = new RestTemplate();

			HttpHeaders headers = new HttpHeaders();
			// 클라이언트가 서버에 어떤 형식(MediaType)으로 달라는 요청을 할 수 있는데 이게 Accpet 헤더를 뜻함.
			List<MediaType> acceptList = new ArrayList<>();
			acceptList.add(MediaType.TEXT_XML);
			headers.setAccept(acceptList);

			// 클라이언트가 request에 실어 보내는 데이타(body)의 형식(MediaType)를 표현
			headers.setContentType(MediaType.TEXT_XML);
			// geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding
			headers.add("Authorization", "Basic " + Base64.getEncoder()
					.encodeToString((geopolicy.getGeoserverUser() + ":" + geopolicy.getGeoserverPassword()).getBytes()));

			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			// Add the String Message converter
			messageConverters.add(new StringHttpMessageConverter());
			// Add the message converters to the restTemplate
			restTemplate.setMessageConverters(messageConverters);

			HttpEntity<String> entity = new HttpEntity<>(headers);

			String url = geopolicy.getGeoserverDataUrl() + "/rest/styles/" + geometryType.toLowerCase() + ".sld";
			ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
			httpStatus = response.getStatusCode();
			layerStyleFileData = response.getBody().toString();
			log.info("-------- getLayerDefaultStyleFileData geometry type = {}, statusCode = {}, body = {}", geometryType, response.getStatusCodeValue(), response.getBody());
		} catch (RestClientException e) {
		    LogMessageSupport.printMessage(e, "-------- RestClientException message = {}", e.getMessage());
			String message = e.getMessage();
			if (message.indexOf("404") >= 0) {
				httpStatus = HttpStatus.NOT_FOUND;
			} else {
				httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
			}
		} catch (Exception e) {
		    LogMessageSupport.printMessage(e, "-------- exception message = {}", e.getMessage());
			String message = e.getMessage();
			if (message.indexOf("404") >= 0) {
				httpStatus = HttpStatus.NOT_FOUND;
			} else {
				httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
			}
		}

		return layerStyleFileData;
	}
     
	/**
	 * 레이어 스타일 파일을 취득
	 * 
	 * @param designLayerId
	 * @return
	 */
	private String getLayerStyleFileData(Long designLayerId) {
		String layerStyleFileData = null;
		HttpStatus httpStatus = null;
		try {
			GeoPolicy geopolicy = geoPolicyService.getGeoPolicy();
			DesignLayer designLayer = designLayerMapper.getDesignLayer(designLayerId);

			RestTemplate restTemplate = new RestTemplate();

			HttpHeaders headers = new HttpHeaders();
			// 클라이언트가 서버에 어떤 형식(MediaType)으로 달라는 요청을 할 수 있는데 이게 Accpet 헤더를 뜻함.
			List<MediaType> acceptList = new ArrayList<>();
			acceptList.add(MediaType.TEXT_XML);
			headers.setAccept(acceptList);

			// 클라이언트가 request에 실어 보내는 데이타(body)의 형식(MediaType)를 표현
			headers.setContentType(MediaType.TEXT_XML);
			// geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding
			headers.add("Authorization", "Basic " + Base64.getEncoder()
					.encodeToString((geopolicy.getGeoserverUser() + ":" + geopolicy.getGeoserverPassword()).getBytes()));

			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			// Add the String Message converter
			messageConverters.add(new StringHttpMessageConverter());
			// Add the message converters to the restTemplate
			restTemplate.setMessageConverters(messageConverters);

			HttpEntity<String> entity = new HttpEntity<>(headers);

			String url = geopolicy.getGeoserverDataUrl() + "/rest/workspaces/" + geopolicy.getGeoserverDataWorkspace() + "/styles/" + designLayer.getDesignLayerKey() + ".sld";
			ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
			httpStatus = response.getStatusCode();
			layerStyleFileData = response.getBody().toString();
			log.info("-------- designLayerKey = {}, statusCode = {}, body = {}", designLayer.getDesignLayerKey(), response.getStatusCodeValue(), response.getBody());
		} catch (RestClientException e) {
		    LogMessageSupport.printMessage(e, "-------- RestClientException message = {}", e.getMessage());
			String message = e.getMessage();
			if (message.indexOf("404") >= 0) {
				httpStatus = HttpStatus.NOT_FOUND;
			} else {
				httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
			}
		} catch (Exception e) {
		    LogMessageSupport.printMessage(e, "-------- exception message = {}", e.getMessage());
			String message = e.getMessage();
			if (message.indexOf("404") >= 0) {
				httpStatus = HttpStatus.NOT_FOUND;
			} else {
				httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
			}
		}

		return layerStyleFileData;
	}
      
	/**
	 * 레이어 스타일 정보를 수정
	 * 
	 * @param geopolicy
	 * @param designLayer
	 * @throws Exception
	 */
	private void updateGeoserverLayerStyle(GeoPolicy geopolicy, DesignLayer designLayer) throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		// 클라이언트가 서버에 어떤 형식(MediaType)으로 달라는 요청을 할 수 있는데 이게 Accpet 헤더를 뜻함.
		List<MediaType> acceptList = new ArrayList<>();
		acceptList.add(MediaType.APPLICATION_JSON);
		headers.setAccept(acceptList);
		// 클라이언트가 request에 실어 보내는 데이타(body)의 형식(MediaType)를 표현
		headers.setContentType(new MediaType("application", "vnd.ogc.sld+xml"));
		// geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding
		headers.add("Authorization", "Basic " + Base64.getEncoder()
				.encodeToString((geopolicy.getGeoserverUser() + ":" + geopolicy.getGeoserverPassword()).getBytes()));

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		// Add the String Message converter
		messageConverters.add(new StringHttpMessageConverter());
		// Add the message converters to the restTemplate
		restTemplate.setMessageConverters(messageConverters);

		HttpEntity<String> entity = new HttpEntity<>(designLayer.getStyleFileContent().trim(), headers);

		String url = geopolicy.getGeoserverDataUrl() + "/rest/workspaces/" + geopolicy.getGeoserverDataWorkspace() + "/styles/" + designLayer.getDesignLayerKey();
		log.info("-------- url = {}, xmlData = {}", url, designLayer.getStyleFileContent().trim());
		ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
		log.info("-------- updateGeoserverLayerStyle statusCode = {}, body = {}", response.getStatusCodeValue(), response.getBody());
	}
       
	/**
	 * geoserver에 존재하는 레이어를 삭제
	 * 
	 * @param geopolicy
	 * @param designLayerKey
	 * @return
	 * @throws Exception
	 */
	private HttpStatus deleteGeoserverDesignLayer(GeoPolicy geopolicy, String designLayerKey) {
		HttpStatus httpStatus = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_XML);
			// geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding
			headers.add("Authorization", "Basic " + Base64.getEncoder()
					.encodeToString((geopolicy.getGeoserverUser() + ":" + geopolicy.getGeoserverPassword()).getBytes()));

			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			// Add the String Message converter
			messageConverters.add(new StringHttpMessageConverter());
			// Add the message converters to the restTemplate
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setMessageConverters(messageConverters);

			HttpEntity<String> entity = new HttpEntity<>(headers);

			String url = geopolicy.getGeoserverDataUrl() + "/rest/workspaces/" + geopolicy.getGeoserverDataWorkspace()
					+ "/datastores/" + geopolicy.getGeoserverDataStore() + "/featuretypes/" + designLayerKey + "?recurse=true";
			log.info("-------- url = {}", url);
			ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
			httpStatus = response.getStatusCode();
			log.info("-------- geoserver layer delete. designLayerKey = {}, statusCode = {}, body = {}", designLayerKey, response.getStatusCodeValue(), response.getBody());
		} catch (RestClientException e) {
		    LogMessageSupport.printMessage(e, "-------- RestClientException message = {}", e.getMessage());
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		} catch (Exception e) {
		    LogMessageSupport.printMessage(e, "-------- exception message = {}", e.getMessage());
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		return httpStatus;
	}
	
	private void deleteGeoserverLayerStyle(GeoPolicy policy, String designLayerKey) {
		HttpStatus httpStatus = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_XML);
			// geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding
			headers.add("Authorization", "Basic " + Base64.getEncoder()
					.encodeToString((policy.getGeoserverUser() + ":" + policy.getGeoserverPassword()).getBytes()));

			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			// Add the String Message converter
			messageConverters.add(new StringHttpMessageConverter());
			// Add the message converters to the restTemplate
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setMessageConverters(messageConverters);

			HttpEntity<String> entity = new HttpEntity<>(headers);

			String url = policy.getGeoserverDataUrl() + "/rest/workspaces/" + policy.getGeoserverDataWorkspace() + "/styles/" + designLayerKey + "?recurse=true";
			log.info("-------- url = {}", url);
			ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
			httpStatus = response.getStatusCode();
		} catch (RestClientException e) {
		    LogMessageSupport.printMessage(e, "-------- RestClientException message = {}", e.getMessage());
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		} catch (Exception e) {
		    LogMessageSupport.printMessage(e, "-------- exception message = {}", e.getMessage());
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		}
	}

    /**
     *
     * @param geopolicy
     * @param designLayer
     * @throws Exception
     */
	private void reloadGeoserverLayerStyle(GeoPolicy geopolicy, DesignLayer designLayer) throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		// 클라이언트가 서버에 어떤 형식(MediaType)으로 달라는 요청을 할 수 있는데 이게 Accpet 헤더를 뜻함.
		List<MediaType> acceptList = new ArrayList<>();
		acceptList.add(MediaType.APPLICATION_JSON);
		headers.setAccept(acceptList);
		// 클라이언트가 request에 실어 보내는 데이타(body)의 형식(MediaType)를 표현
		headers.setContentType(MediaType.TEXT_XML);
		// geoserver basic 암호화 아이디:비밀번호 를 base64로 encoding
		headers.add("Authorization", "Basic " + Base64.getEncoder()
				.encodeToString((geopolicy.getGeoserverUser() + ":" + geopolicy.getGeoserverPassword()).getBytes()));

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		// Add the String Message converter
		messageConverters.add(new StringHttpMessageConverter());
		// Add the message converters to the restTemplate
		restTemplate.setMessageConverters(messageConverters);

		HttpEntity<String> entity = new HttpEntity<>(getReloadLayerStyle(geopolicy.getGeoserverDataWorkspace(), designLayer.getDesignLayerKey()), headers);
		String url = geopolicy.getGeoserverDataUrl() + "/rest/layers/" + geopolicy.getGeoserverDataWorkspace() + ":" + designLayer.getDesignLayerKey();
		ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
		log.info("-------- statusCode = {}, body = {}", response.getStatusCodeValue(), response.getBody());
	}

	/**
	 * geoserver rest api 가 빈 파일을 등록하고 update 해야 함
	 * 
	 * @param designLayerKey
	 * @return
	 */
	private String getEmptyStyleFile(String designLayerKey) {
		
        String fileName = designLayerKey + ".sld";
        StringBuilder builder = new StringBuilder()
        		.append("<style>")
                .append("<name>" + designLayerKey + "</name>")
                .append("<filename>" + fileName + "</filename>")
                .append("</style>");
        return builder.toString();
	}

	/**
     *
	 */
	private String getReloadLayerStyle(String workspace, String designLayerKey) {
		
        StringBuilder builder = new StringBuilder()
                .append("<layer>")
                .append("<enabled>true</enabled>")
                .append("<defaultStyle>")
                .append("<name>" + designLayerKey + "</name>")
                .append("<workspace>" + workspace + "</workspace>")
                .append("</defaultStyle>")
                .append("</layer>");
        return builder.toString();
	}
}
