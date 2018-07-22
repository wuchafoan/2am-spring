package com.jd.srm.service.impl;


import com.demo.springframework.beans.factory.annotation.Autowire;
import com.demo.springframework.beans.factory.annotation.Component;
import com.demo.springframework.beans.factory.annotation.Service;
import com.jd.srm.dao.DemoDao;
import com.jd.srm.service.DemoService;

@Component
public class DemoServiceImpl implements DemoService {

    @Autowire
    private DemoDao demoDao;

    @Override
    public String  printTime() {
        return demoDao.printTime();
    }
}
