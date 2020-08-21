/**
 * 
 */
package lhdt.ds.common.misc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import lhdt.ds.common.domain.DsDomain;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * 모든 service impl 의 부모
 * @author gravity@daumsoft.com
 *
 */
@Slf4j
public  class DsServiceImpl<JPA, MAPPER, DOMAIN, IDTYPE> implements DsService<DOMAIN, IDTYPE> {
	/**
	 * jpa
	 */
	private JpaRepository<DOMAIN, IDTYPE> jpaRepo;
	
	/**
	 * mybatis mapper
	 */
	private DsMapper<DOMAIN, IDTYPE> mapper;
	/**
	 * domain
	 */
	private DOMAIN domain;

	/**
	 * entity manager
	 */
	@PersistenceContext
	private EntityManager em;

	@Override
	public void delete(IDTYPE id) {
		//
		this.jpaRepo.deleteById(id);
	}

	@Override
	public void deleteAll(Iterable<IDTYPE> ids) {
		ids.forEach(x->{
			delete(x);
		});
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void deleteByBizKey(DOMAIN domain) {
		//
		DOMAIN domain2;
		try {
			domain2 = findByBizKey(domain);
			
			delete((IDTYPE)((DsDomain)domain2).getId());
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public List<DOMAIN> findAll() {
		return this.jpaRepo.findAll();
	}
	
	
	@Override 
	public Page<DOMAIN> findAll(Pageable pageable){
		return this.jpaRepo.findAll(pageable);
	}
	
	

	/**
	 * 페이징을 위한 메서드
	 * @autho break8524@vaiv.com
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public Page<DOMAIN> findAllPgByStartPg(Integer startPage, Integer contentsSize) {
		PageRequest pageRequest =
				PageRequest.of(startPage,
						contentsSize, Sort.Direction.DESC, "id");
		return this.findAll(pageRequest);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public DOMAIN findByBizKey(DOMAIN domain) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if(null == domain) {
			log.warn("<<.findByBizKey - null domain");
			return null;
		}

		//domain에 업무키 존재하지 않으면		
		if(!existsBizKey(domain)) {
			log.warn("<<.findByBizKey - not exists bizkey at {}", domain);
			return null;
		}

		//
		List<FieldAndOrder> bizFields = getBizFields(domain);
		Collections.sort(bizFields, new FieldAndOrderComparator());
		
		
		if(DsUtils.isEmpty(bizFields)) {
			log.warn("<<.findByBizKey - empty bizFields");
			return null;
		}
		
		//
		String methodName = getMethodName("findBy", bizFields);
		log.debug(".findByBizKey - methodName:{}", methodName);
		
		//
		Class<?>[] paramTypes = getParamTypes(bizFields);
		
		//
		Object[] paramValues = getParamValues(bizFields, domain);
		
		//
		if(existsMethod(methodName)) {
			//메소드 실행
			Object obj = this.jpaRepo
					.getClass()
					.getMethod(methodName, paramTypes)
					.invoke(this.jpaRepo, paramValues);
			
			//
			log.debug("<<.findByBizKey - {}", obj);
			return (DOMAIN)obj;
			
		}else {
			return findByBizKeyByCreateQuery(bizFields, paramValues);
		}
		

	}


	@Override
	public DOMAIN findById(IDTYPE id) {
		Optional<DOMAIN> vo = this.jpaRepo.findById(id);

		//
		return vo.orElse(null);
	}

	@Override
	public Integer getTotcnt() {
		return mapper.getTotcnt();
	}

	@Override
	public DOMAIN regist(DOMAIN domain) {
		//domain에 업무키 존재하면
		if(existsBizKey(domain)) {
			try {
				//테이블에서 업무키로 조회
				DOMAIN domain2 = findByBizKey(domain);
				
				//데이터 존재하면 업무키로  update
				if(null != domain2) {
					return updateByBizKey(domain);
				}
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}
		
		//insert
		return this.jpaRepo.save(domain);
	}

	@Override
	public DOMAIN update(IDTYPE id, DOMAIN domain) {
		DOMAIN domain2 = findById(id);
		if(null == domain2) {
			return null;
		}
		
		//domain의 값을 domain2에 overwrite하기, except id
		DsUtils.getFieldNames(domain).forEach(x->{
			if("id".equals(x)) {
				return;
			}
			
			//
			Object value = DsUtils.getFieldValue(domain, x);
			DsUtils.setFieldValue(domain2, x, value);
		});

		//
		return this.jpaRepo.save(domain2);
	}


	@SuppressWarnings("unchecked")
	@Override
	public void updateAll(Iterable<DOMAIN> domains) {
		if(DsUtils.isEmpty(domains)) {
			return;
		}
		
		//
		domains.forEach(x->{
			if(null == x) {
				return;
			}
			
			//
			update((IDTYPE)((DsDomain)x).getId(), x);
		});
	}
	
	
	@Override
	public void updateAll(Map<IDTYPE, DOMAIN> map) {
		if(DsUtils.isEmpty(map)) {
			return;
		}
		
		//
		for(Map.Entry<IDTYPE, DOMAIN> entry : map.entrySet()) {
			update(entry.getKey(), entry.getValue());
		}
	}
	
	
	@Override
	public DOMAIN updateByBizKey(DOMAIN domain) {
		//업무키 존재하지 않으면
		if(!existsBizKey(domain)) {
			return null;
		}

		//
		DOMAIN domain2;
		try {
			domain2 = findByBizKey(domain);
			if(null == domain2) {
				return null;
			}
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}

		//domain의 값을 domain2에 overwrite하기, except id
		DsUtils.getFieldNames(domain).forEach(fieldName->{
			if("id".equals(fieldName)) {
				return;
			}
			
			//
			if(hasOneToMany(fieldName) || hasManyToOne(fieldName)) {
				return;
			}
			
			//
			Object value = DsUtils.getFieldValue(domain, fieldName);
			DsUtils.setFieldValue(domain2, fieldName, value);
			log.debug("{} {} {}", fieldName, value, domain2);
		});

		//
		return this.jpaRepo.save(domain2);
	}
	
	


	private boolean hasOneToMany(String fieldName) {
		Field[] fields = domain.getClass().getDeclaredFields();
		
		for(Field f : fields) {
			if(!f.getName().equals(fieldName)) {
				continue;
			}
			
			//
			if(f.isAnnotationPresent(OneToMany.class)){
				return true;
			}
		}
		// TODO Auto-generated method stub
		return false;
	}

	private boolean hasManyToOne(String fieldName) {
		Field[] fields = domain.getClass().getDeclaredFields();
		
		for(Field f : fields) {
			if(!f.getName().equals(fieldName)) {
				continue;
			}
			
			//
			if(f.isAnnotationPresent(ManyToOne.class)){
				return true;
			}
		}
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * domain에 bizkey 존재 여부
	 * @param domain
	 * @return true(domain에  @AnalsField(bizKey=true) 존재) / false
	 */
	private boolean existsBizKey(DOMAIN domain){
		return (0 < getBizFields(domain).size());
	}
	
	
	

	/**
	 * japrepo에 methodName에 해당하는 메소드가 존재하는지 여부
	 * @param methodName
	 * @return
	 */
	private boolean existsMethod(String methodName) {
		Method[] methods = this.jpaRepo.getClass().getMethods();
		if(DsUtils.isEmpty(methods)) {
			return false;
		}
		
		//
		Method method = Arrays.stream(methods)
							.filter(m -> m.getName().equals(methodName))
							.findFirst()
							.orElse(null);
		
		return (null != method);
	}
	
	

	/**
	 * bizFields 로 동적으로 쿼리 생성 & 값 추출
	 * @param bizFields biz fields 목록
	 * @param paramValues 파라미터 값 배열
	 * @return
	 * @since	20200821	init
	 */
	@SuppressWarnings("unchecked")
	private DOMAIN findByBizKeyByCreateQuery(List<FieldAndOrder> bizFields, Object[] paramValues) {
//		String sql = getSql(getIdFields(domain), bizFields);
//		Query q = this.em.createQuery(sql);
//		
//		//파라미터 값 세팅
//		if(DsUtils.isNotEmpty(bizFields)) {
//			int i=0;
//			for(FieldAndOrder f : bizFields) {
//				q.setParameter(f.field.getName(), paramValues[i++]);
//			}
//		}
//		
//		//쿼리 실행
//		List<?> list = q.getResultList();
//		if(DsUtils.isEmpty(list)) {
//			return null;
//		}
		
		List<?> list = em.createQuery(getQuery(bizFields, paramValues)).getResultList();
		if(DsUtils.isEmpty(list)) {
			return null;
		}
		
		//
		return (DOMAIN) list.get(0);
	}
	
	

	
	
	/**
	 * 업무키 필드 목록 조회
	 * @param domain
	 * @return
	 */
	private List<FieldAndOrder> getBizFields(DOMAIN domain){
		//
		List<FieldAndOrder> bizFields = new ArrayList<>();
		
		//
		if(null == domain) {
			log.warn("<<.getBizFields - null domain");
			return bizFields;
		}

		//
		List<Field> fields = new ArrayList<>();
		DsUtils.getFieldsUpTo(domain.getClass(), fields);
		
		if(DsUtils.isEmpty(fields)) {
			log.warn("<<.getBizFields - empty fields");
			return bizFields;
		}

		//
		fields.forEach(f->{
			if(null == f) {
				return;
			}
			
			//
			DsField dsField = f.getAnnotation(DsField.class);
			
			if(null == dsField) {
				return;
			}
			
			if(!dsField.bizKey()) {
				return;
			}
			
			//컬럼명
			String column = f.getName();
			Column c = f.getAnnotation(Column.class);
			if(null != c) {
				column = c.name();
			}
			
			//업무키 존재하면 추가
			bizFields.add(FieldAndOrder.builder()
					.field(f)
					.column(column)
					.order(dsField.order())
					.build());
			
		});
		
		//
		log.debug("<<.getBizFields - {}", bizFields.size());
		return bizFields;
	}
	
	/**
	 * @Id 어노테이션이 존재하는 필드 목록 추출
	 * @param domain
	 * @return
	 */
	private List<FieldAndOrder> getIdFields(DOMAIN domain){
		//
		List<FieldAndOrder> idFields = new ArrayList<>();
		
		//
		if(null == domain) {
			return idFields;
		}
		
		//
		List<Field> fields = new ArrayList<>();
		DsUtils.getFieldsUpTo(domain.getClass(), fields);
		
		if(DsUtils.isEmpty(fields)) {
			return idFields;
		}
		//
		for(Field f : fields) {
			if(!f.isAnnotationPresent(Id.class)) {
				continue;
			}
		
			
			//
			String column = f.getName();
			if(f.isAnnotationPresent(Column.class)) {
				column = f.getAnnotation(Column.class).name();
			}
		
			
			//
			idFields.add(FieldAndOrder.builder()
					.field(f)
					.column(column)
					.order(0)
					.build());
		}

		//
		return idFields;
	}
	
	
	/**
	 * field명으로 메소드명(문자열) 생성 
	 * 메소드명 규칙 : pre + 필드명[ + And + 필드명...] 
	 * @param pre
	 * @param fields
	 * @return
	 */
	private String getMethodName(String pre, List<FieldAndOrder> fields) {
		String str="";
		for(FieldAndOrder e : fields) {
			e.field.setAccessible(true);

			if(DsUtils.isNotEmpty(str)) {
				str += "And";
			}
			str += DsUtils.capitalize(e.field.getName());
		}

		//
		return pre + str;		
	}
	/**
	 * field 목록으로 각 field의 파라미터 타입 배열 생성
	 * @param fields
	 * @return
	 */
	private Class<?>[] getParamTypes(List<FieldAndOrder> fields){
		if(DsUtils.isEmpty(fields)) {
			return new Class[] {};
		}

		//
		Class<?>[] classes = new Class[fields.size()];
		//
		for(int i=0; i<fields.size(); i++) {
			classes[i] = fields.get(i).field.getType();
		}

		//
//		log.debug("<<.getParamTypes - {}", classes);
		return classes;
	}
	/**
	 * field목록으로 파라미터 값 목록 생성
	 * @param list
	 * @param vo
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private Object[] getParamValues(List<FieldAndOrder> list, Object vo) throws IllegalArgumentException, IllegalAccessException{
		if(DsUtils.isEmpty(list)) {
			return new Object[] {};
		}

		//
		Object[] objects = new Object[list.size()];
		for(int i=0; i<list.size(); i++) {
			objects[i] = list.get(i).field.get(vo);
		}


		//
		log.debug("<<.getParamValues - {}", objects);
		return objects;
	}
	
	
	/**
	 * 동적으로 쿼리 생성
	 * @param bizFields
	 * @param paramValues
	 * @return
	 * @since 20200821
	 */
	@SuppressWarnings({ "unchecked" })
	private CriteriaQuery<DOMAIN> getQuery(List<FieldAndOrder> bizFields, Object[] paramValues) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<DOMAIN> q = (CriteriaQuery<DOMAIN>) builder.createQuery(domain.getClass());
		Root<DOMAIN> root = (Root<DOMAIN>) q.from(domain.getClass());
		q.select(root);
		
		//
		if(DsUtils.isNull(bizFields)) {
			em.createQuery(q).getResultList();
		}
		
		//
		int i=0;
		for(FieldAndOrder f : bizFields) {
			q.where(builder.equal(root.get(f.field.getName()), paramValues[i++]));
		}
		
		//
		return q;
	}

	/**
	 * 정상적으로 동작하기는 하나, 어느 정도의 하드코딩이 맘에 들지 않음.
	 * getQuery()사용하기를 추천함
	 * 동적으로 sql문 생성
	 * @param idFields @Id 필드 목록
	 * @param bizFields @DsField 필드 목록
	 * @return
	 */
	@Deprecated
	@SuppressWarnings("unused")
	private String getSql(List<FieldAndOrder> idFields, List<FieldAndOrder> bizFields) {
		
		
		String sql = "";
		
		sql = " SELECT 1 AS dummy";
		
		//get id column
		if(DsUtils.isNotEmpty(idFields)) {
			for(FieldAndOrder f : idFields) {
				sql += ", t." + f.field.getName();
			}
		}
		
		//
		if(DsUtils.isNotEmpty(bizFields)) {
			for(FieldAndOrder f : bizFields) {
				sql += ", t." + f.field.getName();
			}
		}
		
		//
		sql += " FROM " + this.domain.getClass().getSimpleName() + " t"; 
		sql += " WHERE 1=1";
		
		//
		if(DsUtils.isNotEmpty(bizFields)) {
			for(FieldAndOrder f : bizFields) {
				sql += " AND " + f.field.getName() + " = :" + f.field.getName();
			}
		}
		
		
		//
		return sql;
	}
	

	/**
	 * 초기값 세팅
	 * @param j
	 * @param m
	 * @param d
	 */
	@SuppressWarnings("unchecked")
	protected  void set(JPA j, MAPPER m, DOMAIN d) {
		this.jpaRepo = (JpaRepository<DOMAIN, IDTYPE>) j;
		this.mapper = (DsMapper<DOMAIN, IDTYPE>) m;
		this.domain = (DOMAIN) d;
	}
}


@Builder
class FieldAndOrder{
	Field field;
	/**
	 * table의 컬럼 명
	 */
	String column;
	int order;
}


/**
 * FieldAndOrder를 order순으로 정렬
 * @author gravity@daumsoft.com
 * @since 2020. 8. 10.
 *
 */
class FieldAndOrderComparator implements Comparator<FieldAndOrder>{

	@Override
	public int compare(FieldAndOrder o1, FieldAndOrder o2) {
		return o1.order - o2.order;
	}
	
}