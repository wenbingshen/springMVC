package com.wenbing.servlet;

import com.wenbing.annotation.Autowired;
import com.wenbing.annotation.Controller;
import com.wenbing.annotation.RequestMapping;
import com.wenbing.annotation.Service;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

@WebServlet(name = "DispatcherServlet")
public class DispatcherServlet extends HttpServlet {

    List<String> clazzName = new ArrayList<String>();

    Map<String, Object> beans = new HashMap<String, Object>();

    Map<String, Object> handlerMethod = new HashMap<String, Object>();

    public DispatcherServlet() {
        super();
    }

    @Override
    public void init() throws ServletException {
        //1.包扫描
        scanPackage("com.wenbing");
//        for (String name : clazzName) {
//            System.out.println(name);
//        }

        //2.类实例化
        classInstance();

        //3.依赖注入/完成装配类
        inject();

        //4.完成url和controller中的method关系映射
        handlerMapping();
    }

    private void scanPackage(String basePackage) {
        //   com.wenbing -> D:/idea/myselfSpringMVC/src/main/java/com/wenbing
        String path = basePackage.replaceAll("\\.", "/");
        URL url = getClass().getClassLoader().getResource("/" + path);
        //目录的递归扫描
        String filePath = url.getFile();
        File[] files = new File(filePath).listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
//                System.out.println(file.getPath());
                scanPackage(basePackage + "." + file.getName());
            } else {
                //类是以.格式分隔的
                clazzName.add(basePackage + "." + file.getName());
            }
        }
    }

    private void classInstance() {
        if (clazzName.isEmpty()) {
            System.out.println("没有扫描到任何类");
            return;
        }

        //com.wenbing.annotation.RequestMapping.class --> com.wenbing.annotation.RequestMapping
        for (String name : clazzName) {
            String realName = name.replace(".class", "");
            try {
                Class clazz = Class.forName(realName);
                //判断是不是Controller标签批注类
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Controller controller = (Controller) clazz.getAnnotation(Controller.class);
                    //完成实例化
                    Object instance = clazz.newInstance();
                    RequestMapping requestMapping = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
                    String mappingValue = requestMapping.value();

                    //把类的映射路径和类实例化进行绑定
                    beans.put(mappingValue, instance);
                }
                //判断是不是Service标签批注类
                if (clazz.isAnnotationPresent(Service.class)) {
                    Service service = (Service) clazz.getAnnotation(Service.class);
                    //完成实例化
                    Object instance = clazz.newInstance();

                    //把类的映射路径和类实例化进行绑定
                    beans.put(service.value(), instance);
                }
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    private void inject() {
        if (beans.isEmpty()) {
            System.out.println("没有实例化的类！");
            return;
        }

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            //从集合中获取类实例
            Object instance = entry.getValue();

            //获取类中所有成员属性
            Field[] fields = instance.getClass().getDeclaredFields();
            for (Field field : fields) {
                //判断成员属性上有无相应的批注Autowired
                Autowired autowired = field.getAnnotation(Autowired.class);
                String value = autowired.value();

                field.setAccessible(true);

                try {
                    //最关键的地方，完成依赖注入
                    field.set(instance, beans.get(value));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handlerMapping() {
        if (beans.isEmpty()) {
            System.out.println("没有实例化的类！");
            return;
        }
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            //从集合中获取类实例
            Object instance = entry.getValue();
            //判断类中有无Controller类批注
            if (instance.getClass().isAnnotationPresent(Controller.class)) {
                RequestMapping requestMapping = instance.getClass().getAnnotation(RequestMapping.class);
                //获取类上的RequestMapping中定义的路径值 => @获取RequestMapping("/test")
                String path = requestMapping.value();

                Method[] methods = instance.getClass().getMethods();
                for (Method method : methods) {
                    RequestMapping methodMapping = method.getAnnotation(RequestMapping.class);
                    //获取方法上的RequestMapping中定义的路径值 => @获取RequestMapping("/index")
                    String value = new String();
                    if (methodMapping != null) {
                        value = methodMapping.value();
                        //路径映射到对应的方法上 path+value="/test/index" --> 我们这里测试的index方法
                        handlerMethod.put(path + value, method);
                    }
                }
            }
        }
    }


    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        super.service(req, res);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.service(req, resp);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        //工程下路径为springmvc/test/index -->我们要替换成 /test/index
        String path = uri.replaceAll(contextPath, "");
        Method method = (Method) handlerMethod.get(path);
        Object instance = beans.get("/" + path.split("/")[1]);
        try {
            Object object = method.invoke(instance);
            System.out.println(object +"测试");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }
}
