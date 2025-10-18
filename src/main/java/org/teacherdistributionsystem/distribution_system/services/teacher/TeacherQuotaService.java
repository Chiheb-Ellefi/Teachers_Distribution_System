package org.teacherdistributionsystem.distribution_system.services.teacher;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.teacherdistributionsystem.distribution_system.dtos.teacher.QuotaPerGradeDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherQuota;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;

import org.teacherdistributionsystem.distribution_system.enums.QuotaType;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.QuotaPerGradeRepository;
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
    private final QuotaPerGradeRepository quotaPerGradeRepository;

    public void addTeachersQuota( Workbook workbook, Map<String,Teacher> teacherMap,ExamSession session) {
        List<TeacherQuota> teacherQuotas=new ArrayList<>();

      workbook.forEach(sheet -> {
          sheet.forEach(row -> {
              if (row.getRowNum() == 0) return ;
              QuotaPerGradeDto quotaPerGrade;
              try {
                  quotaPerGrade = quotaPerGradeService.getQuotaByGrade(GradeType.fromCode(getCellAsString(row,3)));
              } catch (IllegalArgumentException e) {
                  throw new RuntimeException(e);
              }
              Teacher teacher=teacherMap.get(getCellAsString(row,2));
              Integer teacherQuota=teacher.getQuotaCredit()+quotaPerGrade.getDefaultQuota();
              QuotaType quotaType=teacher.getQuotaCredit()==0?QuotaType.STANDARD:QuotaType.INCREASED;
              String message=teacher.getQuotaCredit()==0?"Standard pour le grade : " + getCellAsString(row,3):"Incrementer a cause d'un credit d'une session précédente";
              TeacherQuota quota=TeacherQuota.builder()
                      .assignedQuota(teacherQuota)
                      .teacher(teacher)
                      .examSession(session)
                      .quotaType(quotaType)
                      .reason(message)
                      .build();
              teacherQuotas.add(quota);
          });
      });

        teacherQuotaRepository.saveAll(teacherQuotas);

    }
   public Map<Long, Integer> getAllQuotas(Long sessionId) {
       Map<Long, Integer> quotas=teacherQuotaRepository.getTeacherQuotaAndId(sessionId).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Integer) row[1]
                ));
       quotas.forEach( (id, quota) -> {
           System.out.println(id + " " + quota);
       });
return quotas;
    }
    @Transactional
    public void updateTeacherQuota(Long teacherId, Integer quota) {
        if(teacherId==null){
            throw new IllegalArgumentException("teacherId cannot be null");
        }
        if(quota==null){
            throw new IllegalArgumentException("quota cannot be null");
        }
        teacherQuotaRepository.updateTeacherQuotaById(teacherId,quota);
    }
    @Transactional
    public void clearAllQuotas(Long sessionId) {
        teacherQuotaRepository.deleteAllInBatchByExamSession_Id(sessionId);
    }
}
