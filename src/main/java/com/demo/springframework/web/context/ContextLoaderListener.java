package com.demo.springframework.web.context;

import com.demo.springframework.beans.factory.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class ContextLoaderListener implements ServletContextListener {

    private final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";

    private final Properties config = new Properties();

    private final List<String> classNameList = new LinkedList<>();

    private final Map<String,Object> context = new HashMap<>();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        System.out.println("servletContextListener Initialized..");
        System.out.println("start init web application context..");
        this.initWebApplicationContext(event.getServletContext());
        System.out.println("start init web application context... successfully");
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("servletContextListener Destroyed..");
        System.out.println("start Destroyed web application context..");
    }


    /**
     * 初始化IOC容器
     * @param servletContext
     */
    private void initWebApplicationContext(ServletContext servletContext) {
        //1
        doLoadConfig(servletContext);
        //2
        String basePackage = config.getProperty("component.scan.base.package");
        doScan(basePackage);
        //3
        doInstance();
        //4
        inject();
        servletContext.setAttribute(WebApplicationContext.SPRING_IOC , this.context);

        for (Map.Entry<String, Object> entry : this.context.entrySet()) {
            System.out.println(entry.getKey() + "------------->" + entry.getValue());
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
                Component component = clazz.getAnnotation(Component.class);

                Object target = clazz.newInstance();

                if (component.value() .equals("")){
                    put(target,generateDefaultBeanId(clazz.getSimpleName()));
                } else {
                    put(target,component.value());
                }

                Class<?>[] interfaces = clazz.getInterfaces();
                if (interfaces != null){
                    for (Class<?> interfaceClazz : interfaces) {
                        put(target,generateDefaultBeanId(interfaceClazz.getSimpleName()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("doInstance sucessfully..");

    }

    private void put(Object target , String beanId){
        System.out.println(beanId + "has bean add to ioc sucessfully");
        context.put(beanId, target);
    }

    /**
     * 判断是否需要实例化
     * @param clazz
     * @return
     */
    private boolean needInstance(Class<?> clazz){
        if(clazz.isAnnotationPresent(Component.class) && !clazz.isInterface()){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 注入
     */
    private void inject(){
        try {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                Object instance = entry.getValue();
                Class<?> clazz = instance.getClass();
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
                    field.set(instance, injectBody);
                    System.out.println("bean:" + beanId +"-> field:" + field.getName() + " has bean inject field sucessfully");
                }
                System.out.println(clazz.getName() + " has bean inject 100%");
            }
        } catch(Exception e){
            System.err.println("ioc dependency inject happened a exception");
            e.printStackTrace();
        }
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
     * @param servletContext
     */
    private void doLoadConfig(ServletContext servletContext){

        System.out.println("start doLoadConfig..");

        String location = servletContext.getInitParameter(CONTEXT_CONFIG_LOCATION);

        System.out.println("start read contextConfigLocation..");

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream( location);
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

}
