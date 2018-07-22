package com.demo.springframework.web.servlet;

import com.demo.springframework.beans.factory.annotation.Autowire;
import com.demo.springframework.beans.factory.annotation.Component;
import com.demo.springframework.beans.factory.annotation.Controller;
import com.demo.springframework.beans.factory.annotation.RequestMapping;
import com.demo.springframework.web.context.WebApplicationContext;
import com.demo.springframework.web.context.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {

    private final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";

    private final Properties config = new Properties();

    private final List<String> classNameList = new LinkedList<>();

    private final Map<String,Object> mvcIOC = new HashMap<>();

    private final Map<String,Handler> handlerMap = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doService(req,resp);
    }


    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        ServletContext servletContext = servletConfig.getServletContext();

        doLoadConfig(servletConfig);

        String basePackage = config.getProperty("component.scan.base.package");

        doScan(basePackage);

        doInstance();

        inject(servletContext);

        servletContext.setAttribute(WebApplicationContext.SPRING_MVC_IOC , this.mvcIOC);

        System.out.println("springMVC IOC load 100% sucessfully...");
    }

    /**
     * 处理请求
     * @param req
     * @param resp
     */
    private void doService(HttpServletRequest req, HttpServletResponse resp) {
        String requestMapping = req.getRequestURI();
        Handler handler = handlerMap.get(requestMapping);

        PrintWriter writer = null;
        try {
            writer = resp.getWriter();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(null == handler){
            writer.println("<h1> 404 not found </h1>");
            writer.flush();
        } else {
            handler.process();
        }
    }


    private void doInstance() {

        System.out.println("start doInstance ...");

        for (String className : classNameList) {
            try {
                Class<?> clazz = Class.forName(className);
                if(!needInstance(clazz)){
                    continue;
                }
                Controller component = clazz.getAnnotation(Controller.class);
                this.put(clazz,component.value());
                if(clazz.isInterface()){
                    for (Class<?> interfaceName : clazz.getInterfaces()) {
                        put(clazz,null);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("doInstance sucessfully..");

    }

    private void put(Class clazz , String beanId){
        Object instance = null;
        try {
            instance = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(beanId == null || "".equals(beanId)){
            beanId = generateDefaultBeanId(clazz.getSimpleName());
        }

        System.out.println(beanId + "has bean add to ioc sucessfully");
        mvcIOC.put(beanId, instance);
    }

    /**
     * 判断是否需要实例化
     * @param clazz
     * @return
     */
    private boolean needInstance(Class<?> clazz){
        if(clazz.isAnnotationPresent(Controller.class)){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 注入
     */
    private void inject(ServletContext servletContext){
        Map<String,Object> context = (Map<String,Object>) WebApplicationContextUtils.getWebApplicationContext(servletContext, WebApplicationContext.SPRING_IOC);
        try {
            for (Map.Entry<String, Object> entry : mvcIOC.entrySet()) {
                Object target = entry.getValue();
                Class<?> clazz = target.getClass();
                for (Field field : clazz.getDeclaredFields()) {
                    if (!field.isAnnotationPresent(Autowire.class)) {
                        continue;
                    }
                    String beanId;
                    Autowire autowire = field.getDeclaredAnnotation(Autowire.class);
                    if ("".equals(autowire.value())) {
                        beanId = generateDefaultBeanId(field.getName());
                    } else {
                        beanId = autowire.value();
                    }

                    Object injectBody = context.get(beanId);
                    if(null == injectBody){
                        System.err.println(clazz.getName() + "的" +  beanId + "属性注入失败");
                    }
                    field.setAccessible(true);
                    field.set(target, injectBody);
                    System.out.println("bean:" + beanId +"-> field:" + field.getName() + " has bean inject field sucessfully");
                }
                System.out.println(clazz.getName() + " has bean inject 100%");

                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                String prefix = requestMapping.value();

                for (Method method : clazz.getDeclaredMethods()) {
                    if(!method.isAnnotationPresent(RequestMapping.class)){
                        continue;
                    }
                    RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
                    String suffix = methodRequestMapping.value();
                    handlerMap.put(generatePath(prefix,suffix) , new Handler(method,target,null));

                }
            }
        } catch(Exception e){
            System.err.println("ioc dependency inject happened a exception");
            e.printStackTrace();
        }
    }

    private String generatePath(String prefix , String suffix){
        if(null == prefix){
            prefix = "";
        }

        if(suffix!=null && !suffix.startsWith("/")){
            suffix = "/" + suffix;
        }

        String requestMapping = prefix +  suffix;
        System.out.println("RequestMapping:" + requestMapping);
        return requestMapping;

    }

    private String generateDefaultBeanId(String s){
        if(Character.isLowerCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
    }

    /**
     * 加载spring配置文件
     *
     * @param servletConfig
     */
    private void doLoadConfig(ServletConfig servletConfig){

        System.out.println("start do springMVC LoadConfig..");

        String location = servletConfig.getInitParameter( CONTEXT_CONFIG_LOCATION);

        System.out.println("start read springMVC contextConfigLocation..");

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            config.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("doLoadConfig..sucessfully");
    }

    /**
     * 进行包扫描
     *
     * @param
     */
    private void doScan(String basePackage){

        System.out.println("start doScan , basePackage:" + basePackage);
        String scanPackage = "/" + basePackage.replaceAll("\\.","/");
        URL url = this.getClass().getResource(scanPackage);
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            String name = file.getName();
            if(file.isDirectory()){
                doScan(basePackage.concat(".").concat(name));
            }else{
                String className = basePackage.concat(".").concat(name).replace(".class", "");
                classNameList.add(className);
                System.out.println(className + " has bean scaned ..");
            }
        }
    }


    private class Handler{

        Method method;

        Object target;

        Object[] args;

        public Handler(Method method, Object target, Object[] args) {
            this.method = method;
            this.target = target;
            this.args = args;
        }

        public Object process(){
            try {
                method.setAccessible(true);
                Object result = method.invoke(target, args);
                return result;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }

    }


}
