package com.jd.srm.dao.impl;


import com.demo.springframework.beans.factory.annotation.Component;
import com.demo.springframework.beans.factory.annotation.Repository;
import com.jd.srm.dao.DemoDao;

import java.util.Date;


@Component
public class DemoDaoImpl implements DemoDao {

    @Override
    public String printTime() {
        System.out.println("北京时间:" + new Date());
        return "北京时间:" + new Date();
    }
}
