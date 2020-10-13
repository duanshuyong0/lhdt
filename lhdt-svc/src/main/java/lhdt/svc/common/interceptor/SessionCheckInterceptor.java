/**
 * 
 */
package lhdt.svc.common.interceptor;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 세션 검사 interceptor
 * @author gravity
 *
 */
@Slf4j
@Component
public class SessionCheckInterceptor extends HandlerInterceptorAdapter {
	
	@PostConstruct
	private void init() {
		log.info("<< {}", this);
	}
	

	/**
	 * 초기화 시점 이벤트 감지
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		
		//TODO

		return true;
	}

	/**
	 * 모든 전송 상태 시점 이벤트 감지
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
	}

	/**
	 *  전송 상태 이후 시점 이벤트 감지
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {
	}

}
