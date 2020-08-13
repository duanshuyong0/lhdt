package lhdt.svc.cityplanning.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import lhdt.svc.cityplanning.domain.CityPlanReportDetail;

public interface CityPlanReportParserService {
    protected ArrayList<List<String>> readExcelData(String fileName) throws IOException, InvalidFormatException {
        ArrayList<List<String>> resultCol = new ArrayList<>();

        Workbook wb = WorkbookFactory.create(new File(fileName)); // Or .xlsx
        Sheet s = wb.getSheetAt(0);
        Integer lastRowNum = s.getLastRowNum();
        for(int i = 1; i < lastRowNum; i++) {
            Row r1 = s.getRow(i);
            Short lastCelNum = r1.getLastCellNum();
            List<String> row = new ArrayList<String>();
            for(int j = 0; j < lastCelNum; j++) {
                var p = r1.getCell(j).toString();
                row.add(p);
            }
            resultCol.add(row);
        }
        return resultCol;
    }

    protected abstract List<CityPlanReportDetail> procExcelDataObj(ArrayList<List<String>> excelDatas);

    public List<CityPlanReportDetail> procExcelDataByCityPlan(String fileName) throws IOException, InvalidFormatException {
        var p = readExcelData(fileName);
        var resultCol = procExcelDataObj(p);
        return resultCol;
    }

}
