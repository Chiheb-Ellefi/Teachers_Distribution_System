package org.teacherdistributionsystem.distribution_system.services.teachers;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherQuota;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;

import org.teacherdistributionsystem.distribution_system.enums.QuotaType;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherQuotaRepository;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.teacherdistributionsystem.distribution_system.utils.data.ExcelCellUtils.getCellAsString;

@Service
public class TeacherQuotaService {
    private final TeacherQuotaRepository teacherQuotaRepository;

    public TeacherQuotaService(TeacherQuotaRepository teacherQuotaRepository) {
        this.teacherQuotaRepository = teacherQuotaRepository;
    }
    public void addTeachersQuota(FileInputStream file, Map<String,Teacher> teacherMap,ExamSession session) throws IOException {
        List<TeacherQuota> teacherQuotas=new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(file);
      workbook.forEach(sheet -> {
          sheet.forEach(row -> {
              if (row.getRowNum() == 0) return ;
              TeacherQuota quota=TeacherQuota.builder()
                      .assignedQuota(GradeType.fromCode(getCellAsString(row,3)).getDefaultQuota())
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
}
