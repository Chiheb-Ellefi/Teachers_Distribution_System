package org.teacherdistributionsystem.distribution_system.services.teachers;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Grade;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.GradeTypeRepository;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.teacherdistributionsystem.distribution_system.utils.data.ExcelCellUtils.getCellAsString;

@Service
public class GradeService {
    private final GradeTypeRepository gradeTypeRepository;

    public GradeService(GradeTypeRepository gradeTypeRepository) {
        this.gradeTypeRepository = gradeTypeRepository;
    }

    public void addGrades(Workbook workbook) {
        List<Grade> grades=new ArrayList<>();
       workbook.forEach(sheet -> {
           sheet.forEach(row -> {
               if (row.getRowNum() == 0) return;
               Grade gradeType= Grade.builder()
                       .gradeCode(getCellAsString(row,3))
                       .gradeLibelle(GradeType.fromCode(getCellAsString(row,3)).getLabel())
                       .defaultQuotaPerSession(GradeType.fromCode(getCellAsString(row,3)).getDefaultQuota())
                       .priorityLevel(GradeType.fromCode(getCellAsString(row,3)).getPriority())
                       .build();
               grades.add(gradeType);
               gradeTypeRepository.saveAll(grades);

           });
       });

    }


}
