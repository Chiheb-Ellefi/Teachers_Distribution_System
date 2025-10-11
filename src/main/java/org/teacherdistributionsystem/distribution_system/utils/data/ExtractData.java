package org.teacherdistributionsystem.distribution_system.utils.data;


import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.teacherdistributionsystem.distribution_system.entities.teacher.GradeType;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.repositories.GradeTypeRepository;
import org.teacherdistributionsystem.distribution_system.repositories.TeacherRepository;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.util.function.Function;

@Component
public class ExtractData {

    private final TeacherRepository teacherRepository;
    private final GradeTypeRepository gradeTypeRepository;
    @Value("${spring.application.teachersListFile}")
    private  String teachersListFile;

    public ExtractData(TeacherRepository teacherRepository, GradeTypeRepository gradeTypeRepository) {
        this.teacherRepository = teacherRepository;
        this.gradeTypeRepository = gradeTypeRepository;
    }

    @PostConstruct
 void populateTeachersTable() throws IOException {
        List<Teacher> teachers=new ArrayList<>();
        List<GradeType> grades=new ArrayList<>();
     FileInputStream file = new FileInputStream(teachersListFile);
     Workbook workbook = new XSSFWorkbook(file);
     Sheet sheet = workbook.getSheetAt(0);
     sheet.forEach(row -> {
         if (row.getRowNum() != 0) {

             Function<Integer, String> getString = i -> {
                 Cell cell = row.getCell(i);
                 if (cell == null) return "";
                 return switch (cell.getCellType()) {
                     case STRING -> cell.getStringCellValue().trim();
                     case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
                     case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                     default -> "";
                 };
             };
             Function<Integer, Integer> getInteger = i -> {
                 Cell cell = row.getCell(i);
                 if (cell == null) return 0;
                 return cell.getCellType() == CellType.NUMERIC ? (int) cell.getNumericCellValue() : 0;
             };
             Function<Integer, Boolean> getBoolean = i -> {
                 Cell cell = row.getCell(i);
                 if (cell == null) return false;
                 return cell.getCellType() == CellType.BOOLEAN && cell.getBooleanCellValue();
             };
             Teacher teacher = Teacher.builder()
                     .nom(getString.apply(0))
                     .prenom(getString.apply(1))
                     .email(getString.apply(2))
                     .gradeCode(getString.apply(3))
                     .codeSmartex(getInteger.apply(4))
                     .participeSurveillance(getBoolean.apply(5))
                     .build();
             teachers.add(teacher);

             GradeType gradeType=GradeType.builder()
                     .gradeCode(getString.apply(3))
                     .gradeLibelle(GradeMap.fromCode(getString.apply(3)).getLabel())
                     .defaultQuotaPerSession(GradeMap.fromCode(getString.apply(3)).getDefaultQuota())
                     .priorityLevel(GradeMap.fromCode(getString.apply(3)).getPriority())
                     .build();
             grades.add(gradeType);
         }
        teacherRepository.saveAll(teachers);
         gradeTypeRepository.saveAll(grades);
     });

 }

}
