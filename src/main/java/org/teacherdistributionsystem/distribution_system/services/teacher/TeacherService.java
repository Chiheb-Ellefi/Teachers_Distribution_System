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
import org.teacherdistributionsystem.distribution_system.models.responses.teacher.TeacherResponse;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherRepository;


import java.util.ArrayList;
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

    public Map<String, Teacher> populateTeachersTable(Workbook workbook) {
        List<Teacher> teachers = new ArrayList<>();

        workbook.forEach(sheet -> {
            sheet.forEach(row -> {
                if (row.getRowNum() == 0) return;

                Teacher teacher = Teacher.builder()
                        .nom(getCellAsString(row, 0))
                        .prenom(getCellAsString(row, 1))
                        .email(getCellAsString(row, 2))
                        .gradeCode(getCellAsString(row, 3))
                        .codeSmartex(getCellAsInteger(row, 4))
                        .participeSurveillance(getCellAsBoolean(row, 5))
                        .quotaCredit(0)
                        .build();
                teachers.add(teacher);
            });
        });

        boolean isEmpty = teacherRepository.count() == 0;

        if (isEmpty) {
            // Table is empty - populate with all teachers from Excel
            List<Teacher> savedTeachers = teacherRepository.saveAll(teachers);
            return savedTeachers.stream()
                    .collect(Collectors.toMap(Teacher::getEmail, teacher -> teacher));
        } else {
            // Table has data - add only new teachers
            List<Integer> existingCodes = teacherRepository.findAllCodesSmartex();
            List<Teacher> newTeachers = teachers.stream()
                    .filter(t -> !existingCodes.contains(t.getCodeSmartex()))
                    .toList();

            if (!newTeachers.isEmpty()) {
                teacherRepository.saveAll(newTeachers);
            }

            // Return ALL teachers (existing + newly added)
            List<Teacher> allTeachers = teacherRepository.findAll();
            return allTeachers.stream()
                    .collect(Collectors.toMap(Teacher::getEmail, teacher -> teacher));
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

    public PageResponse<TeacherResponse> getAllTeachers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TeacherResponse> teacherPage = teacherRepository.getAllTeachers(pageable);
        Map<Long, Integer> quotaPerTeacher=teacherQuotaService.getAllQuotas();
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
}
