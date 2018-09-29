package com.wenbing.service;


import com.wenbing.annotation.Service;

@Service("myTestService")
public class MyTestService {

    public String test(){
        return "MyTestService";
    }
}
