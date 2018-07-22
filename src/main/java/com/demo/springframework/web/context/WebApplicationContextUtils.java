package com.demo.springframework.web.context;

import javax.servlet.ServletContext;

public class WebApplicationContextUtils {

   public static Object getWebApplicationContext(ServletContext sc , String attrName){
       Object context = sc.getAttribute(attrName);
       return context;
   }


}
