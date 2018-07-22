package com.jd.srm.biz;


import com.demo.springframework.beans.factory.annotation.Component;

@Component
public class HelloService {

    public String sayHello(){
        return "hello,world";
    }

}
