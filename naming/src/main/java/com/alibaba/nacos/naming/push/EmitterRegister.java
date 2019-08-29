/*
 * Copyright (C) 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacos.naming.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * emitter service register.
 * <p>
 * will load some emitter from spring.factories by {@SpringFactoriesLoader#loadFactoryNames}
 *
 * @author pbting
 * @date 2019-08-28 11:38 AM
 */
@Component
public class EmitterRegister implements BeanDefinitionRegistryPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EmitterRegister.class);

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        final ClassLoader classLoader = registry.getClass().getClassLoader();
        final Class<IEmitter> factoryClass = IEmitter.class;

        List<String> emitterRegistry = SpringFactoriesLoader.loadFactoryNames(factoryClass, classLoader);

        for (String emitter : emitterRegistry) {
            try {
                Class<?> instanceClass = ClassUtils.forName(emitter, classLoader);
                if (!factoryClass.isAssignableFrom(instanceClass)) {
                    throw new IllegalArgumentException(
                        "Class [" + emitter + "] is not assignable to [" + factoryClass.getName() + "]");
                }
                registry.registerBeanDefinition(emitter, new RootBeanDefinition(instanceClass));
            } catch (ClassNotFoundException e) {
                logger.error("load emitter service cause an exception with class not fount.", e);
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }
}
