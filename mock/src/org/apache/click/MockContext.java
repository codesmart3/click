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
package org.apache.click;

import org.apache.click.servlet.MockServletConfig;
import org.apache.click.servlet.MockResponse;
import org.apache.click.servlet.MockRequest;
import java.util.Locale;
import javax.servlet.ServletConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.click.service.ConsoleLogService;
import org.apache.click.servlet.MockServletContext;
import org.apache.click.servlet.MockSession;
import org.apache.click.util.ClickUtils;

/**
 * Provides a mock {@link org.apache.click.Context} object for unit testing.
 * <p/>
 * <b>Note:</b> if you want to test your Click Page instances use
 * {@link MockContainer} instead.
 * <p/>
 * This class defines a couple of helper methods to quickly create all the mock
 * objects needed for unit testing. Please see the following methods:
 * <ul>
 *   <li>{@link #initContext()}</li>
 *   <li>{@link #initContext(Locale)}</li>
 *   <li>{@link #initContext(String)}</li>
 *   <li>{@link #initContext(Locale, String)}</li>
 *   <li>{@link #initContext(MockServletConfig, MockRequest, MockResponse, ClickServlet)}</li>
 * </ul>
 * To use this class in your own tests invoke one of the methods above.
 * For example:
 * <pre class="prettyprint">
 * public class FormTest extends TestCase {
 *     // Create a mock context
 *     MockContext context = MockContext.initContext("test-form.htm");
 *     MockRequest request = context.getMockRequest();
 *
 *     // The request value that should be set as the textField value
 *     String requestValue = "one";
 *
 *     // Set form name and field name parameters
 *     request.setParameter("form_name", "form");
 *     request.setParameter("name", requestValue);
 *
 *     // Create form and fields
 *     Form form = new Form("form");
 *     TextField nameField = new TextField("name");
 *     form.add(nameField);
 *
 *     // Check that nameField value is null
 *     Assert.assertNull(nameField.getValueObject());
 *
 *     // Simulate a form onProcess callback
 *     form.onProcess();
 *
 *     // Check that nameField value is now bound to request value
 *     Assert.assertEquals(requestValue, nameField.getValueObject());
 * }
 * </pre>
 *
 * @author Bob Schellink
 */
public class MockContext extends Context {

    // ----------------------------------------------------------- Constructors

    /**
     * Create a new MockContext instance for the specified request.
     *
     * @deprecated use the other constructor instead.
     *
     * @param request the servlet request
     */
     MockContext(HttpServletRequest request) {
        super(request, null);
    }

    /**
     * Create a new MockContext instance for the specified Mock objects.
     *
     * @param servletConfig the mock servletConfig
     * @param request the mock request
     * @param response the mock response
     * @param isPost specified if this a POST or GET request
     * @param clickServlet the mock clickServlet
     */
    MockContext(ServletConfig servletConfig, HttpServletRequest request,
        HttpServletResponse response, boolean isPost, ClickServlet clickServlet) {
        super(servletConfig == null ? null : servletConfig.getServletContext(),
            servletConfig, request, response, isPost, clickServlet);
    }

    // --------------------------------------------------------- Public getters and setters

    /**
     * Return the mock {@link org.apache.click.ClickServlet} instance for this
     * context.
     *
     * @return the clickServlet instance
     */
    public ClickServlet getServlet() {
        return clickServlet;
    }

    /**
     * Return the {@link org.apache.click.servlet.MockRequest} instance for this
     * context.
     *
     * @return the MockRequest instance
     */
    public MockRequest getMockRequest() {
        return MockContainer.findMockRequest(request);
    }

    // -------------------------------------------------------- Public methods

    /**
     * Creates and returns a new Context instance.
     *<p/>
     * <b>Note:</b> servletPath will default to '/mock.htm'.
     *
     * @return new Context instance
     */
    public static MockContext initContext() {
        return initContext("/mock.htm");
    }

    /**
     * Creates and returns a new Context instance for the specified servletPath.
     *
     * @param servletPath the requests servletPath
     * @return new Context instance
     */
    public static MockContext initContext(String servletPath) {
        return initContext(Locale.getDefault(), servletPath);
    }

    /**
     * Creates and returns a new Context instance for the specified locale.
     *
     * <b>Note:</b> servletPath will default to '/mock.htm'.
     *
     * @param locale the requests locale
     * @return new Context instance
     */
    public static MockContext initContext(Locale locale) {
        return initContext(locale, "/mock.htm");
    }

    /**
     * Creates and returns new Context instance for the specified request.
     *
     * @deprecated use one of the other initContext methods because those will
     * construct a complete mock stack including a MockRequest.
     *
     * @param request the mock request
     * @return new Context instance
     */
    public static MockContext initContext(HttpServletRequest request) {
        MockContext mockContext = new MockContext(request);
        Context.pushThreadLocalContext(mockContext);
        return (MockContext) Context.getThreadLocalContext();
    }

    /**
     * Creates and returns a new Context instance for the specified locale and
     * servletPath.
     *
     * @param locale the requets locale
     * @param servletPath the requests servletPath
     * @return new Context instance
     */
    public static MockContext initContext(Locale locale, String servletPath) {
        if (locale == null) {
            throw new IllegalArgumentException("Locale cannot be null");
        }
        MockServletContext servletContext = new MockServletContext();
        String servletName = "click-servlet";
        MockServletConfig servletConfig = new MockServletConfig(servletName,
            servletContext);

        ClickServlet servlet = new ClickServlet();

        MockResponse response = new MockResponse();

        MockSession session = new MockSession(servletContext);

        MockRequest request = new MockRequest(locale, MockServletContext.DEFAULT_CONTEXT_PATH,
            servletPath, servletContext, session);

        return initContext(servletConfig, request, response, servlet);
    }

    /**
     * Creates and returns a new Context instance for the specified mock
     * objects.
     *
     * @param servletConfig the mock servletConfig
     * @param request the mock request
     * @param response the mock response
     * @param clickServlet the mock clickServlet
     * @return new Context instance
     */
    public static MockContext initContext(MockServletConfig servletConfig,
        MockRequest request, MockResponse response, ClickServlet clickServlet) {
        ActionEventDispatcher controlRegistry = new ActionEventDispatcher();
        return initContext(servletConfig, request, response, clickServlet,
            controlRegistry);
    }


    /**
     * Creates and returns a new Context instance for the specified mock
     * objects.
     *
     * @param servletConfig the mock servletConfig
     * @param request the mock request
     * @param response the mock response
     * @param clickServlet the mock clickServlet
     * @param controlRegistry the controlRegistry instance
     * @return new Context instance
     */
    public static MockContext initContext(MockServletConfig servletConfig,
        MockRequest request, MockResponse response, ClickServlet clickServlet,
        ActionEventDispatcher controlRegistry) {

        try {
            //Sanity checks
            if (servletConfig == null) {
                throw new IllegalArgumentException("ServletConfig cannot be null");
            }
            if (servletConfig.getServletContext() == null) {
                throw new IllegalArgumentException("ServletConfig.getServletContext() cannot return null");
            }
            if (request == null) {
                throw new IllegalArgumentException("Request cannot be null");
            }
            if (response == null) {
                throw new IllegalArgumentException("Response cannot be null");
            }
            if (clickServlet == null) {
                throw new IllegalArgumentException("ClickServlet cannot be null");
            }

            boolean isPost = true;
            if (request != null) {
                isPost = request.getMethod().equalsIgnoreCase("POST");
            }

            MockServletContext servletContext =
                (MockServletContext) servletConfig.getServletContext();

            servletContext.setAttribute(ClickServlet.MOCK_MODE_ENABLED,
                Boolean.TRUE);
            request.setAttribute(ClickServlet.MOCK_MODE_ENABLED, Boolean.TRUE);

            clickServlet.init(servletConfig);

            MockContext mockContext = new MockContext(servletConfig, request,
                response, isPost, clickServlet);

            ActionEventDispatcher.pushThreadLocalDispatcher(controlRegistry);
            Context.pushThreadLocalContext(mockContext);

            ConsoleLogService logService = (ConsoleLogService) ClickUtils.getLogService();
            logService.setLevel(ConsoleLogService.TRACE_LEVEL);
            return (MockContext) Context.getThreadLocalContext();
        } catch (Exception e) {
            throw new MockContainer.CleanRuntimeException(e);
        }
    }

    /**
     * Execute all listeners that was registered by the processed Controls.
     *
     * @return true if all listeners returned true, false otherwise
     */
    public boolean executeActionListeners() {
        ActionEventDispatcher controlRegistry = ActionEventDispatcher.getThreadLocalDispatcher();

        // Fire POST_ON_PROCESS events
        return controlRegistry.fireActionEvents(this, ActionEventDispatcher.POST_ON_PROCESS_EVENT);
    }

    /**
     * Fire all action events that was registered by the processed Controls, and
     * clears all registered listeners from the ControlRegistry.
     *
     * @deprecated use {@link #executeActionListeners()} instead
     *
     * @return true if all listeners returned true, false otherwise
     */
    public boolean fireActionEventsAndClearRegistry() {
        return executeActionListeners();
    }
}