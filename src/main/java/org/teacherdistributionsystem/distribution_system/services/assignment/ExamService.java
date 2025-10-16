package org.teacherdistributionsystem.distribution_system.services.assignment;


import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.teacherdistributionsystem.distribution_system.entities.assignment.Exam;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;
import org.teacherdistributionsystem.distribution_system.models.keys.ExamKey;
import org.teacherdistributionsystem.distribution_system.models.projections.ExamForAssignmentProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.ExamProjection;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.ExamRepository;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import java.util.function.Function;
import java.util.stream.Collectors;

import static org.teacherdistributionsystem.distribution_system.utils.ExcelCellUtils.getCellAsString;
import static org.teacherdistributionsystem.distribution_system.utils.HelperMethods.getLocalDate;
import static org.teacherdistributionsystem.distribution_system.utils.HelperMethods.getLocalTime;

@Service
public class ExamService {
    private final ExamRepository examRepository;
    private final TeacherRepository teacherRepository;

    public ExamService(ExamRepository examRepository, TeacherRepository teacherRepository) {
        this.examRepository = examRepository;
        this.teacherRepository = teacherRepository;
    }
    public void addExams(Workbook workbook, ExamSession examSession) {
        List<Exam> examList = new ArrayList<>();
        Set<ExamKey> seenExams = new HashSet<>();
        Map<Integer, Teacher> teacherMap = teacherRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Teacher::getCodeSmartex, Function.identity()));
        workbook.forEach(sheet -> {
            sheet.forEach(row -> {
                if (row.getRowNum() == 0) return;

                String codeStr = getCellAsString(row, 6);
                if (codeStr.isEmpty()) return;

                int codeSmartex;
                try {
                    codeSmartex = Integer.parseInt(codeStr);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid code in row " + row.getRowNum() + ": " + codeStr);
                    return;
                }

                Teacher teacher = teacherMap.get(codeSmartex);
                if (teacher == null) {
                    System.err.println("No teacher found for code: " + codeSmartex);
                    return;
                }

                LocalDate examDate = getLocalDate(getCellAsString(row, 0));
                LocalTime startTime = getLocalTime(getCellAsString(row, 1));
                LocalTime endTime = getLocalTime(getCellAsString(row, 2));
                assert examSession != null;
                int jourNumero = (int) ChronoUnit.DAYS.between(examSession.getStartDate(), examDate) + 1;

                ExamKey examKey = new ExamKey(
                        examDate,
                        startTime,
                        endTime,
                        getCellAsString(row, 4),
                        teacher.getId(),
                        getCellAsString(row, 7)
                );


                if (seenExams.contains(examKey)) {
                    return;
                }

                seenExams.add(examKey);

                Exam exam = Exam.builder()
                        .examDate(examDate)
                        .startTime(startTime)
                        .endTime(endTime)
                        .examSession(examSession)
                        .examType(getCellAsString(row, 4))
                        .responsable(teacher)
                        .numRooms(getCellAsString(row, 7))
                        .requiredSupervisors(2)
                        .seance(SeanceType.fromTime(startTime))
                        .jourNumero(jourNumero)
                        .build();

                examList.add(exam);
            });
        });

        examRepository.saveAll(examList);
    }

    public List<ExamForAssignmentProjection> getExamsForAssignment(Long sessionId) {
        return examRepository.getExamsBySessionIdForAssignment(sessionId);

    }
    public List<ExamProjection> getExam(Long sessionId) {
        return examRepository.getExamsBySessionId(sessionId);

    }
    public void updateRequiredSupervisors(String examId, Integer requiredSupervisors) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        examRepository.updateRequiredSupervisorsForRelatedExams(
                exam.getExamSession().getId(),
                exam.getJourNumero(),
                exam.getSeance(),
                exam.getNumRooms(),
                requiredSupervisors
        );
    }

    @Transactional
    public void clearAllExams() {
        examRepository.deleteAllInBatch();
    }

    public Boolean dataExists(){
       return  examRepository.count()!=0;
    }

}
