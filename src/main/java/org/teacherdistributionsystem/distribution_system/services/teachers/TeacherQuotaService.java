package org.teacherdistributionsystem.distribution_system.services.teachers;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.dtos.teacher.QuotaPerGradeDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherQuota;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;

import org.teacherdistributionsystem.distribution_system.enums.QuotaType;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherQuotaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.teacherdistributionsystem.distribution_system.utils.ExcelCellUtils.getCellAsString;

@Service
@RequiredArgsConstructor
public class TeacherQuotaService {
    private final TeacherQuotaRepository teacherQuotaRepository;
    private final QuotaPerGradeService quotaPerGradeService;

    public void addTeachersQuota( Workbook workbook, Map<String,Teacher> teacherMap,ExamSession session) {
        List<TeacherQuota> teacherQuotas=new ArrayList<>();

      workbook.forEach(sheet -> {
          sheet.forEach(row -> {
              if (row.getRowNum() == 0) return ;
              QuotaPerGradeDto quotaPerGrade;
              try {
                  quotaPerGrade = quotaPerGradeService.getQuotaByGrade(GradeType.fromCode(getCellAsString(row,3)));
              } catch (BadRequestException e) {
                  throw new RuntimeException(e);
              }
              TeacherQuota quota=TeacherQuota.builder()
                      .assignedQuota(quotaPerGrade.getDefaultQuota())
                      .teacher(teacherMap.get(getCellAsString(row,2)))
                      .examSession(session)
                      .quotaType(QuotaType.STANDARD)
                      .reason("Standard pour le grade : " + getCellAsString(row,3))
                      .build();
              teacherQuotas.add(quota);
          });
      });

        teacherQuotaRepository.saveAll(teacherQuotas);

    }
   public Map<Long, Integer> getAllQuotas() {
       return teacherQuotaRepository.getTeacherQuotaAndId().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Integer) row[1]
                ));

    }
}
