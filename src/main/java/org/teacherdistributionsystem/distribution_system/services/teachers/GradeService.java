package org.teacherdistributionsystem.distribution_system.services.teachers;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.teacherdistributionsystem.distribution_system.dtos.teacher.QuotaPerGradeDto;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Grade;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.GradeTypeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.teacherdistributionsystem.distribution_system.utils.ExcelCellUtils.getCellAsString;

@Service
@RequiredArgsConstructor
public class GradeService {
    private final GradeTypeRepository gradeTypeRepository;
    private final QuotaPerGradeService quotaPerGradeService;



    public void addGrades(Workbook workbook) {
        List<Grade> grades=new ArrayList<>();
        Map<GradeType,Integer> priorityPerGradeMap=quotaPerGradeService.getPrioritiesByGrade();
       workbook.forEach(sheet -> {
           sheet.forEach(row -> {
               if (row.getRowNum() == 0) return;
               QuotaPerGradeDto quotaPerGrade;
               GradeType grade=GradeType.fromCode(getCellAsString(row,3));
               try {
                   quotaPerGrade = quotaPerGradeService.getQuotaByGrade(grade);
               } catch (IllegalArgumentException  e) {
                   throw new RuntimeException(e);
               }
               Grade gradeType= Grade.builder()
                       .gradeCode(getCellAsString(row,3))
                       .gradeLibelle(grade.getLabel())
                       .defaultQuotaPerSession(quotaPerGrade.getDefaultQuota())
                       .priorityLevel(priorityPerGradeMap.get(grade))
                       .build();
               grades.add(gradeType);
               gradeTypeRepository.saveAll(grades);

           });
       });

    }

    @Transactional
    public void clearAllGrades() {
        gradeTypeRepository.deleteAllInBatch();
    }

}
