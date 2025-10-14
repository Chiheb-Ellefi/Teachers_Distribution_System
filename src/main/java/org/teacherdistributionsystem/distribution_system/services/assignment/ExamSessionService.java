package org.teacherdistributionsystem.distribution_system.services.assignment;

import org.apache.coyote.BadRequestException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.ExamSessionDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;
import org.teacherdistributionsystem.distribution_system.enums.SemesterType;
import org.teacherdistributionsystem.distribution_system.enums.SessionType;
import org.teacherdistributionsystem.distribution_system.mappers.assignment.ExamSessionMapper;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.ExamSessionRepository;

import java.time.LocalDate;


import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.teacherdistributionsystem.distribution_system.utils.HelperMethods.getLocalDate;

@Service
public class ExamSessionService {
    private  final ExamSessionRepository examSessionRepository;
    public ExamSessionService(ExamSessionRepository examSessionRepository) {
        this.examSessionRepository = examSessionRepository;
    }
    public ExamSession addSession( Workbook workbook) {

        Sheet sheet = workbook.getSheetAt(0);
        LocalDate startDate = null;
        LocalDate endDate = null;
        String academicYear ;
        String sessionLibelle = null;
        String semesterLibelle = null;

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;

            Function<Integer, String> getString = i -> {
                Cell cell = row.getCell(i);
                if (cell == null) return "";
                return switch (cell.getCellType()) {
                    case STRING -> cell.getStringCellValue().trim();
                    case NUMERIC -> {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                        } else {
                            yield String.valueOf((int) cell.getNumericCellValue());
                        }
                    }
                    case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                    default -> "";
                };
            };

            LocalDate examDate = getLocalDate(getString.apply(0));

            if (startDate == null || examDate.isBefore(startDate)) {
                startDate = examDate;
            }
            if (endDate == null || examDate.isAfter(endDate)) {
                endDate = examDate;
            }

            sessionLibelle = SessionType.valueOf(getString.apply(3)).getLabel();
            semesterLibelle = getString.apply(5);
        }

        if (endDate == null) {
            throw new IllegalStateException("No valid exam dates found in Excel file.");
        }

        academicYear = String.valueOf(endDate.getYear());
        int jourNumero = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;

        ExamSession session = ExamSession.builder()
                .startDate(startDate)
                .endDate(endDate)
                .sessionLibelle(sessionLibelle)
                .academicYear(academicYear)
                .semesterCode(SemesterType.fromLabel(semesterLibelle).getLabel())
                .numExamDays(jourNumero)
                .seancesPerDay(SeanceType.values().length)
                .semesterLibelle(semesterLibelle)
                .build();
      return   examSessionRepository.save(session);
    }
    ExamSessionDto getExamSessionDto(Long sessionId) throws BadRequestException {
        Supplier<BadRequestException> exceptionSupplier = () -> new BadRequestException("No valid exam session found with id " + sessionId);
        return ExamSessionMapper.toExamSessionDto(examSessionRepository.findById(sessionId).orElseThrow(exceptionSupplier));
    }

}
