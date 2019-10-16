package org.sxl.rpc.post;

import com.sxl.common.core.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.sxl.rpc.ann.RpcService;
import org.sxl.rpc.container.LocalHandlerMap;

/**
 * @Author: shenxl
 * @Date: 2019/10/11 10:41
 * @Version 1.0
 * @description：${description}
 */
@Slf4j
public class ParseRpcServiceAnn implements BeanPostProcessor {

    private LocalHandlerMap localHandlerMap;
    public ParseRpcServiceAnn(LocalHandlerMap localHandlerMap){
        this.localHandlerMap=localHandlerMap;

    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        RpcService rpcService=bean.getClass().getAnnotation(RpcService.class);
        if(rpcService!=null){
            String serviceName = rpcService.value().getName();
            String serviceVersion = rpcService.version();
            if (StringUtil.isNotEmpty(serviceVersion)) {
                serviceName += "-" + serviceVersion;
            }
            if (null!=localHandlerMap){

                localHandlerMap.getHandlers().put(serviceName, bean);
                log.info("服务实例 {} 加入本地缓存...",serviceName);
            }
        }
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        return bean;
    }

}