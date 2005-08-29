/*
 * Copyright 2005 Malcolm A. Edgar
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
package net.sf.click.control;

/**
 * Provides a Label display control. The Label control performs no server side
 * processing, and is used primarily to add descriptive labels or horizontal
 * rules to auto rendered forms. For example:
 *
 * <pre class="codeJava">
 * Form form = <span class="kw">new</span> Form(getContext(), <span class="st">"form"</span>);
 * ..
 *
 * form.add(<span class="kw">new</span> Label(<span class="st">"&lt;hr/&gt;"</span>)); </pre>
 *
 * HTML output:
 * <pre class="codeHtml">
 * &lt;tr&gt;&lt;td colspan='2' align='left'&gt;&lt;hr/&gt;&lt;/td&gt;&lt;/tr&gt; </pre>
 *
 * @author Malcolm Edgar
 * @version $Id$
 */
public class Label extends Field {

    private static final long serialVersionUID = -6211362222692407741L;

    // ----------------------------------------------------------- Constructors

    /**
     * Create a Label display control.
     *
     * @param label the label display value
     */
    public Label(String label) {
        super(label);
    }

    /**
     * Create a Label with no label/name defined, <b>please note</b> the
     * control's name must be defined before it is valid.
     * <p/>
     * <div style="border: 1px solid red;padding:0.5em;">
     * No-args constructors are provided for Java Bean tools support and are not
     * intended for general use. If you create a control instance using a
     * no-args constructor you must define its name before adding it to its
     * parent. </div>
     */
    public Label() {
        super();
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Returns true.
     *
     * @see Field#onProcess()
     */
    public boolean onProcess() {
        return true;
    }

    /**
     * Returns the label.
     *
     * @see Object#toString()
     */
    public String toString() {
        return getLabel();
    }
}
