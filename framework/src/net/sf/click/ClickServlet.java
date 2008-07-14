/*
 * Copyright 2004-2008 Malcolm A. Edgar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.click;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.click.service.ConfigService;
import net.sf.click.service.LogService;
import net.sf.click.service.XmlConfigService;
import net.sf.click.util.ClickUtils;
import net.sf.click.util.ErrorPage;
import net.sf.click.util.Format;
import net.sf.click.util.HtmlStringBuffer;
import net.sf.click.util.PageImports;
import net.sf.click.util.PropertyUtils;
import net.sf.click.util.RequestTypeConverter;
import net.sf.click.util.SessionMap;
import ognl.Ognl;
import ognl.OgnlException;
import ognl.TypeConverter;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.ParseErrorException;

/**
 * Provides the Click application HttpServlet.
 * <p/>
 * Generally developers will simply configure the <tt>ClickServlet</tt> and
 * will not use it directly in their code. For a Click web application to
 * function the <tt>ClickServlet</tt> must be configured in the web
 * application's <tt>/WEB-INF/web.xml</tt> file. A simple web application which
 * maps all <tt>*.htm</tt> requests to a ClickServlet is provided below.
 *
 * <pre class="codeConfig">
 * &lt;web-app&gt;
 *    &lt;servlet&gt;
 *       &lt;servlet-name&gt;<font color="blue">click-servlet</font>&lt;/servlet-name&gt;
 *       &lt;servlet-class&gt;<font color="red">net.sf.click.ClickServlet</font>&lt;/servlet-class&gt;
 *       &lt;load-on-startup&gt;<font color="red">0</font>&lt;/load-on-startup&gt;
 *    &lt;/servlet&gt;
 *    &lt;servlet-mapping&gt;
 *       &lt;servlet-name&gt;<font color="blue">click-servlet</font>&lt;/servlet-name&gt;
 *       &lt;url-pattern&gt;<font color="red">*.htm</font>&lt;/url-pattern&gt;
 *    &lt;/servlet-mapping&gt;
 * &lt;/web-app&gt; </pre>
 *
 * By default the <tt>ClickServlet</tt> will attempt to load an application
 * configuration file using the path: &nbsp; <tt>/WEB-INF/click.xml</tt>
 *
 * <h4>Servlet Mapping</h4>
 * By convention all Click page templates should have a .htm extension, and
 * the ClickServlet should be mapped to process all *.htm URL requests. With
 * this convention you have all the static HTML pages use a .html extension
 * and they will not be processed as Click pages.
 *
 * <h4>Load On Startup</h4>
 * Note you should always set <tt>load-on-startup</tt> element to be 0 so the
 * servlet is initialized when the server is started. This will prevent any
 * delay for the first client which uses the application.
 * <p/>
 * The <tt>ClickServlet</tt> performs as much work as possible at startup to
 * improve performance later on. The Click start up and caching strategy is
 * configured with the Click application mode in the "<tt>click.xml</tt>" file.
 * See the User Guide for information on how to configure the application mode.
 *
 * <h4>ConfigService</h4>
 *
 * A single application {@link ConfigService} instance is created by the ClickServlet at
 * startup. Once the ConfigService has been initialized it is stored in the
 * ServletContext using the key "<tt>net.sf.click.service.ConfigService</tt>".
 *
 * @author Malcolm Edgar
 */
public class ClickServlet extends HttpServlet {

    // -------------------------------------------------------------- Constants

    private static final long serialVersionUID = 1L;

    /**
     * The <tt>mock page reference</tt> request attribute: key: &nbsp;
     * <tt>mock_page_reference</tt>.
     * <p/>
     * This attribute stores the each Page instance as a request attribute.
     * <p/>
     * <b>Note:</b> a page is <tt>only</tt> stored as a request attribute
     * if the {@link #MOCK_MODE_ENABLED} attribute is set.
     */
    static final String MOCK_PAGE_REFERENCE = "mock_page_reference";

    /**
     * The <tt>mock mode</tt> request attribute: key: &nbsp;
     * <tt>mock_mode_enabled</tt>.
     * <p/>
     * If this attribute is set (the value does not matter) certain features
     * will be enabled which is needed for running Click in a mock environment.
     */
    static final String MOCK_MODE_ENABLED = "mock_mode_enabled";

    /**
     * The click application configuration service classname init parameter name:
     * &nbsp; "<tt>config-service-class</tt>".
     */
    protected final static String CONFIG_SERVICE_CLASS = "config-service-class";

    /**
     * The forwarded request marker attribute: &nbsp; "<tt>click-forward</tt>".
     */
    protected final static String CLICK_FORWARD = "click-forward";

    /**
     * The Page to forward to request attribute: &nbsp; "<tt>click-page</tt>".
     */
    protected final static String FORWARD_PAGE = "forward-page";

    // ------------------------------------------------------ Instance Varables

    /** The click application configuration service. */
    protected ConfigService configService;

    /** The application log service. */
    protected LogService logger;

    /** The request parameters OGNL type converter. */
    protected TypeConverter typeConverter;

    // --------------------------------------------------------- Public Methods

    /**
     * Initialize the Click servlet and the Velocity runtime.
     *
     * @see javax.servlet.GenericServlet#init()
     *
     * @throws ServletException if the application configuration service could
     * not be initialized
     */
    public void init() throws ServletException {

        try {

            // Create and initialize the application config service
            configService = createConfigService(getServletContext());
            initConfigService(getServletContext());

            logger = configService.getLogService();

            if (logger.isInfoEnabled()) {
                logger.info("initialized in " + configService.getApplicationMode()
                            + " mode");
            }

        } catch (Throwable e) {
            // In mock mode this exception can occur if click.xml is not
            // available.
            if (getServletContext().getAttribute(MOCK_MODE_ENABLED) != null) {
                return;
            }

            e.printStackTrace();

            String msg = "error while initializing Click servlet; throwing "
                         + "javax.servlet.UnavailableException";

            log(msg, e);

            throw new UnavailableException(e.toString());
        }
    }

    /**
     * @see javax.servlet.GenericServlet#destroy()
     */
    public void destroy() {

        try {

            // Destroy the application config service
            destroyConfigService(getServletContext());

        } catch (Throwable e) {
            // In mock mode this exception can occur if click.xml is not
            // available.
            if (getServletContext().getAttribute(MOCK_MODE_ENABLED) != null) {
                return;
            }

            e.printStackTrace();

            String msg = "error while destroying Click servlet, throwing "
                         + "javax.servlet.UnavailableException";

            log(msg, e);

        } finally {
            // Dereference the application config service
            configService = null;
        }

        super.destroy();
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Handle HTTP GET requests. This method will delegate the request to
     * {@link #handleRequest(HttpServletRequest, HttpServletResponse, boolean)}.
     *
     * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
     *
     * @param request the servlet request
     * @param response the servlet response
     * @throws ServletException if click app has not been initialized
     * @throws IOException if an I/O error occurs
     */
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        handleRequest(request, response, false);
    }

    /**
     * Handle HTTP POST requests. This method will delegate the request to
     * {@link #handleRequest(HttpServletRequest, HttpServletResponse, boolean)}.
     *
     * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
     *
     * @param request the servlet request
     * @param response the servlet response
     * @throws ServletException if click app has not been initialized
     * @throws IOException if an I/O error occurs
     */
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        handleRequest(request, response, true);
    }

    /**
     * Handle the given servlet request and render the results to the
     * servlet response.
     * <p/>
     * If an exception occurs within this method the exception will be delegated
     * to:
     * <p/>
     * {@link #handleException(HttpServletRequest, HttpServletResponse, boolean, Throwable, Class)}
     *
     * @param request the servlet request to process
     * @param response the servlet response to render the results to
     * @param isPost determines whether the request is a POST
     */
    protected void handleRequest(HttpServletRequest request,
        HttpServletResponse response, boolean isPost) {

        long startTime = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            HtmlStringBuffer buffer = new HtmlStringBuffer(200);
            buffer.append(request.getMethod());
            if (ServletFileUpload.isMultipartContent(request)) {
                buffer.append(" (multipart) ");
            } else {
                buffer.append(" ");
            }
            buffer.append(request.getRequestURL());
            logger.debug(buffer);
        }

        Page page = null;
        try {

            Context context = createContext(request, response, isPost);
            // Bind context to current thread
            Context.pushThreadLocalContext(context);

            // Check for fatal exception that occurred while creating Context
            Exception exception = (Exception)
                request.getAttribute(Context.CONTEXT_FATAL_EXCEPTION);

            if (exception != null) {
                // Process exception through Click's exception handler.
                throw exception;
            }

            ControlRegistry controlRegistry = createControlRegistry();
            // Bind ControlRegistry to current thread
            ControlRegistry.pushThreadLocalRegistry(controlRegistry);

            page = createPage(request);

            if (page.isStateful()) {
                synchronized (page) {
                    processPage(page);
                    processPageOnDestroy(page, startTime);
                    // Mark page as already destroyed for finally block
                    page = null;
                }

            } else {
                processPage(page);
            }

        } catch (Exception e) {
            Class pageClass =
                configService.getPageClass(ClickUtils.getResourcePath(request));

            handleException(request, response, isPost, e, pageClass);

        } catch (ExceptionInInitializerError eiie) {
            Throwable cause = eiie.getException();
            cause = (cause != null) ? cause : eiie;

            Class pageClass =
                configService.getPageClass(ClickUtils.getResourcePath(request));

            handleException(request, response, isPost, cause, pageClass);

        } finally {

            try {
                if (page != null) {
                    if (page.isStateful()) {
                        synchronized (page) {
                            processPageOnDestroy(page, startTime);
                        }

                    } else {
                        processPageOnDestroy(page, startTime);
                    }
                }

            } finally {
                // Only clear the context and logger when running in normal mode.
                if (request.getAttribute(MOCK_MODE_ENABLED) == null) {
                    Context.popThreadLocalContext();
                }
                ControlRegistry.popThreadLocalRegistry();
            }
        }
    }

    /**
     * Provides the application exception handler. The application exception
     * will be delegated to the configured error page. The default error page is
     * {@link ErrorPage} and the page template is "click/error.htm" <p/>
     * Applications which wish to provide their own customized error handling
     * must subclass ErrorPage and specify their page in the
     * "/WEB-INF/click.xml" application configuration file. For example:
     *
     * <pre class="codeConfig">
     *  &lt;page path=&quot;<span class="navy">click/error.htm</span>&quot; classname=&quot;<span class="maroon">com.mycorp.util.ErrorPage</span>&quot;/&gt;
     * </pre>
     *
     * If the ErrorPage throws an exception, it will be logged as an error and
     * then be rethrown nested inside a RuntimeException.
     *
     * @param request the servlet request with the associated error
     * @param response the servlet response
     * @param isPost boolean flag denoting the request method is "POST"
     * @param exception the error causing exception
     * @param pageClass the page class with the error
     */
    protected void handleException(HttpServletRequest request,
        HttpServletResponse response, boolean isPost, Throwable exception,
        Class pageClass) {

        if (exception instanceof ParseErrorException == false) {
            logger.error("handleException: ", exception);
        }

        ErrorPage finalizeRef = null;
        try {
            final ErrorPage errorPage = createErrorPage(pageClass, exception);

            finalizeRef = errorPage;

            errorPage.setError(exception);
            if (errorPage.getFormat() == null) {
                errorPage.setFormat(configService.createFormat());
            }
            errorPage.setHeaders(configService.getPageHeaders(ConfigService.ERROR_PATH));
            errorPage.setMode(configService.getApplicationMode());
            errorPage.setPageClass(pageClass);
            errorPage.setPath(ConfigService.ERROR_PATH);

            processPageFields(errorPage, new FieldCallback() {
                public void processField(String fieldName, Object fieldValue) {
                    if (fieldValue instanceof Control) {
                        Control control = (Control) fieldValue;
                        if (control.getName() == null) {
                            control.setName(fieldName);
                        }

                        if (!errorPage.getModel().containsKey(control.getName())) {
                            errorPage.addControl(control);
                        }
                    }
                }
            });

            if (errorPage.isStateful()) {
                synchronized (errorPage) {
                    processPage(errorPage);
                    processPageOnDestroy(errorPage, 0);
                    // Mark page as already destroyed for finally block
                    finalizeRef = null;
                }

            } else {
                processPage(errorPage);
            }

        } catch (Exception ex) {
            String message =
                "handleError: " + ex.getClass().getName()
                 + " thrown while handling " + exception.getClass().getName()
                 + ". Now throwing RuntimeException.";

            logger.error(message, ex);

            throw new RuntimeException(ex);

        } finally {
            if (finalizeRef != null) {
                if (finalizeRef.isStateful()) {
                    synchronized (finalizeRef) {
                        processPageOnDestroy(finalizeRef, 0);
                    }

                } else {
                    processPageOnDestroy(finalizeRef, 0);
                }
            }
        }
    }

    /**
     * Process the given page invoking its "on" event callback methods
     * and directing the response. This method does not invoke the "onDestroy()"
     * callback method.
     *
     * @param page the Page to process
     * @throws Exception if an error occurs
     */
    protected void processPage(Page page) throws Exception {

        final Context context = page.getContext();
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();
        final boolean isPost = context.isPost();

        // Support direct access of click-error.htm
        if (page instanceof ErrorPage) {
            ErrorPage errorPage = (ErrorPage) page;
            errorPage.setMode(configService.getApplicationMode());

            // Clear the control registry
            ControlRegistry.getThreadLocalRegistry().clearRegistry();
        }

        boolean continueProcessing = page.onSecurityCheck();

        String tracePrefix = null;

        if (logger.isTraceEnabled()) {
            tracePrefix = page.getClass().getName();
            tracePrefix = tracePrefix.substring(tracePrefix.lastIndexOf('.') + 1);
            tracePrefix = "   invoked: " + tracePrefix;

            logger.trace(tracePrefix + ".onSecurityCheck() : " + continueProcessing);
        }

        if (continueProcessing) {

            page.onInit();

            if (logger.isTraceEnabled()) {
                logger.trace(tracePrefix + ".onInit()");
            }

            if (page.hasControls()) {
                List controls = page.getControls();

                for (int i = 0, size = controls.size(); i < size; i++) {
                    Control control = (Control) controls.get(i);
                    control.onInit();

                    if (logger.isTraceEnabled()) {
                        String controlClassName = control.getClass().getName();
                        controlClassName = controlClassName.substring(controlClassName.lastIndexOf('.') + 1);
                        String msg =  "   invoked: '" + control.getName()
                            + "' " + controlClassName + ".onInit()";
                        logger.trace(msg);
                    }
                }
            }

            ControlRegistry controlRegistry = ControlRegistry.getThreadLocalRegistry();

            // Check if processing can continue
            if (!onProcessCheck(page, context, controlRegistry)) {
                return;
            }

            // Make sure dont process a forwarded request
            if (page.hasControls() && !context.isForward()) {
                List controls = page.getControls();

                for (int i = 0, size = controls.size(); i < size; i++) {
                    Control control = (Control) controls.get(i);

                    boolean onProcessResult = control.onProcess();
                    if (!onProcessResult) {
                        continueProcessing = false;
                    }

                    if (logger.isTraceEnabled()) {
                        String controlClassName = control.getClass().getName();
                        controlClassName = controlClassName.substring(controlClassName.lastIndexOf('.') + 1);

                        String msg =  "   invoked: '" + control.getName() + "' "
                            + controlClassName + ".onProcess() : " + onProcessResult;
                        logger.trace(msg);
                    }
                }

                // Fire all the registered action events
                continueProcessing = controlRegistry.fireActionEvents(context);

                if (logger.isTraceEnabled()) {
                    String msg =  "   invoked: Control listeners : " + continueProcessing;
                    logger.trace(msg);
                }
            }

            if (continueProcessing) {
                if (isPost) {
                    page.onPost();

                    if (logger.isTraceEnabled()) {
                        logger.trace(tracePrefix + ".onPost()");
                    }

                } else {
                    page.onGet();

                    if (logger.isTraceEnabled()) {
                        logger.trace(tracePrefix + ".onGet()");
                    }
                }

                page.onRender();

                if (logger.isTraceEnabled()) {
                    logger.trace(tracePrefix + ".onRender()");
                }

                if (page.hasControls()) {
                    List controls = page.getControls();

                    for (int i = 0, size = controls.size(); i < size; i++) {
                        Control control = (Control) controls.get(i);
                        control.onRender();

                        if (logger.isTraceEnabled()) {
                            String controlClassName = control.getClass().getName();
                            controlClassName = controlClassName.substring(controlClassName.lastIndexOf('.') + 1);
                            String msg =  "   invoked: '" + control.getName()
                                + "' " + controlClassName + ".onRender()";
                            logger.trace(msg);
                        }
                    }
                }
            }
        }

        if (StringUtils.isNotBlank(page.getRedirect())) {
            String url = page.getRedirect();

            if (url.charAt(0) == '/') {
                url = request.getContextPath() + url;

                // Check for two scenarios, one without parameters and one with:
                // #1. /context/my-page.jsp
                // #2. /context/my-page.jsp?param1=value&param2=other-page.jsp
                if (url.endsWith(".jsp")) {
                    url = StringUtils.replaceOnce(url, ".jsp", ".htm");
                } else if (url.indexOf(".jsp?") >= 0) {
                    url = StringUtils.replaceOnce(url, ".jsp?", ".htm?");
                }
            }

            url = response.encodeRedirectURL(url);

            if (logger.isTraceEnabled()) {
                logger.debug("   redirect: " + url);

            } else if (logger.isDebugEnabled()) {
                logger.debug("redirect: " + url);
            }

            response.sendRedirect(url);

        } else if (StringUtils.isNotBlank(page.getForward())) {
            request.setAttribute(CLICK_FORWARD, CLICK_FORWARD);

            if (logger.isTraceEnabled()) {
                logger.debug("   forward: " + page.getForward());

            } else if (logger.isDebugEnabled()) {
                logger.debug("forward: " + page.getForward());
            }

            if (page.getForward().endsWith(".jsp")) {
                // CLK-141. If path is a jsp page, change forward value.
                if (page.getPath().endsWith(".jsp")) {
                    page.setForward(page.getPath());
                }
                renderJSP(page);

            } else {
                RequestDispatcher dispatcher =
                    request.getRequestDispatcher(page.getForward());

                dispatcher.forward(request, response);
            }

        } else if (page.getPath() != null) {
            // CLK-141. If path is a jsp page, set forward value.
            if (page.getPath().endsWith(".jsp")) {
                page.setForward(page.getPath());
                renderJSP(page);

            } else {
                renderTemplate(page);
            }

        } else {
            if (logger.isTraceEnabled()) {
                logger.debug("   path not defined for " + page.getClass().getName());

            } else if (logger.isDebugEnabled()) {
                logger.debug("path not defined for " + page.getClass().getName());
            }
        }
    }

    /**
     * Render the Velocity template defined by the page's path.
     * <p/>
     * This method creates a Velocity Context using the Page's model Map and
     * then merges the template with the Context writing the result to the
     * HTTP servlet response.
     * <p/>
     * This method was adapted from org.apache.velocity.servlet.VelocityServlet.
     *
     * @param page the page template to merge
     * @throws Exception if an error occurs
     */
    protected void renderTemplate(Page page) throws Exception {

        long startTime = System.currentTimeMillis();

        final Map model = createTemplateModel(page);

        HttpServletResponse response = page.getContext().getResponse();

        response.setContentType(page.getContentType());

        Writer writer = response.getWriter();

        if (page.getHeaders() != null) {
            setPageResponseHeaders(response, page.getHeaders());
        }

        configService.getTemplateService().renderTemplate(page, model, writer);

        if (!configService.isProductionMode()) {
            HtmlStringBuffer buffer = new HtmlStringBuffer(50);
            if (logger.isTraceEnabled()) {
                buffer.append("   ");
            }
            buffer.append("renderTemplate: ");
            if (!page.getTemplate().equals(page.getPath())) {
                buffer.append(page.getPath());
                buffer.append(",");
            }
            buffer.append(page.getTemplate());
            buffer.append(" - ");
            buffer.append(System.currentTimeMillis() - startTime);
            buffer.append(" ms");
            logger.info(buffer);
        }
    }

    /**
     * Render the given page as a JSP to the response.
     *
     * @param page the page to render
     * @throws Exception if an error occurs rendering the JSP
     */
    protected void renderJSP(Page page) throws Exception {

        long startTime = System.currentTimeMillis();

        HttpServletRequest request = page.getContext().getRequest();

        HttpServletResponse response = page.getContext().getResponse();

        setRequestAttributes(page);

        RequestDispatcher dispatcher = null;

        // Since the "getTemplate" returns the page.getPath() by default, which is *.htm
        // we need to change to *.jsp in order to compare to the page.getForward()
        String jspTemplate = StringUtils.replace(page.getTemplate(), ".htm", ".jsp");
 
        if (page.getForward().equals(jspTemplate)) {
            dispatcher = request.getRequestDispatcher(page.getForward());

        } else {
            dispatcher = request.getRequestDispatcher(page.getTemplate());
        }

        dispatcher.forward(request, response);

        if (!configService.isProductionMode()) {
            HtmlStringBuffer buffer = new HtmlStringBuffer(50);
            buffer.append("renderJSP: ");
            if (!page.getTemplate().equals(page.getForward())) {
                buffer.append(page.getTemplate());
                buffer.append(",");
            }
            buffer.append(page.getForward());
            buffer.append(" - ");
            buffer.append(System.currentTimeMillis() - startTime);
            buffer.append(" ms");
            logger.info(buffer);
        }
    }

    /**
     * Return a new Page instance for the given request. This method will
     * invoke {@link #initPage(String, Class, HttpServletRequest)} to create
     * the Page instance and then set the properties on the page.
     *
     * @param request the servlet request
     * @return a new Page instance for the given request
     */
    protected Page createPage(HttpServletRequest request) {

        // Log request parameters
        if (logger.isTraceEnabled()) {
            Map requestParams = new TreeMap();

            Enumeration e = request.getParameterNames();
            while (e.hasMoreElements()) {
                String name = e.nextElement().toString();
                String value = request.getParameter(name);
                requestParams.put(name, value);
            }

            Iterator i = requestParams.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                String name = entry.getKey().toString();
                String value = entry.getValue().toString();

                String msg = "   request param: " + name + "="
                    + ClickUtils.limitLength(value, 40);

                logger.trace(msg);
            }
        }

        String path = Context.getThreadLocalContext().getResourcePath();

        if (request.getAttribute(FORWARD_PAGE) != null) {
            Page forwardPage = (Page) request.getAttribute(FORWARD_PAGE);

            if (forwardPage.getFormat() == null) {
                forwardPage.setFormat(configService.createFormat());
            }

            request.removeAttribute(FORWARD_PAGE);

            return forwardPage;
        }

        Class pageClass = configService.getPageClass(path);

        if (pageClass == null) {
            pageClass = configService.getNotFoundPageClass();
            path = ConfigService.NOT_FOUND_PATH;
        }

        final Page page = initPage(path, pageClass, request);

        if (page.getFormat() == null) {
            page.setFormat(configService.createFormat());
        }

        return page;
    }

    /**
     * Process the given pages controls <tt>onDestroy</tt> methods, reset the pages
     * navigation state and process the pages <tt>onDestroy</tt> method.
     *
     * @param page the page to process
     * @param startTime the start time to log if greater than 0 and not in
     * production mode
     */
    protected void processPageOnDestroy(Page page, long startTime) {
        if (page.hasControls()) {
            List controls = page.getControls();

            for (int i = 0, size = controls.size(); i < size; i++) {
                try {
                    Control control = (Control) controls.get(i);
                    control.onDestroy();

                    if (logger.isTraceEnabled()) {
                        String controlClassName = control.getClass().getName();
                        controlClassName = controlClassName.substring(controlClassName.lastIndexOf('.') + 1);
                        String msg =  "   invoked: '" + control.getName()
                        + "' " + controlClassName + ".onDestroy()";
                        logger.trace(msg);
                    }
                } catch (Throwable error) {
                    logger.error(error.toString(), error);
                }
            }
        }

        // Reset the page navigation state
        try {
            // Reset the path
            String path = page.getContext().getResourcePath();
            page.setPath(path);

            // Reset the foward
            if (configService.isJspPage(path)) {
                page.setForward(StringUtils.replace(path, ".htm", ".jsp"));
            } else {
                page.setForward((String) null);
            }

            // Reset the redirect
            page.setRedirect((String) null);

        } catch (Throwable error) {
            logger.error(error.toString(), error);
        }

        try {
            page.onDestroy();

            if (page.isStateful()) {
                page.getContext().setSessionAttribute(page.getClass().getName(), page);
            } else {
                page.getContext().removeSessionAttribute(page.getClass().getName());
            }

            if (logger.isTraceEnabled()) {
                String shortClassName = page.getClass().getName();
                shortClassName =
                    shortClassName.substring(shortClassName.lastIndexOf('.') + 1);
                logger.trace("   invoked: " + shortClassName + ".onDestroy()");
            }

            if (!configService.isProductionMode() && startTime > 0) {
                logger.info("handleRequest:  " + page.getPath() + " - "
                        + (System.currentTimeMillis() - startTime)
                        + " ms");
            }

        } catch (Throwable error) {
            logger.error(error.toString(), error);
        }
    }

    /**
     * Initialize a new page instance using
     * {@link #newPageInstance(String, Class, HttpServletRequest)} method and
     * setting format, headers and the forward if a JSP.
     * <p/>
     * This method will also automatically register any public Page controls
     * in the page's model. When the page is created any public visible
     * page Control variables will be automatically added to the page using
     * the method {@link Page#addControl(Control)} method. If the controls name
     * is not defined it is set to the member variables name before it is added
     * to the page.
     * <p/>
     * This feature saves you from having to manually add the controls yourself.
     * If you don't want the controls automatically added, simply declare them
     * as non public variables.
     * <p/>
     * An example auto control registration is provided below. In this example
     * the Table control is automatically added to the model using the name
     * <tt>"table"</tt>, and the ActionLink controls are added using the names
     * <tt>"editDetailsLink"</tt> and <tt>"viewDetailsLink"</tt>.
     *
     * <pre class="codeJava">
     * <span class="kw">public class</span> OrderDetailsPage <span class="kw">extends</span> Page {
     *
     *     <span class="kw">public</span> Table table = <span class="kw">new</span> Table();
     *     <span class="kw">public</span> ActionLink editDetailsLink = <span class="kw">new</span> ActionLink();
     *     <span class="kw">public</span>ActionLink viewDetailsLink = <span class="kw">new</span> ActionLink();
     *
     *     <span class="kw">public</span> OrderDetailsPage() {
     *         ..
     *     }
     * } </pre>
     *
     * @param path the page path
     * @param pageClass the page class
     * @param request the page request
     * @return initialized page
     */
    protected Page initPage(String path, Class pageClass,
            HttpServletRequest request) {

        try {
            Page newPage = null;

            // Look up the page in the users session.
            HttpSession session = request.getSession(false);
            if (session != null) {
                newPage = (Page) session.getAttribute(pageClass.getName());
            }

            if (newPage == null) {
                newPage = newPageInstance(path, pageClass, request);

                if (logger.isTraceEnabled()) {
                    String shortClassName = pageClass.getName();
                    shortClassName =
                        shortClassName.substring(shortClassName.lastIndexOf('.') + 1);
                    logger.trace("   invoked: " + shortClassName + ".<<init>>");
                }
            }

            activatePageInstance(newPage);

            if (newPage.getHeaders() == null) {
                newPage.setHeaders(configService.getPageHeaders(path));
            }

            newPage.setPath(path);

            if (configService.isJspPage(path)) {
                newPage.setForward(StringUtils.replace(path, ".htm", ".jsp"));
            }

            // Bind to final variable to enable callback processing
            final Page page = newPage;

            if (configService.isPagesAutoBinding()) {
                // Automatically add public controls to the page
                processPageFields(newPage, new FieldCallback() {
                    public void processField(String fieldName, Object fieldValue) {
                        if (fieldValue instanceof Control) {
                            Control control = (Control) fieldValue;
                            if (control.getName() == null) {
                                control.setName(fieldName);
                            }

                            if (!page.getModel().containsKey(control.getName())) {
                                page.addControl(control);
                            }
                        }
                    }
                });

                processPageRequestParams(page);
            }

            // In mock mode add the Page instance as a request attribute.
            if (request.getAttribute(MOCK_MODE_ENABLED) != null) {
                request.setAttribute(MOCK_PAGE_REFERENCE, page);
            }

            return newPage;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Process the page binding any request parameters to any public Page
     * fields with the same name which are "primitive" types. These types
     * include string, numbers and booleans.
     * <p/>
     * Type conversion is performed using the <tt>TypeConverter</tt>
     * returned by the {@link #getTypeConverter()} method.
     *
     * @param page the page whose fields are to be processed
     * @throws OgnlException if an error occurs
     */
    protected void processPageRequestParams(Page page) throws OgnlException {

        if (configService.getPageFields(page.getClass()).isEmpty()) {
            return;
        }

        Map ognlContext = null;

        boolean customConverter =
            ! getTypeConverter().getClass().equals(RequestTypeConverter.class);

        HttpServletRequest request = page.getContext().getRequest();

        for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement().toString();
            String value = request.getParameter(name);

            if (StringUtils.isNotBlank(value)) {

                Field field = configService.getPageField(page.getClass(), name);

                if (field != null) {
                    Class type = field.getType();

                    if (customConverter
                        || (type.isPrimitive()
                            || String.class.isAssignableFrom(type)
                            || Number.class.isAssignableFrom(type)
                            || Boolean.class.isAssignableFrom(type))) {

                        if (ognlContext == null) {
                            ognlContext = Ognl.createDefaultContext(page, null, getTypeConverter());
                        }

                        PropertyUtils.setValueOgnl(page, name, value, ognlContext);

                        if (logger.isTraceEnabled()) {
                            logger.trace("   auto bound variable: " + name + "=" + value);
                        }
                    }
                }
            }
        }
    }

    /**
     * Return a new Page instance for the given page path, class and request.
     * <p/>
     * The default implementation of this method simply creates new page
     * instances:
     * <pre class="codeJava">
     * <span class="kw">protected</span> Page newPageInstance(String path, Class pageClass,
     *     HttpServletRequest request) <span class="kw">throws</span> Exception {
     *
     *     <span class="kw">return</span> (Page) pageClass.newInstance();
     * } </pre>
     *
     * This method is designed to be overridden by applications providing their
     * own page creation patterns.
     * <p/>
     * A typical example of this would be with Inversion of Control (IoC)
     * frameworks such as Spring or HiveMind. For example a Spring application
     * could override this method and use a <tt>ApplicationContext</tt> to instantiate
     * new Page objects:
     * <pre class="codeJava">
     * <span class="kw">protected</span> Page newPageInstance(String path, Class pageClass,
     *     HttpServletRequest request) <span class="kw">throws</span> Exception {
     *
     *     String beanName = path.substring(0, path.indexOf(<span class="st">"."</span>));
     *
     *     <span class="kw">if</span> (applicationContext.containsBean(beanName)) {
     *         Page page = (Page) applicationContext.getBean(beanName);
     *
     *     } <span class="kw">else</span> {
     *         page = (Page) pageClass.newIntance();
     *     }
     *
     *     <span class="kw">return</span> page;
     * } </pre>
     *
     * @param path the request page path
     * @param pageClass the page Class the request is mapped to
     * @param request the page request
     * @return a new Page object
     * @throws Exception if an error occurs creating the Page
     */
    protected Page newPageInstance(String path, Class pageClass,
            HttpServletRequest request) throws Exception {

        return (Page) pageClass.newInstance();
    }

    /**
     * Provides an extension point for ClickServlet sub classes to activate
     * stateful page which may have been deserialized.
     * <p/>
     * This method does nothing and is designed for extension.
     *
     * @param page the page instance to activate
     */
    protected void activatePageInstance(Page page) {
    }

    /**
     * Return a new VelocityContext for the given pages model and Context.
     * <p/>
     * The following values automatically added to the VelocityContext:
     * <ul>
     * <li>any public Page fields using the fields name</li>
     * <li>context - the Servlet context path, e.g. /mycorp</li>
     * <li>format - the {@link Format} object for formatting the display of objects</li>
     * <li>imports - the {@link PageImports} object</li>
     * <li>messages - the page messages bundle</li>
     * <li>path - the page of the page template to render</li>
     * <li>request - the pages servlet request</li>
     * <li>response - the pages servlet request</li>
     * <li>session - the {@link SessionMap} adaptor for the users HttpSession</li>
     * </ul>
     *
     * @param page the page to create a VelocityContext for
     * @return a new VelocityContext
     */
    protected Map createTemplateModel(final Page page) {

        if (configService.isPagesAutoBinding()) {
            processPageFields(page, new FieldCallback() {
                public void processField(String fieldName, Object fieldValue) {
                    if (fieldValue instanceof Control == false) {
                        page.getModel().put(fieldName, fieldValue);

                    } else {
                        // Add any controls not already added to model
                        Control control = (Control) fieldValue;
                        if (!page.getModel().containsKey(control.getName())) {
                            page.addControl(control);
                        }
                    }
                }
            });
        }

        final Map model = new HashMap(page.getModel());

        final HttpServletRequest request = page.getContext().getRequest();

        Object pop = model.put("request", request);
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                         + " model contains an object keyed with reserved "
                         + "name \"request\". The page model object "
                         + pop + " has been replaced with the request object";
            logger.warn(msg);
        }

        pop = model.put("response", page.getContext().getResponse());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                         + " model contains an object keyed with reserved "
                         + "name \"response\". The page model object "
                         + pop + " has been replaced with the response object";
            logger.warn(msg);
        }

        SessionMap sessionMap = new SessionMap(request.getSession(false));
        pop = model.put("session", sessionMap);
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                         + " model contains an object keyed with reserved "
                         + "name \"session\". The page model object "
                         + pop + " has been replaced with the request "
                         + " session";
            logger.warn(msg);
        }

        pop = model.put("context", request.getContextPath());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                         + " model contains an object keyed with reserved "
                         + "name \"context\". The page model object "
                         + pop + " has been replaced with the request "
                         + " context path";
            logger.warn(msg);
        }

        Format format = page.getFormat();
        if (format != null) {
            pop = model.put("format", format);
            if (pop != null && !page.isStateful()) {
                String msg = page.getClass().getName() + " on "
                        + page.getPath()
                        + " model contains an object keyed with reserved "
                        + "name \"format\". The page model object " + pop
                        + " has been replaced with the format object";
                logger.warn(msg);
            }
        }

        String path = page.getPath();
        if (path != null) {
           pop = model.put("path", path);
            if (pop != null && !page.isStateful()) {
                String msg = page.getClass().getName() + " on "
                        + page.getPath()
                        + " model contains an object keyed with reserved "
                        + "name \"path\". The page model object " + pop
                        + " has been replaced with the page path";
                logger.warn(msg);
            }
        }

        pop = model.put("messages", page.getMessages());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                         + " model contains an object keyed with reserved "
                         + "name \"messages\". The page model object "
                         + pop + " has been replaced with the request "
                         + " messages";
            logger.warn(msg);
        }

        PageImports pageImports = page.getPageImports();
        pageImports.popuplateTemplateModel(model);

        return model;
    }

    /**
     * Set the HTTP headers in the servlet response. The Page response headers
     * are defined in {@link Page#getHeaders()}.
     *
     * @param response the response to set the headers in
     * @param headers the map of HTTP headers to set in the response
     */
    protected void setPageResponseHeaders(HttpServletResponse response,
            Map headers) {

        for (Iterator i = headers.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = entry.getKey().toString();
            Object value = entry.getValue();

            if (value instanceof String) {
                String strValue = (String) value;
                if (!strValue.equalsIgnoreCase("Content-Encoding")) {
                    response.setHeader(name, strValue);
                }

            } else if (value instanceof Date) {
                long time = ((Date) value).getTime();
                response.setDateHeader(name, time);

            } else {
                int intValue = ((Integer) value).intValue();
                response.setIntHeader(name, intValue);
            }
        }
    }

    /**
     * Set the page model, context, format, messages and path as request
     * attributes to support JSP rendering. These request attributes include:
     * <ul>
     * <li>any public Page fields using the fields name</li>
     * <li>context - the Servlet context path, e.g. /mycorp</li>
     * <li>format - the {@link Format} object for formatting the display of objects</li>
     * <li>forward - the page forward path, if defined</li>
     * <li>imports - the {@link PageImports} object</li>
     * <li>messages - the page messages bundle</li>
     * <li>path - the page of the page template to render</li>
     * </ul>
     *
     * @param page the page to set the request attributes on
     */
    protected void setRequestAttributes(final Page page) {
        final HttpServletRequest request = page.getContext().getRequest();

        processPageFields(page, new FieldCallback() {
            public void processField(String fieldName, Object fieldValue) {
                if (fieldValue instanceof Control == false) {
                    request.setAttribute(fieldName, fieldValue);
                }  else {
                    // Add any controls not already added to model
                    Control control = (Control) fieldValue;
                    if (!page.getModel().containsKey(control.getName())) {
                        page.addControl(control);
                    }
                }
            }
        });

        Map model = page.getModel();
        for (Iterator i = model.entrySet().iterator(); i.hasNext();)  {
            Map.Entry entry = (Map.Entry) i.next();
            String name = entry.getKey().toString();
            Object value = entry.getValue();

            request.setAttribute(name, value);
        }

        request.setAttribute("context", request.getContextPath());
        if (model.containsKey("context")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                            + " model contains an object keyed with reserved "
                            + "name \"context\". The request attribute "
                            + "has been replaced with the request "
                            + "context path";
            logger.warn(msg);
        }

        request.setAttribute("format", page.getFormat());
        if (model.containsKey("format")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                            + " model contains an object keyed with reserved "
                            + "name \"format\". The request attribute "
                            + "has been replaced with the format object";
            logger.warn(msg);
        }

        request.setAttribute("forward", page.getForward());
        if (model.containsKey("forward")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                            + " model contains an object keyed with reserved "
                            + "name \"forward\". The request attribute "
                            + "has been replaced with the page path";
            logger.warn(msg);
        }

        request.setAttribute("path", page.getPath());
        if (model.containsKey("path")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                            + " model contains an object keyed with reserved "
                            + "name \"path\". The request attribute "
                            + "has been replaced with the page path";
            logger.warn(msg);
        }

        request.setAttribute("messages", page.getMessages());
        if (model.containsKey("messages")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                            + " model contains an object keyed with reserved "
                            + "name \"messages\". The request attribute "
                            + "has been replaced with the page messages";
            logger.warn(msg);
        }

        PageImports pageImports = page.getPageImports();
        pageImports.popuplateRequest(request, model);
    }

    /**
     * Return the request parameters OGNL <tt>TypeConverter</tt>. By default
     * this method returns a {@link RequestTypeConverter} instance.
     *
     * @return the request parameters OGNL <tt>TypeConverter</tt>
     */
    protected TypeConverter getTypeConverter() {
        if (typeConverter == null) {
            typeConverter = new RequestTypeConverter();
        }

        return typeConverter;
    }

    /**
     * Creates and returns a new Context instance for this path, class and
     * request.
     * <p/>
     * The default implementation of this method simply creates a new Context
     * instance.
     * <p/>
     * Subclasses can override this method to provide a custom Context.
     *
     * @param request the page request
     * @param response the page response
     * @param isPost true if this is a post request, false otherwise
     * @return a Context instance
     */
    protected Context createContext(HttpServletRequest request,
            HttpServletResponse response, boolean isPost) {

        Context context = new Context(getServletContext(),
                                      getServletConfig(),
                                      request,
                                      response,
                                      isPost,
                                      this);
        return context;
    }

    /**
     * Creates and returns a new ControlRegistry instance.
     *
     * @return the new ControlRegistry instance
     */
    protected ControlRegistry createControlRegistry() {
        return new ControlRegistry();
    }

    /**
     * Creates and returns a new ErrorPage instance.
     * <p/>
     * This method creates the custom page as specified in <tt>click.xml</tt>,
     * otherwise the default ErrorPage instance.
     * <p/>
     * Subclasses can override this method to provide custom ErrorPages tailored
     * for specific exceptions.
     * <p/>
     * <b>Note</b> you can safely use {@link net.sf.click.Context} in this
     * method.
     *
     * @param pageClass the page class with the error
     * @param exception the error causing exception
     * @return a new ErrorPage instance
     */
    protected ErrorPage createErrorPage(Class pageClass, Throwable exception) {
        try {
            return (ErrorPage) configService.getErrorPageClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the application configuration service instance.
     *
     * @return the application configuration service instance
     */
    protected ConfigService getConfigService() {
        return configService;
    }

    /**
     * Return a new Page instance for the given path.
     *
     * @param path the Page path configured in the click.xml file
     * @param request the Page request
     * @return a new Page object
     * @throws IllegalArgumentException if the Page is not found
     */
    protected Page createPage(String path, HttpServletRequest request) {
        Class pageClass = getConfigService().getPageClass(path);

        if (pageClass == null) {
            String msg = "No Page class configured for path: " + path;
            throw new IllegalArgumentException(msg);
        }

        return initPage(path, pageClass, request);
    }

    /**
     * Return a new Page instance for the page Class.
     *
     * @param pageClass the class of the Page to create
     * @param request the Page request
     * @return a new Page object
     * @throws IllegalArgumentException if the Page Class is not configured
     * with a unique path
     */
    protected Page createPage(Class pageClass, HttpServletRequest request) {
        String path = getConfigService().getPagePath(pageClass);

        if (path == null) {
            String msg =
                "No path configured for Page class: " + pageClass.getName();
            throw new IllegalArgumentException(msg);
        }

        return initPage(path, pageClass, request);
    }

    // ------------------------------------------------ Package Private Methods

   /**
    * Create a Click application ConfigService instance.
    *
    * @param servletContext the Servlet Context
    * @return a new application ConfigService instance
    * @throws Exception if an initialization error occurs
    */
    ConfigService createConfigService(ServletContext servletContext)
        throws Exception {

        Class serviceClass = XmlConfigService.class;

        String classname = servletContext.getInitParameter(CONFIG_SERVICE_CLASS);
        if (StringUtils.isNotBlank(classname)) {
            serviceClass = ClickUtils.classForName(classname);
        }

        return (ConfigService) serviceClass.newInstance();
    }

    /**
     * Initialize the Click application <tt>ConfigService</tt> instance and bind
     * it as a ServletContext attribute using the key
     * "<tt>net.sf.click.service.ConfigService</tt>".
     * <p/>
     * This method will use the configuration service class specified by the
     * {@link #CONFIG_SERVICE_CLASS} parameter, otherwise it will create a
     * {@link net.sf.click.service.XmlConfigService} instance.
     *
     * @param servletContext the servlet context to retrieve the
     * {@link #CONFIG_SERVICE_CLASS} from
     * @return the application configuration service instance
     * @throws RuntimeException if the configuration service cannot be
     * initialized
     */
    void initConfigService(ServletContext servletContext) {

        if (configService != null) {
            try {

                // Note this order is very important as components need to lookup
                // the configService out of the ServletContext while the service
                // is being initialized.
                servletContext.setAttribute(ConfigService.CONTEXT_NAME, configService);

                // Initialize the ConfigService instance
                configService.onInit(servletContext);

            } catch (Exception e) {

                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Destroy the application configuration service instance and remove
     * it from the ServletContext attribute.
     *
     * @param servletContext the servlet context
     * @throws RuntimeException if the configuration service cannot be
     * destroyed
     */
    void destroyConfigService(ServletContext servletContext) {

        if (configService != null) {

            try {
                configService.onDestroy();

            } catch (Exception e) {

                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            } finally {
                servletContext.setAttribute(ConfigService.CONTEXT_NAME, null);
            }
        }
    }

    /**
     * Process all the Pages public fields using the given callback.
     *
     * @param page the page to obtain the fields from
     * @param callback the fields iterator callback
     */
    void processPageFields(Page page, FieldCallback callback) {

        Field[] fields = configService.getPageFieldArray(page.getClass());

        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];

                try {
                    Object fieldValue = field.get(page);

                    if (fieldValue != null) {
                        callback.processField(field.getName(), fieldValue);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Returns true if the Click onProcess event should start, false otherwise.
     * <p/>
     * This method act as a hook for subclasses to determine if onProcess should
     * fire or not.
     *
     * @param page the page to process
     * @param context the request context
     * @param controlRegistry the request control registry
     * @return true if processing should continue, false otherwise
     */
    boolean onProcessCheck(Page page, Context context, ControlRegistry controlRegistry) {
        return true;
    }

    // ---------------------------------------------------------- Inner Classes

    /**
     * Field iterator callback.
     */
    static interface FieldCallback {

        public void processField(String fieldName, Object fieldValue);

    }

}
