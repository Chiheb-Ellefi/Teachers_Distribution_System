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

    public void addTeachersQuota(Workbook workbook, Map<String, Teacher> teacherMap, ExamSession session) {
        List<TeacherQuota> teacherQuotas = new ArrayList<>();

        workbook.forEach(sheet -> {
            System.out.println("QuotaService - Processing sheet: " + sheet.getSheetName());
            sheet.forEach(row -> {
                if (row.getRowNum() == 0) {
                    System.out.println("QuotaService - Skipping header row");
                    return;
                }

                // FIXED: Read email from column 3 (was column 2)
                String email = getCellAsString(row, 3);

                // FIXED: Read grade from column 4 (was column 3)
                String gradeCodeStr = getCellAsString(row, 4);

                System.out.println("QuotaService - Row " + row.getRowNum() +
                        ": email=" + email + ", gradeCode=" + gradeCodeStr);

                if (email == null || email.trim().isEmpty()) {
                    System.err.println("QuotaService - Empty email at row " + row.getRowNum());
                    return;
                }

                if (gradeCodeStr == null || gradeCodeStr.trim().isEmpty()) {
                    System.err.println("QuotaService - Empty grade code at row " + row.getRowNum());
                    return;
                }

                QuotaPerGradeDto quotaPerGrade;
                try {
                    quotaPerGrade = quotaPerGradeService.getQuotaByGrade(GradeType.fromCode(gradeCodeStr));
                } catch (IllegalArgumentException e) {
                    System.err.println("QuotaService - Invalid grade code: " + gradeCodeStr + " at row " + row.getRowNum());
                    throw new RuntimeException("Invalid grade code: " + gradeCodeStr, e);
                }

                Teacher teacher = teacherMap.get(email);
                if (teacher == null) {
                    System.err.println("QuotaService - Teacher not found for email: " + email + " at row " + row.getRowNum());
                    return;
                }

                Integer teacherQuota =quotaPerGrade.getDefaultQuota();
                QuotaType quotaType =QuotaType.STANDARD ;
                String message = teacher.getQuotaCredit() == 0
                        ? "Standard pour le grade : " + gradeCodeStr
                        : "Vous avez un crédit restant de la dernière session d'examens";

                TeacherQuota quota = TeacherQuota.builder()
                        .assignedQuota(teacherQuota)
                        .teacher(teacher)
                        .examSession(session)
                        .quotaType(quotaType)
                        .reason(message)
                        .build();

                teacherQuotas.add(quota);
            });
        });

        if (!teacherQuotas.isEmpty()) {
            teacherQuotaRepository.saveAll(teacherQuotas);
            System.out.println("QuotaService - Saved " + teacherQuotas.size() + " teacher quotas");
        }
    }
   public Map<Long, Integer> getAllQuotas(Long sessionId) {
       return teacherQuotaRepository.getTeacherQuotaAndId(sessionId).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Integer) row[1]
                ));

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

    public void updateQuotaPerGrade(String grade,Integer quota){
        teacherQuotaRepository.updateQuotaPerGrade(grade,quota);
    }
}
