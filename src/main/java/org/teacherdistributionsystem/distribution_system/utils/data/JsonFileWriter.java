package org.teacherdistributionsystem.distribution_system.utils.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.teacherdistributionsystem.distribution_system.models.responses.AssignmentResponseModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class JsonFileWriter {
    @Value("${spring.application.outputDir}")
    private String outputDir;
    @Value("${spring.application.outputFileName}")
    private String outputFileName;

    private final ObjectMapper objectMapper;

    // Inject Spring's auto-configured ObjectMapper
    public JsonFileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeDataToJsonFile(AssignmentResponseModel assignmentResponse) {
        try {
            Files.createDirectories(Paths.get(outputDir));
            File file = new File(outputDir + "/" + outputFileName);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, assignmentResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}