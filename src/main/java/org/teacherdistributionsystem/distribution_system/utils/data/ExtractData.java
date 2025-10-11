package org.teacherdistributionsystem.distribution_system.utils.data;


import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.io.FileInputStream;
import java.io.IOException;

@Component
public class ExtractData {
    @Value("${spring.application.fileLocation}")
    private  String fileLocation;
    @PostConstruct
 void getDataFromExcel() throws IOException {
     FileInputStream file = new FileInputStream("/home/chihebellefi/Downloads/ens_dist/Enseigants--Participation aux surveillances.xlsx");
     Workbook workbook = new XSSFWorkbook(file);
     Sheet sheet = workbook.getSheetAt(0);
     sheet.forEach(row -> {
         row.forEach(cell -> {
            if(cell.getCellType()== CellType.STRING)
             System.out.println(cell.getStringCellValue());
         });
     });


 }
}
