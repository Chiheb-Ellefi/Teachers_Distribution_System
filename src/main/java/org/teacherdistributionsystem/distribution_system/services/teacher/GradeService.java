package org.teacherdistributionsystem.distribution_system.services.teacher;

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
        List<Grade> grades = new ArrayList<>();
        Map<GradeType, Integer> priorityPerGradeMap = quotaPerGradeService.getPrioritiesByGrade();

        workbook.forEach(sheet -> {
            sheet.forEach(row -> {
                if (row.getRowNum() == 0) return;

                // FIXED: Read from column 4 instead of column 3
                String gradeCodeStr = getCellAsString(row, 4);

                if (gradeCodeStr == null || gradeCodeStr.trim().isEmpty()) {
                    System.err.println("Empty grade code at row " + row.getRowNum());
                    return;
                }

                QuotaPerGradeDto quotaPerGrade;
                GradeType grade;

                try {
                    grade = GradeType.fromCode(gradeCodeStr);
                    quotaPerGrade = quotaPerGradeService.getQuotaByGrade(grade);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid grade code: " + gradeCodeStr + " at row " + row.getRowNum());
                    throw new RuntimeException("Invalid grade code: " + gradeCodeStr, e);
                }

                Grade gradeType = Grade.builder()
                        .gradeCode(gradeCodeStr)
                        .gradeLibelle(grade.getLabel())
                        .defaultQuotaPerSession(quotaPerGrade.getDefaultQuota())
                        .priorityLevel(priorityPerGradeMap.get(grade))
                        .build();

                grades.add(gradeType);
            });
        });

        // Save all grades at once (moved outside the loop for better performance)
        if (!grades.isEmpty()) {
            gradeTypeRepository.saveAll(grades);
        }
    }

    @Transactional
    public void clearAllGrades() {
        gradeTypeRepository.deleteAllInBatch();
    }

}
