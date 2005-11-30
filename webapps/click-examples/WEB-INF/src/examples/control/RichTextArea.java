/*
 * Copyright 2004-2005 Malcolm A. Edgar
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
package examples.control;

import java.text.MessageFormat;

import net.sf.click.control.Field;
import net.sf.click.control.TextArea;
import net.sf.click.util.HtmlStringBuffer;

/**
 * Provides a HTML Rich TextArea editor control using the
 * <a href="http://sourceforge.net/projects/tinymce/">TinyMCE</a>
 * JavaScript library.
 * <p/>
 * To utilize this control in your application include <tt>tiny_mce</tt>
 * JavaScript library in the web apps root directory.
 *
 * @see TextArea
 *
 * @author Malcolm Edgar
 */
public class RichTextArea extends TextArea {

    private static final long serialVersionUID = 4955743445097747828L;

    /** The TinyMCE JavaScript import. */
    protected static final String HTML_IMPORTS =
        "<script type=\"text/javascript\" src=\"{0}/tiny_mce/tiny_mce.js\"></script>\n";

    /**
     * The textarea TinyMCE theme [<tt>simle</tt> | <tt>advanced</tt>],
     * default value: &nbsp; <tt>"simle"</tt>
     */
    protected String theme = "simple";

    /**
     * Create a TinyMCE rich TextArea control with the given name.
     *
     * @param name the name of the control
     */
    public RichTextArea(String name) {
        super(name);
    }

    /**
     * Return the textarea TinyMCE theme.
     *
     * @return the textarea TinyMCE theme
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Return the JavaScript include: &nbsp; <tt>"tiny_mce/tiny_mce.js"</tt>
     *
     * @see Field#getHtmlImports()
     */
    public String getHtmlImports() {
        String[] args = { getContext().getRequest().getContextPath() };
        return MessageFormat.format(HTML_IMPORTS, args);
    }

    /**
     * This method overrides the TextArea <tt>toString()</tt> method to
     * add TinyMCE JavaScript initialization code.
     *
     * @see TextArea#toString()
     */
    public String toString() {
        HtmlStringBuffer buffer = new HtmlStringBuffer();
        buffer.append(super.toString());

        buffer.elementStart("script");
        buffer.appendAttribute("type", "text/javascript");
        buffer.closeTag();
        buffer.append("\ntinyMCE.init({\n");
        buffer.append("   theme : \"");
        buffer.append(getTheme());
        buffer.append("\",\n");
        buffer.append("   mode: \"exact\",\n");
        buffer.append("   elements : \"");
        buffer.append(getId());
        buffer.append("\"\n");
        buffer.append("});\n");
        buffer.elementEnd("script");

        return buffer.toString();
    }

}
