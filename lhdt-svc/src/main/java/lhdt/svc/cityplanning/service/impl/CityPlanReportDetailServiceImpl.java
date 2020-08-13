package lhdt.svc.cityplanning.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lhdt.svc.cityplanning.domain.CityPlanReportDetail;
import lhdt.svc.cityplanning.persistence.CityPlanReportDetailRepository;
import lhdt.svc.cityplanning.service.CityPlanReportDetailService;

@Service
public class CityPlanReportDetailServiceImpl extends CityPlanReportDetailService {

    @Autowired
    CityPlanReportDetailRepository cityPlanReportDetailRepository;

    @Override
    public CityPlanReportDetail save(CityPlanReportDetail vo) {
        return this.cityPlanReportDetailRepository.save(vo);
    }

    @Override
    public List<CityPlanReportDetail> findAll() {
        ArrayList<CityPlanReportDetail> result = new ArrayList<>();
        this.cityPlanReportDetailRepository.findAll().forEach(result::add);
        return result;
    }

    @Override
    public CityPlanReportDetail findById(Long id) {
        return this.cityPlanReportDetailRepository.findById(id).orElse(null);
    }

    @Override
    public List<CityPlanReportDetail> findAllById(Long id) {
        ArrayList<CityPlanReportDetail> result = new ArrayList<>();
        this.cityPlanReportDetailRepository.findAll().forEach(result::add);
        return result;
    }

    @Override
    public boolean existVoByUk(CityPlanReportDetail vo) {
        return this.cityPlanReportDetailRepository.existsByIdAndCityPlanId(vo.getId(), vo.getCityPlanId());
    }

    @Override
    public CityPlanReportDetail findByUk(CityPlanReportDetail vo) {
        return this.cityPlanReportDetailRepository.findByIdAndCityPlanId(vo.getId(), vo.getCityPlanId());
    }

    @Override
    public List<CityPlanReportDetail> findAllByUk(CityPlanReportDetail vo) {
        return this.cityPlanReportDetailRepository.findAllByIdAndCityPlanId(vo.getId(), vo.getCityPlanId());
    }

    @Override
    public void deleteByVo(CityPlanReportDetail vo) {
        this.cityPlanReportDetailRepository.delete(vo);
    }
}
