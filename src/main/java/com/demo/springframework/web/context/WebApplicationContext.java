package com.demo.springframework.web.context;

import javax.servlet.ServletContext;

public interface WebApplicationContext {

    String SPRING_IOC = WebApplicationContext.class.getName() + ".ROOT";

    String SPRING_MVC_IOC = WebApplicationContext.class.getName() + "MVC.ROOT";

}
