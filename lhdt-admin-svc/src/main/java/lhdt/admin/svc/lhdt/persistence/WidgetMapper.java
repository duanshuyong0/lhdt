package lhdt.admin.svc.lhdt.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import lhdt.admin.svc.config.LhdtConnMapper;
import lhdt.admin.svc.lhdt.domain.Widget;

/**
 * 위젯
 * @author jeongdae
 *
 */
@LhdtConnMapper
public interface WidgetMapper {

	/**
	 * widgetId 최대값
	 * @return
	 */
	Long getMaxWidgetId();
	
	/**
	 * 위젯 목록
	 * @param widget
	 * @return
	 */
	List<Widget> getListWidget(Widget widget);
	
	/**
	 * 위젯 저장
	 * @param widget
	 * @return
	 */
	int insertWidget(Widget widget);
	
	/**
	 * 위젯 수정
	 * @param widget
	 * @return
	 */
	int updateWidget(Widget widget);
}
