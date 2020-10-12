package com.yankaizhang.springframework.test.controller;

import com.yankaizhang.springframework.annotation.Autowired;
import com.yankaizhang.springframework.annotation.Controller;
import com.yankaizhang.springframework.annotation.RequestMapping;
import com.yankaizhang.springframework.annotation.RequestParam;
import com.yankaizhang.springframework.test.service.TestService;
import com.yankaizhang.springframework.webmvc.ModelAndView;
import com.yankaizhang.springframework.test.service.impl.UserServiceImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@Controller
public class TestController {

    @Autowired
    TestService testService;

    @Autowired
    UserServiceImpl userService;

    @RequestMapping("/hello")
    public ModelAndView hello(HttpServletRequest req, HttpServletResponse resp, @RequestParam("name") String name){
        String result = testService.sayHello(name);
        HashMap<String, Object> map = new HashMap<>();
        map.put("answer", result);
        ModelAndView modelAndView = new ModelAndView("hello", map);     // 返回到hello.html
        return modelAndView;
    }

    @RequestMapping("/userHello")
    public ModelAndView userHello(HttpServletRequest req, HttpServletResponse resp, @RequestParam("username") String username){
        String result = userService.helloUser(username);
        HashMap<String, Object> map = new HashMap<>();
        map.put("answer", result);
        ModelAndView modelAndView = new ModelAndView("hello", map);     // 返回到hello.html
        return modelAndView;
    }

    @RequestMapping("/index")
    public String toIndex(HttpServletRequest req, HttpServletResponse resp){
        req.setAttribute("username", "用户名");
        return "index";
    }
}