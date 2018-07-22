package com.jd.srm.controller;


import com.demo.springframework.beans.factory.annotation.Autowire;
import com.demo.springframework.beans.factory.annotation.Controller;
import com.demo.springframework.beans.factory.annotation.RequestMapping;
import com.jd.srm.biz.HelloService;
import com.jd.srm.service.DemoService;

@Controller
@RequestMapping("/demo")
public class DemoController {

    @Autowire
    private DemoService demoService;

    @Autowire
    private HelloService helloService;

    @RequestMapping("/printTime")
    public void printTime(){
        String time = demoService.printTime();
    }

    @RequestMapping("/hello")
    public void hello(){
        String word = helloService.sayHello();
    }

}
