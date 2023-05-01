package dev.filechampion.filechampion4j;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggingExtension implements AfterTestExecutionCallback {

    private final List<String> capturedLogs = new ArrayList<>();

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        // Get the logger used by the target class
        Logger targetLogger = Logger.getLogger(context.getRequiredTestClass().getName());

        // Create a custom handler to capture the logs
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                // Add the log message to the capturedLogs list
                capturedLogs.add(record.getMessage());
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };

        // Attach the handler to the target class logger
        targetLogger.addHandler(handler);

        // Run the test method
        context.getTestMethod().ifPresent(method -> {
            try {
                method.invoke(context.getRequiredTestInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Remove the handler from the target class logger
        targetLogger.removeHandler(handler);

        // Store the LoggingExtension instance in the ExtensionContext.Store
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(getClass(), this);
    }

    public List<String> getCapturedLogs() {
        return capturedLogs;
    }

}
