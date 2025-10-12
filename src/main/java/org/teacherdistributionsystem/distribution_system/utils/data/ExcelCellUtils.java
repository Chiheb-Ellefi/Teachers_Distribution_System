package org.teacherdistributionsystem.distribution_system.utils.data;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

public class ExcelCellUtils {
    public static String getCellAsString(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }


    public static Integer getCellAsInteger(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return 0;

        return cell.getCellType() == CellType.NUMERIC
                ? (int) cell.getNumericCellValue()
                : 0;
    }


    public static Boolean getCellAsBoolean(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return false;

        return cell.getCellType() == CellType.BOOLEAN
                && cell.getBooleanCellValue();
    }


    public static Double getCellAsDouble(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return 0.0;

        return cell.getCellType() == CellType.NUMERIC
                ? cell.getNumericCellValue()
                : 0.0;
    }
}
