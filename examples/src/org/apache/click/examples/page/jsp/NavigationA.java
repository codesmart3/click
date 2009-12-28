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
package org.apache.click.examples.page.jsp;

import org.apache.click.control.ActionLink;
import org.apache.click.examples.page.BorderPage;
import org.apache.click.util.Bindable;

/**
 * Provides an navigation example Page demonstrating forward and redirect
 * page navigation.
 *
 * @author Malcolm Edgar
 */
public class NavigationA extends BorderPage {

    @Bindable protected ActionLink forwardLink = new ActionLink("forwardLink", this, "onForwardClick");
    @Bindable protected ActionLink forwardParamLink = new ActionLink("forwardParamLink", this, "onForwardParamClick");
    @Bindable protected ActionLink redirectLink = new ActionLink("redirectLink", this, "onRedirectClick");
    @Bindable protected ActionLink redirectParamLink = new ActionLink("redirectParamLink", this, "onRedirectParamClick");

    // --------------------------------------------------------- Event Handlers

    /**
     * @see org.apache.click.Page#onInit()
     */
    @Override
    public void onInit() {
        super.onInit();

        // Initialise param ActionLink values from any parameters passed through
        // from other pages via forwards or redirects.
        Integer number = new Integer(1);

        // If request has been forwarded
        if (getContext().isForward()) {
            // If a request attribute was passed increment its value.
            Integer param = (Integer) getContext().getRequestAttribute("param");
            if (param != null) {
                number = new Integer(param.intValue() + 1);
            }

        // Else request may have been redirected
        } else {
            String param = getContext().getRequest().getParameter("param");
            if (param != null) {
                number = new Integer(Integer.parseInt(param) + 1);
            }
        }

        forwardParamLink.setValue(number.toString());
        redirectParamLink.setValue(number.toString());
    }

    public boolean onForwardClick() {
        setForward(getTarget());
        return false;
    }

    public boolean onForwardParamClick() {
        Integer param = forwardParamLink.getValueInteger();
        getContext().setRequestAttribute("param", param);
        setForward(getTarget());
        return false;
    }

    public boolean onRedirectClick() {
        setRedirect(getTarget());
        return false;
    }

    public boolean onRedirectParamClick() {
        setRedirect(getTarget() + "?param=" + redirectParamLink.getValue());
        return false;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Target template to forward to.
     * <p/>
     * In order to forward to a Page with a JSP template, we specify the target
     * with an htm extension so that ClickServlet will process the Page.
     * After the Page NavigationB.java is processed, Click will forward to the
     * underlying template /jsp/navigation-b.jsp.
     */
    public String getTarget() {
        return "/jsp/navigation-b.htm";
    }

    /**
     * Returns the name of the border template: &nbsp; <tt>"/border-template.jsp"</tt>
     *
     * @see org.apache.click.Page#getTemplate()
     */
    @Override
    public String getTemplate() {
        return "/border-template.jsp";
    }
}
