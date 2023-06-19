package pl.zajonz.librarytest.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PerformanceInfo {

    private int id;
    private String email;
    private long executionTime;
    private String classMethodName;
    private LocalDateTime methodStartTime;

}
