package com.sihai.web.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;


@SpringBootTest
class CosManagerTest {

    @Resource
    private CosManager cosManager;

    @Test
    void deleteObject() {
        cosManager.deleteObject("/test/20240510211804.jpg");
    }

    @Test
    void deleteObjects() {
        cosManager.deleteObjects(Arrays.asList("test/20240510212001.jpg", "test/20240510211850.jpg"));
    }

    @Test
    void deleteDir() {
        cosManager.deleteDir("/test/");
    }
}