/**
 * 
 */
package lhdt.anals.hello.controller;

import java.io.IOException;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import dev.hyunlab.core.PpTransferObject;
import lhdt.anals.common.AnalsController;
import lhdt.anals.common.AnalsUtils;
import lhdt.anals.hello.domain.Child;
import lhdt.anals.hello.domain.Hello;
import lhdt.anals.hello.domain.SubType0;
import lhdt.anals.hello.service.HelloService;
import lhdt.anals.hello.service.SampleService;
import lhdt.anals.hello.types.DefaultType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * sample controller
 * @author gravity@daumsoft.com
 *
 */
@Slf4j
@RestController
@RequestMapping("/hello/")
public class HelloController extends AnalsController {
	@Autowired
	private HelloService service;

	@Autowired
	private SampleService sampleService;

	@GetMapping()
	public ResponseEntity<Map<String,Object>> helloWorld() throws Exception{
		
		//
		Map<String,Object> map = new HashMap<>();
		
		//등록
		Hello afterRegistDomain = service.regist(Hello.builder().helloGroupId("abcd").helloGroupNo(1L) .build());
		log.debug("afterRegistDomain:{}", afterRegistDomain);
		
		
		//n건 조회
		List<Hello> hellos = (List<Hello>) service.findAll();
		log.debug("hellos:{} {}", hellos.size(), hellos);
		
		
		//pk로 1건 조회
		Hello hello = service.findById(41L);
		log.debug("hello:{}", hello);
				
		
		//1건 수정
		hello.setCn("내용 내용");
		service.update(hello.getId(), hello);
		
		
		//domain을  Set에 추가. n건 수정
		Set<Hello> domains = new HashSet<>();
		domains.add(hello);
		service.updateAll(domains);
		
		
		//domain을 맵에 추가. n건 수정
		Map<Long, Hello> domainMap = new HashMap<>();
		domainMap.put(hello.getId(), hello);
		service.updateAll(domainMap);
		
		
		//업무키로 조회
		Hello bizHello = service.findByBizKey(Hello.builder().helloGroupId("abcd").helloGroupNo(1L).build());
		log.debug("bizHello:{}", bizHello);
		
		//
		PpTransferObject trans = AnalsUtils.doGet("http://www.daumsoft.com");
		log.debug("{}", trans.getResultMessage());
		
		return super.res(bizHello);
		
		//
//		Long helloId = AnalsUtils.getUniqueLong();
//		
//		//
//		Hello h1 = Hello.builder()
//				.helloName("hello world")
//				.cn(new Date().toString())
//				.build();
//		
//		
//		//등록 by jpa
//		Hello saveResult = service.regist(h1);
//		map.put("saveResult", saveResult);
//		
//		//수정 by jpa
//		service.updt(h1);
//		
//		//조회 by mybatis
//		Hello data = service.findById(helloId);
//		map.put(AnalsConst.DATA, data);
//		
//		//전체목록 by mybatis
//		List<Hello> datas = service.findAll();
//		map.put(AnalsConst.DATAS, datas);
//		
//		//검색,페이징 조건
//		Hello searchHello = new Hello();
//		searchHello.setSearchHelloName("world");
//		searchHello.setOffset(0); //0부터 시작
//		searchHello.setPageSize(10);
//		
//		//검색 건수
//		int totcnt = service.findTotcnt(searchHello);
//		map.put(AnalsConst.TOTCNT, totcnt);
//		
//		//페이징
//		List<Hello> pagingDatas = service.findByPage(searchHello);
//		map.put("pagingDatas", pagingDatas);
		
		//
//		return super.res(map);
	}

	/**
	 * 모든 SybType 정보를 가지고 옵니다
	 * @return
	 */
	@GetMapping("/select")
	public List<SubType0> selectExample() {
		return this.sampleService.findAll();
	}


	/**
	 * 하나의 SybType 정보를 가지고 옵니다
	 * @return
	 */
	@GetMapping("/select2")
	public SubType0 select2Example() {
		var findAll = this.sampleService.findAll().get(0);
		return findAll;
	}

	/**
	 * Uk에 대한 SubType이 존재하는지 확인합니다
	 * @return
	 */
	@GetMapping("/exists")
	public boolean existsExample() {
		var sp = new SubType0();
		sp.setHelloName("a");
		sp.setCn("a");
		return this.sampleService.existVoByUk(sp);
	}

	/**
	 * 하나의 데이터를 입력합니다
	 * @return
	 */
	@GetMapping("/insert")
	public SubType0 insertExample() {
		var sp = new SubType0();
		sp.setHelloName("김도현");
		sp.setCn("a");
		sp.setDefaultType(DefaultType.First);
		var p = this.sampleService.registByUk(sp);
		if (p == null) {
			return null;
		} else {
			return p;
		}
	}


	/**
	 * 1: M 관계의 데이터를 입력합니다
	 * @return
	 */
	@GetMapping("/insert2")
	public SubType0 insert2Example() {
		var sp = new SubType0();
		sp.setHelloName("a");
		sp.setCn("a");
		for(var i = 0; i < 10; i++) {
			var ch = new Child();
			ch.setText("hi! ==> " + i);
			sp.addChild(ch);
		}
		var p = this.sampleService.registByUk(sp);
		if (p == null) {
			return null;
		} else {
			return p;
		}
	}

	/**
	 * 특정 데이터를 업데이트합니다
	 * @return
	 */
	@GetMapping("/update")
	public SubType0 updateExample() {
		var sp = new SubType0();
		sp.setHelloName("a");
		var p = this.sampleService.findByUk(sp);
		p.setHelloName("p");
		p = this.sampleService.update(p);
		if(p == null) {
			return null;
		} else {
			return p;
		}
	}

	/**
	 * 데이터를 삭제합니다
	 * @return
	 */
	@GetMapping("/delete")
	public String deleteExample() {
		var sp = new SubType0();
		sp.setHelloName("a");
		this.sampleService.deleteAllByUk(sp);
		return "1";
	}
}
