/**
 * 
 */
package lhdt.ds.common.misc;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import dev.hyunlab.core.PpTransferObject;
import dev.hyunlab.core.util.PpDateUtil;
import dev.hyunlab.core.util.PpUtil;

/**
 * 이것저것 유틸리티
 * @author gravity@daumsoft.com
 *
 */
public class DsUtils extends PpUtil{
	
	/**
	 * result map 생성
	 * @return
	 */
	public static Map<String,Object> createResultMap(){
		return createResultMap(HttpStatus.OK, null, null);
	}
	
	/**
	 * result map 생성
	 * @param statusCode
	 * @param errorCode
	 * @param message
	 * @return
	 */
	public static Map<String,Object> createResultMap(HttpStatus statusCode, String errorCode, String message){
		Map<String,Object> map = new HashMap<>();
		
		//
		map.put(DsConst.STATUS_CODE, statusCode);
		map.put(DsConst.ERROR_CODE, errorCode);
		map.put(DsConst.MESSAGE, message);
		
		//
		return map;
	}
	
	/**
	 * @see lhdt.anals.common.AnalsUtils.doGet(String, Map<String, Object>, Map<String, Object>)
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static PpTransferObject doGet(String url) throws Exception {
		return doGet(url, null, null);
	}
	
	
	
	/**
	 * @see lhdt.anals.common.AnalsUtils.doGet(String, Map<String, Object>, Map<String, Object>)
	 * @param url
	 * @param param
	 * @return
	 * @throws Exception
	 */
	public static PpTransferObject doGet(String url, Map<String,Object> param) throws Exception {
		return doGet(url, param, null);
	}
	
	/**
	 * get 방식으로 요청
	 * @param url
	 * @param param key/value
	 * @param requestProperty 헤더 key/value
	 * @return 리턴받은 값
	 * @throws Exception 
	 * @history
	 * 	1002 add requestProperty
	 */
	public static PpTransferObject doGet(String url, Map<String,Object> param, Map<String, Object> requestProperty) throws Exception {
		return doGetOrPost(HttpMethod.GET, url, param, requestProperty);
	}
	

	/**
	 * @see lhdt.anals.common.AnalsUtils.doPost(String, Map<String, Object>, Map<String, Object>)
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static PpTransferObject doPost(String url) throws Exception {
		return doPost(url, null, null);
	}
	
	
	/**
	 * @see lhdt.anals.common.AnalsUtils.doPost(String, Map<String, Object>, Map<String, Object>)
	 * @param url
	 * @param param
	 * @return
	 * @throws Exception
	 */
	public static PpTransferObject doPost(String url, Map<String,Object> param) throws Exception {
		return doPost(url, param, null);
	}
	
	/**
	 * post방식으로 요청
	 * @param url
	 * @param param
	 * @param requestProperty
	 * @return
	 * @throws Exception
	 */
	public static PpTransferObject doPost(String url, Map<String,Object> param, Map<String, Object> requestProperty) throws Exception {
		return doGetOrPost(HttpMethod.POST, url, param, requestProperty);
	}
	/**
	 * unique한  long값 생성 & 리턴
	 * @return
	 */
	public static Long getUniqueLong() {
		return (new Date()).getTime();
	}
	
	
	
	
	/**
	 * create HttpClient
	 * @return
	 */
	private static HttpClient createHttpClient() {
		//
		return HttpClient.newBuilder()
				.version(Version.HTTP_1_1)
				.connectTimeout(Duration.ofSeconds(10))
				.build();
	}
	

	/**
	 * create HttpRequest
	 * @param httpMethod
	 * @param url
	 * @param param
	 * @param requestProperty
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private static HttpRequest createHttpRequest(HttpMethod httpMethod,  String url, Map<String,Object> param, Map<String,Object> requestProperty) throws UnsupportedEncodingException {
		//
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.setHeader("User-Agent", "java 11 HttpClient Bot")
				.uri(URI.create(getFullUrl(httpMethod, url, param)));
		
		//
		switch (httpMethod) {
		case GET:
			requestBuilder.GET();
			break;

		default:
			requestBuilder.POST(ofFormData(param));
			break;
		}
		
		//
		if(DsUtils.isNotEmpty(requestProperty)) {
			for(Map.Entry<String, Object> entry : requestProperty.entrySet()) {
				requestBuilder.header(entry.getKey(), entry.getValue().toString());				
			}
		}
		
		//
		return requestBuilder.build();
	}
	
	
	
	/**
	 * 실제로  get/post 요청/응답하는 메소드
	 * @param httpMethod
	 * @param url
	 * @param param
	 * @param requestProperty
	 * @return PpTransferObject	resultCode=상태코드, resultMessage=응답문자열
	 * @throws Exception
	 */
	private static PpTransferObject doGetOrPost(HttpMethod httpMethod, String url, Map<String,Object> param, Map<String, Object> requestProperty) throws Exception {
		//
		HttpClient client = createHttpClient();
		
		//
		HttpRequest request = createHttpRequest(httpMethod, url, param, requestProperty);
		
		//
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		
		PpTransferObject trans = new PpTransferObject();
		trans.setResultCode(""+response.statusCode());
		trans.setResultMessage(response.body());
		
		//
		return trans;
	}
	
	
	/**
	 * url뒤에 파라미터 추가하여 full url생성
	 * @param httpMethod 
	 * @param url
	 * @param param
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private static String getFullUrl(HttpMethod httpMethod, String url, Map<String,Object> param) throws UnsupportedEncodingException {
		//
		String fullUrl = url + "?_" + PpDateUtil.getYmdhms();
		
		//
		if(DsUtils.isEmpty(param) || HttpMethod.POST == httpMethod){
			return fullUrl;
		}
		
		//
		for(Map.Entry<String, Object> d : param.entrySet()) {
			fullUrl += "&" + d.getKey() + "=" + URLEncoder.encode(""+d.getValue(), "UTF-8");
		}
		
		//
		return fullUrl;
	}
	
	
	/**
	 * post방식으로 데이터를 전송하기 위해 데이터를 변환
	 * @param param
	 * @return
	 */
	private static HttpRequest.BodyPublisher ofFormData(Map<String,Object> param){
		
		//
		if(DsUtils.isEmpty(param)) {
			return HttpRequest.BodyPublishers.ofString("");
		}
		
		//
		StringBuffer sb = new StringBuffer();
		
		for(Map.Entry<String, Object> entry : param.entrySet()) {
			if(0 < sb.length() ) {
				sb.append("&");
			}
			
			//
			sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
			sb.append("=");
			sb.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
		}
		
		//
		return HttpRequest.BodyPublishers.ofString(sb.toString());
	}


	/**
	 *
	 * @param klass Input Reflection Class
	 * @param order Input Order, 0 or 1
	 * @param <T>
	 * @return
	 */
	public static <T extends Enum<T> & DSEnum> Map<String, String> getEnum2Map(
			Class<T> klass, Integer order) {

		Map<String, String> vals = new HashMap<String, String>(0);

		try {
			Method m = klass.getMethod("values", null);
			Object obj = m.invoke(null, null);

			for (Enum<T> enumval : (Enum<T>[]) obj) {
				if(order == 0) {
					vals.put(enumval.toString(), enumval.name());
				} else {
					vals.put(enumval.name(), enumval.toString());
				}
			}

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return vals;
	}
}