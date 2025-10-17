package org.teacherdistributionsystem.distribution_system.utils;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PythonScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(PythonScriptExecutor.class);
    @Value("${spring.application.outputDir}")
    private  String DATA_DIR ;
    @Value("${spring.application.scriptDir}")
    private String scriptDir;
    @Value("${spring.application.scriptFilename}")
    private String scriptFileName;
    private static final int MAX_WAIT_TIME_SECONDS = 300;
    private static final int CHECK_INTERVAL_MS = 2000;

    @Async
    public CompletableFuture<Boolean> executePythonScript(String jsonFileName ) {
        try {
            logger.info("Starting background thread to execute Python script");
            String pythonScriptPath=Paths.get(scriptDir,scriptFileName).toFile().getAbsolutePath();
            Path filePath = Paths.get(DATA_DIR, jsonFileName);
            if (!waitForFileToBeReady(filePath)) {
                logger.error("File {} is not ready after waiting", jsonFileName);
                return CompletableFuture.completedFuture(false);
            }

            logger.info("File {} is ready, executing Python script", jsonFileName);

            // Execute Python script
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python3",
                    pythonScriptPath,
                    filePath.toString()
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("Python output: {}", line);
                }
            }

            // Wait for completion
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);

            if (finished && process.exitValue() == 0) {
                logger.info("Python script executed successfully");
                return CompletableFuture.completedFuture(true);
            } else {
                logger.error("Python script failed with exit code: {}", process.exitValue());
                return CompletableFuture.completedFuture(false);
            }

        } catch (Exception e) {
            logger.error("Error executing Python script", e);
            return CompletableFuture.completedFuture(false);
        }
    }


    private boolean waitForFileToBeReady(Path filePath) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long lastSize = -1;
        int stableChecks = 0;

        while ((System.currentTimeMillis() - startTime) < MAX_WAIT_TIME_SECONDS * 1000) {
            File file = filePath.toFile();

            // Check if file exists
            if (!file.exists()) {
                logger.debug("Waiting for file {} to be created...", filePath);
                Thread.sleep(CHECK_INTERVAL_MS);
                continue;
            }

            // Check if file size is stable (not being written to)
            long currentSize = file.length();

            if (currentSize == lastSize && currentSize > 0) {
                stableChecks++;
                // If size hasn't changed for 3 consecutive checks, file is ready
                if (stableChecks >= 3) {
                    // Additional validation: check if it's valid JSON
                    if (isValidJsonFile(filePath)) {
                        logger.info("File {} is ready (size: {} bytes)", filePath, currentSize);
                        return true;
                    } else {
                        logger.debug("File exists but JSON is not valid yet...");
                        stableChecks = 0;
                    }
                }
            } else {
                stableChecks = 0;
                lastSize = currentSize;
                logger.debug("File size changed to {} bytes, waiting for stability...", currentSize);
            }

            Thread.sleep(CHECK_INTERVAL_MS);
        }

        return false;
    }


    private boolean isValidJsonFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            // Basic JSON validation - check if it starts with { or [ and ends properly
            content = content.trim();
            return (content.startsWith("{") && content.endsWith("}")) ||
                    (content.startsWith("[") && content.endsWith("]"));
        } catch (Exception e) {
            return false;
        }
    }
}