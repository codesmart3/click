/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.click.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import ognl.Ognl;
import ognl.OgnlException;

import org.apache.click.util.PropertyUtils;

/**
 * Provides an OGNL based property services.
 */
public class OGNLPropertyService implements PropertyService {

    /** Provides a synchronized cache of OGNL expressions. */
    private static final Map<String, Object> OGNL_EXPRESSION_CACHE
        = new ConcurrentHashMap<String, Object>();

    //Public Methods  --------------------------------------------------------

    /**
     * @see PropertyService#onInit(ServletContext)
     *
     * @param servletContext the application servlet context
     * @throws IOException if an IO error occurs initializing the service
     */
    public void onInit(ServletContext servletContext) throws IOException {
    }

    /**
     * @see PropertyService#onDestroy()
     */
    public void onDestroy() {
    }

    /**
     * Return the property value for the given object and property name.
     * <p/>
     * For performance and backward compatibility reasons this method uses
     * reflection internally to get the property value.
     * <p/>
     * This method is thread-safe, and caches reflected accessor methods in an
     * internal synchronized cache
     * <p/>
     * If the given source object is a <tt>Map</tt> this method will simply
     * return the value for the given key name.
     *
     * @see PropertyService#getValue(Object, String)
     *
     * @param source the source object
     * @param name the name of the property
     * @return the property value for the given source object and property name
     */
    public Object getValue(Object source, String name) {
        return PropertyUtils.getValue(source, name);
    }

    /**
     * Return the property value for the given object and property name.
     * <p/>
     * For performance and backward compatibility reasons this method uses
     * reflection internally to get the property value.
     * <p/>
     * This method uses reflection internally to get the property value.
     * <p/>
     * This method caches the reflected property methods in the given Map cache.
     * You must NOT modify the cache. Also note cache is ONLY valid for the
     * current thread, as access to the cache is not synchronized. If you need
     * multi-threaded access to shared cache use a thread-safe Map object, such
     * as <tt>Collections.synchronizedMap(new HashMap())</tt>.
     * <p/>
     * If the given source object is a <tt>Map</tt> this method will simply
     * return the value for the given key name.
     *
     * @see PropertyService#getValue(Object, String, Map)
     *
     * @param source the source object
     * @param name the name of the property
     * @param cache the cache of reflected property Method objects, do NOT modify
     * this cache
     * @return the property value for the given source object and property name
     */
    public Object getValue(Object source, String name, Map<?, ?> cache) {
        return PropertyUtils.getValue(source, name);
    }

    /**
     * Set the named property value on the target object using the OGNL library.
     *
     * @see PropertyService#setValue(Object, String, Object)
     *
     * @param target the target object to set the property of
     * @param name the name of the property to set
     * @param value the property value to set
     */
    public void setValue(Object target, String name, Object value) {
        OGNLTypeConverter typeConverter = new OGNLTypeConverter();

        Map<?, ?> ognlContext =
            Ognl.createDefaultContext(target, null, typeConverter);

        try {
            Object expression = OGNL_EXPRESSION_CACHE.get(name);
            if (expression == null) {
                expression = Ognl.parseExpression(name);
                OGNL_EXPRESSION_CACHE.put(name, expression);
            }

            Ognl.setValue(expression, ognlContext, target, value);


        } catch (OgnlException oe) {
            throw new RuntimeException(oe.toString(), oe);
        }
    }

}

