package pl.zajonz.librarytest.mapper;

import org.mapstruct.Mapper;
import pl.zajonz.librarytest.model.PerformanceInfo;
import pl.zajonz.librarytest.model.User;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface PerformanceInfoMapper {

    PerformanceInfo toPerformanceInfo(User user, Long executionTime, String classMethodName, LocalDateTime methodStartTime);

}
