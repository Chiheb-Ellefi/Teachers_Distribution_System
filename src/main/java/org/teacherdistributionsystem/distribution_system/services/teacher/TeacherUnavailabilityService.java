package org.teacherdistributionsystem.distribution_system.services.teacher;


import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherUnavailability;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilitiesProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilityProjection;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherRepository;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherUnavailabilityRepository;
import org.teacherdistributionsystem.distribution_system.models.keys.TeacherKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.teacherdistributionsystem.distribution_system.utils.ExcelCellUtils.getCellAsInteger;
import static org.teacherdistributionsystem.distribution_system.utils.ExcelCellUtils.getCellAsString;

@Service
public class TeacherUnavailabilityService {
    private final TeacherUnavailabilityRepository teacherUnavailabilitRepository;
    private final TeacherRepository teacherRepository;
    public TeacherUnavailabilityService(TeacherUnavailabilityRepository teacherUnavailabilitRepository, TeacherRepository teacherRepository) {
        this.teacherUnavailabilitRepository = teacherUnavailabilitRepository;
        this.teacherRepository = teacherRepository;
    }
    public void addTeachersUnavailability( Workbook workbook, ExamSession session) {

        Map<TeacherKey, Teacher> teacherMap = teacherRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        t -> new TeacherKey(t.getNom(), t.getPrenom()),
                        t -> t,
                        (existing, duplicate) -> {
                            System.err.println("WARNING: Duplicate teacher name found: " +
                                    existing.getNom() + " " + existing.getPrenom() +
                                    " (IDs: " + existing.getId() + " and " + duplicate.getId() +
                                    "). Using ID: " + existing.getId());
                            return existing;
                        }
                ));

        List<TeacherUnavailability> teacherUnavailabilityList = getTeacherUnavailabilities(workbook, session, teacherMap);

        teacherUnavailabilitRepository.saveAll(teacherUnavailabilityList);
    }

    private static List<TeacherUnavailability> getTeacherUnavailabilities( Workbook workbook, ExamSession session, Map<TeacherKey, Teacher> teacherMap)  {
        List<TeacherUnavailability> teacherUnavailabilityList = new ArrayList<>();

        workbook.forEach(sheet -> {
            sheet.forEach(row -> {
                if (row.getRowNum() == 0) return;

                String nom = getCellAsString(row, 2);
                String prenom = getCellAsString(row, 3);

                TeacherKey key = new TeacherKey(
                        getCellAsString(row, 2),
                        getCellAsString(row, 3)
                );
                Teacher teacher = teacherMap.get(key);

                if (teacher == null) {
                    System.err.println("Teacher not found: " + nom + " " + prenom);
                    return;
                }
                TeacherUnavailability teacherUnavailability = TeacherUnavailability.builder()
                        .teacher(teacher)
                        .examSession(session)
                        .numeroJour(getCellAsInteger(row, 4))
                        .seance(getCellAsString(row, 5))
                        .build();

                teacherUnavailabilityList.add(teacherUnavailability);
            });
        });
        return teacherUnavailabilityList;
    }
    public List<TeacherUnavailabilityProjection> getTeacherUnavailabilitiesBySessionId(Long sessionId) {
       return teacherUnavailabilitRepository.getTeacherUnavailableBySessionId(sessionId);
    }
    public List<TeacherUnavailabilitiesProjection> getTeacherUnavailabilityListBySessionId(Long sessionId) {
        return teacherUnavailabilitRepository.findAllByExamSessionId(sessionId);
    }
    public void cleanUp(){
        teacherUnavailabilitRepository.deleteAll();
    }
}
