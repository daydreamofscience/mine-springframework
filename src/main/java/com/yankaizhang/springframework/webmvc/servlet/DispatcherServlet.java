package com.yankaizhang.springframework.webmvc.servlet;

import com.yankaizhang.springframework.annotation.Controller;
import com.yankaizhang.springframework.annotation.RequestMapping;
import com.yankaizhang.springframework.beans.BeanWrapper;
import com.yankaizhang.springframework.context.ApplicationContext;
import com.yankaizhang.springframework.webmvc.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 手写实现DispatcherServlet
 */
@Slf4j
@SuppressWarnings("all")
public class DispatcherServlet extends HttpServlet {

    public static final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);
    private final String LOCATION = "contextConfigLocation";
    private List<HandlerMapping> handlerMappings = new ArrayList<>();
    private Map<HandlerMapping, HandlerAdapter> handlerAdapterMap = new HashMap<>();
    private List<ViewResolver> viewResolvers = new ArrayList<>();
    private ApplicationContext context;

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 初始化IoC容器
        context = new ApplicationContext(config.getInitParameter(LOCATION));
        initStrategies(context);
        logger.debug("容器中实例个数：" + String.valueOf(context.getFactoryBeanInstanceCache().size()));
    }

    private void initStrategies(ApplicationContext context){
        initMultipartResolver(context); // 多部分文件上传解析multipart
        initLocaleResolver(context);    // 本地化解析
        initThemeResolver(context);     // 主题解析

        initHandlerMappings(context);       // url映射到controller
        initHandlerAdapters(context);       // 多类型参数动态匹配，获得ModelAndView对象

        initHandlerExceptionResolvers(context);     // 运行异常处理
        initRequestToViewNameTranslator(context);   // 直接将请求解析到视图名
        initViewResolvers(context);     // 通过viewResolver将逻辑视图解析为具体视图实现
        initFlashMapManager(context);   // 初始化Flash映射管理器

        logger.debug("Dispatcher Servlet init done!");
    }

    /*
      这些暂时不实现
     */
    private void initFlashMapManager(ApplicationContext context){}
    private void initRequestToViewNameTranslator(ApplicationContext context){}
    private void initHandlerExceptionResolvers(ApplicationContext context){}
    private void initThemeResolver(ApplicationContext context){}
    private void initLocaleResolver(ApplicationContext context){}
    private void initMultipartResolver(ApplicationContext context){}


    /**
     * 初始化HandlerMapping
     */
    private void initHandlerMappings(ApplicationContext context){
        Map<String, BeanWrapper> ioc = context.getFactoryBeanInstanceCache();   // 获取已经存在的实例化好的对象
        try {
            for (BeanWrapper beanWrapper : ioc.values()) {
                Object beanInstance = beanWrapper.getWrappedInstance();
                if (beanInstance == null) continue;   // 排除可能有的bean没有在容器中

                Class<?> clazz = beanInstance.getClass();
                if (!clazz.isAnnotationPresent(Controller.class)) continue;

                String baseUrl = "";
                if (clazz.isAnnotationPresent(RequestMapping.class)){
                    RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                    baseUrl = requestMapping.value();   // 如果标注了@MyController注解的值
                }

                // 将controller标注@MyRequestMapping的方法加入handlerMapping
                for (Method method : clazz.getDeclaredMethods()) {
                    if (!method.isAnnotationPresent(RequestMapping.class)) continue;

                    RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);

                    // 这里生成的最终url应该是正则表达式形式
                    String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                    Pattern pattern = Pattern.compile(url);
                    handlerMappings.add(new HandlerMapping(beanInstance, method, pattern));

                    System.out.println("Mapped: " + url + " ===> " + method);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 注册每个handler的参数适配器
     * 注意HandlerMapping是所说的handler的包装类
     */
    private void initHandlerAdapters(ApplicationContext context){
        for (HandlerMapping handlerMapping : handlerMappings) {
            handlerAdapterMap.put(handlerMapping, new HandlerAdapter());
        }
    }


    /**
     * 注册模板解析器
     */
    private void initViewResolvers(ApplicationContext context){
        String templateRoot = context.getConfig().getProperty("templateRoot");
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();

        File templateRootDir = new File(templateRootPath);
        for (File template : templateRootDir.listFiles()) {
            viewResolvers.add(new ViewResolver(templateRoot, template.getName()));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        }catch (Exception e){
            e.printStackTrace();
            try {
                ModelAndView modelAndView = new ModelAndView("500");
                HashMap<String, Object> map = new HashMap<>();
                map.put("stackTrace", Arrays.toString(e.getStackTrace()));
                modelAndView.setModel(map);
                processDispatchResult(req, resp, modelAndView); // 返回500渲染页面
            }catch (Exception e1){
                e1.printStackTrace();
                resp.getWriter().write("<h1>500 Exception</h1>\n Message：\n" +
                        e.getMessage() + "\nStackTrace：\n" + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        logger.debug("请求==>" + req.getRequestURI());
        HandlerMapping handlerMapping = getHandlerMapping(req);
        if (null == handlerMapping){
            // 如果没有这个controller，返回404页面
            processDispatchResult(req, resp, new ModelAndView("404"));
            return;
        }

        HandlerAdapter handlerAdapter = getHandlerAdapter(handlerMapping);
        if (handlerAdapter == null){
            throw new Exception("There is no HandlerAdapter corresponding to the request \"" + req.getRequestURI() + "\"");
        }
        ModelAndView model = handlerAdapter.handle(req, resp, handlerMapping);
        processDispatchResult(req, resp, model);
    }

    /**
     * 调用视图处理器处理相应视图
     */
    private void processDispatchResult(HttpServletRequest req, HttpServletResponse resp, ModelAndView modelAndView)
            throws Exception {
        if (null == modelAndView) return;
        if (viewResolvers.isEmpty()) return;

        for (ViewResolver viewResolver : viewResolvers) {
            if (!viewResolver.getViewName().equals(modelAndView.getViewName().trim()+ViewResolver.DEFAULT_TEMPLATE_SUFFIX)) continue;
            View view = viewResolver.resolveViewName(modelAndView.getViewName(), null);
            if (view != null){
                view.render(modelAndView.getModel(), req, resp);
                return;
            }
        }
    }


    /**
     * 获取handler对应的HandlerAdapter
     */
    private HandlerAdapter getHandlerAdapter(HandlerMapping handlerMapping){
        if (handlerAdapterMap.isEmpty()) return null;
        HandlerAdapter handlerAdapter = handlerAdapterMap.get(handlerMapping);
        if (handlerAdapter.supports(handlerMapping)){
            return handlerAdapter;
        }
        return null;
    }

    /**
     * 根据相应请求获取对应Handler
     */
    private HandlerMapping getHandlerMapping(HttpServletRequest req){
        if (handlerMappings.isEmpty()) return null;
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();  // contextPath是项目部署的url地址，需要替换为空字符
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        for (HandlerMapping handler : handlerMappings) {
            // 如果这个url被某个regex匹配到了，就返回这个对应的handler
            if (handler.getPattern().matcher(url).matches()){
                return handler;
            }
        }
        return null;
    }
}

