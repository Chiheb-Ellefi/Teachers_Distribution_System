package org.teacherdistributionsystem.distribution_system.services.teachers;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherPreference;
import org.teacherdistributionsystem.distribution_system.enums.PreferenceType;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherPreferenceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.teacherdistributionsystem.distribution_system.utils.ExcelCellUtils.getCellAsString;

@Service
public class TeacherPreferenceService {
    public final TeacherPreferenceRepository teacherPreferenceRepository;
    public TeacherPreferenceService(TeacherPreferenceRepository teacherPreferenceRepository) {
        this.teacherPreferenceRepository = teacherPreferenceRepository;
    }


    public void addTeachersPreference( Workbook workbook, Map<String, Teacher> teacherMap,ExamSession examSession)  {
        List<TeacherPreference> teacherPreferences=new ArrayList<>();

         workbook.forEach(sheet -> {
             sheet.forEach(row -> {
                 if (row.getRowNum() == 0) return;
                 TeacherPreference teacherPreference=TeacherPreference.builder()
                         .examSession(examSession)
                         .teacher(teacherMap.get(getCellAsString(row,2)))
                         .preferenceTypes(List.of(PreferenceType.NOTHING))
                         .priorityWeight(0)
                         .build();
                 teacherPreferences.add(teacherPreference);

             });
         });

        teacherPreferenceRepository.saveAll(teacherPreferences);
    }
}
