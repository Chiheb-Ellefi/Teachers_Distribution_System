package org.teacherdistributionsystem.distribution_system.services.teacher;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import org.teacherdistributionsystem.distribution_system.dtos.teacher.TeacherDto;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.mappers.teacher.TeacherMapper;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherNameProjection;

import org.teacherdistributionsystem.distribution_system.models.responses.PageResponse;
import org.teacherdistributionsystem.distribution_system.models.responses.teacher.GradeCount;
import org.teacherdistributionsystem.distribution_system.models.responses.teacher.TeacherResponse;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherRepository;
import org.teacherdistributionsystem.distribution_system.utils.TeacherMaps;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.teacherdistributionsystem.distribution_system.utils.ExcelCellUtils.*;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    private final TeacherQuotaService teacherQuotaService;

        public TeacherMaps populateTeachersTable(Workbook workbook) {
            List<Teacher> teachers = new ArrayList<>();
            Map<String, String> abrvToEmailMap = new HashMap<>();

            workbook.forEach(sheet -> {
                sheet.forEach(row -> {
                    if (row.getRowNum() == 0) return;

                    String nom = getCellAsString(row, 0);
                    String prenom = getCellAsString(row, 1);
                    String abrv = getCellAsString(row, 2);
                    String email = getCellAsString(row, 3);
                    String gradeCode = getCellAsString(row, 4);
                    Integer codeSmartex = getCellAsInteger(row, 5);
                    Boolean participeSurveillance = getCellAsBoolean(row, 6);
                    System.out.println("Row " + row.getRowNum() + ": nom=" + nom + ", email=" + email + ", gradeCode=" + gradeCode);
                    Teacher teacher = Teacher.builder()
                            .nom(nom)
                            .prenom(prenom)
                            .email(email)
                            .gradeCode(gradeCode)
                            .codeSmartex(codeSmartex)
                            .participeSurveillance(participeSurveillance)
                            .quotaCredit(0)
                            .build();

                    teachers.add(teacher);

                    // Store abbreviation to email mapping for later lookup
                    if (abrv != null && !abrv.trim().isEmpty() && email != null) {
                        abrvToEmailMap.put(abrv.trim(), email);
                    }
                });
            });

            boolean isEmpty = teacherRepository.count() == 0;

            if (isEmpty) {
                List<Teacher> savedTeachers = teacherRepository.saveAll(teachers);
                Map<String, Teacher> emailMap = savedTeachers.stream()
                        .collect(Collectors.toMap(Teacher::getEmail, teacher -> teacher));

                return new TeacherMaps(emailMap, abrvToEmailMap);
            } else {
                List<String> existingEmails = teacherRepository.findAll().stream()
                        .map(Teacher::getEmail)
                        .toList();

                List<Teacher> newTeachers = teachers.stream()
                        .filter(t -> t.getEmail() != null && !existingEmails.contains(t.getEmail()))
                        .toList();

                if (!newTeachers.isEmpty()) {
                    teacherRepository.saveAll(newTeachers);
                }

                List<Teacher> allTeachers = teacherRepository.findAll();
                Map<String, Teacher> emailMap = allTeachers.stream()
                        .collect(Collectors.toMap(
                                Teacher::getEmail,
                                teacher -> teacher,
                                (existing, replacement) -> existing
                        ));

                return new TeacherMaps(emailMap, abrvToEmailMap);
            }
        }


    public Map<Long, Boolean>getTeacherParticipeSurveillance(){
        return teacherRepository.getAllParticipants().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Boolean) row[1]
                ));
    }
    public Map<Long, String>getAllGrades() {
        return teacherRepository.getAllGrades().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1]
                ));
    }
     public Map<Long, String>getAllEmails() {
        return teacherRepository.getAllEmails().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1]
                ));
    }
    public Map<Long, String> getAllNames() {
        return teacherRepository.getAllNames()
                .stream()
                .collect(Collectors.toMap(
                        TeacherNameProjection::getId,
                        t -> t.getPrenom() + " " + t.getNom()
                ));
    }

    public PageResponse<TeacherResponse> getAllTeachers(int page, int size,Long sessionId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TeacherResponse> teacherPage = teacherRepository.getAllTeachers(pageable);
        Map<Long, Integer> quotaPerTeacher=teacherQuotaService.getAllQuotas(sessionId);
        teacherPage.getContent().forEach(teacher -> {
            Integer quota=quotaPerTeacher.get(teacher.getId());
            Integer credit=teacher.getCredit();
           Integer total =teacher.getParticipeSurveillance()? (credit != null ? credit : 0) + (quota != null ? quota : 0):0;
            teacher.setQuota(total);
        });
        return new PageResponse<>(
                teacherPage.getContent(),
                teacherPage.getNumber(),
                teacherPage.getSize(),
                teacherPage.getTotalElements()
        );
    }

    public TeacherDto getTeacherDetails(Long teacherId) {
        Supplier<EntityNotFoundException> e=()->new EntityNotFoundException("Teacher with id " + teacherId + " not found");
        return TeacherMapper.toTeacherDto(teacherRepository.findById(teacherId).orElseThrow(e));
    }

    public List<TeacherDto> containsName(String name) {
        return teacherRepository.getAllContainsName(name).stream().map(TeacherMapper::toTeacherDto).collect(Collectors.toList());
    }

    public Long getTeachersCount() {
      return teacherRepository.count();
    }

    public List<GradeCount> teachersPerGrade() {
        return teacherRepository.countTeachersByGradeCode();
    }



    public void clearAllTeachers() {
        teacherRepository.deleteAll();
    }

  public   TeacherDto getTeacherById(Long teacherId) {
        return TeacherMapper.toTeacherDto(teacherRepository.findById(teacherId).orElseThrow(EntityNotFoundException::new));
    }
}
