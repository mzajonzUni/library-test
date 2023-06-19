package pl.zajonz.librarytest.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pl.zajonz.librarytest.model.PerformanceInfo;
import pl.zajonz.librarytest.model.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PerformanceInfoMapperTest {

    @Autowired
    private PerformanceInfoMapper mapper;

    @Test
    void testToMeasuredInfo() {
        //given
        User user = User.builder()
                .id(1)
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .role("ROLE_TEST")
                .password("TEST")
                .build();
        LocalDateTime methodStartTime = LocalDateTime.now();
        String classMethodName = "Test.test(..)";
        long executionTime = 123L;

        //when
        PerformanceInfo performanceInfo = mapper.toPerformanceInfo(user, executionTime, classMethodName, methodStartTime);

        //then
        assertEquals(user.getId(), performanceInfo.getId());
        assertEquals(user.getEmail(), performanceInfo.getEmail());
        assertEquals(executionTime, performanceInfo.getExecutionTime());
        assertEquals(classMethodName, performanceInfo.getClassMethodName());
        assertEquals(methodStartTime, performanceInfo.getMethodStartTime());
    }
}