package com.wenbing.controller;


import com.wenbing.annotation.Autowired;
import com.wenbing.annotation.Controller;
import com.wenbing.annotation.RequestMapping;
import com.wenbing.service.MyTestService;

@Controller
@RequestMapping("/test")
public class myTestController {

    @Autowired("myTestService")
    MyTestService myTestService;

    @RequestMapping(value = "/index")
    public String index() {
        return "Test SpringMVC" + myTestService.test();
    }

}
