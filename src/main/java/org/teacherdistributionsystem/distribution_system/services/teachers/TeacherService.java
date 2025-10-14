package org.teacherdistributionsystem.distribution_system.services.teachers;

import org.apache.poi.ss.usermodel.Workbook;


import org.springframework.stereotype.Service;

import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherNameProjection;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherRepository;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.teacherdistributionsystem.distribution_system.utils.ExcelCellUtils.*;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;

    }

   public Map<String,Teacher> populateTeachersTable( Workbook workbook)  {
        List<Teacher> teachers=new ArrayList<>();
   workbook.forEach(sheet -> {
       sheet.forEach(row -> {
           if (row.getRowNum() == 0) return;

           Teacher teacher = Teacher.builder()
                   .nom(getCellAsString(row,0))
                   .prenom(getCellAsString(row,1))
                   .email(getCellAsString(row,2))
                   .gradeCode(getCellAsString(row,3))
                   .codeSmartex(getCellAsInteger(row,4))
                   .participeSurveillance(getCellAsBoolean(row,5))
                   .build();
           teachers.add(teacher);
       });
   });

       List<Teacher> savedTeachers=  teacherRepository.saveAll(teachers);
       return savedTeachers.stream().collect(Collectors.toMap(Teacher::getEmail, teacher -> teacher));
    }

    public List<Long> getTeachersId() {
        return teacherRepository.getAllIds();
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

}
