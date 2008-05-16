/*
 * Copyright 2008 Malcolm A. Edgar
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
package net.sf.click.extras.control;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.sf.click.control.Option;
import net.sf.click.control.Select;
import net.sf.click.util.HtmlStringBuffer;

import org.apache.commons.lang.StringUtils;

/**
 * Provides a Country Select control: &nbsp; &lt;select&gt;&lt;/select&gt;.
 *
 * <table class='htmlHeader' cellspacing='6'>
 * <tr>
 * <td>Select</td>
 * <td>
 * <select title='Select Control'>
 * <option value="AL">Albania</option>
 * <option value="DZ">Algeria</option>
 * <option value="AR">Argentina</option>
 * </select>
 * </td>
 * </tr>
 * </table>
 *
 * The CountrySelect Control that is fully i18n aware. The country list is not
 * hardcoded but genereated by the JDK, i.e. there's an <code>Option</code> for
 * each country that has a <i>supported</i> <code>Locale</code> from the side
 * of the JDK.
 * <p/>
 *
 * <i>Note:</i> Newer versions of the JDK seems to support more <code>Locale</code>s.
 * <p/>
 * <i>Obs.:</i> Especially practial for registration forms.
 *
 * @author Ahmed Mohombe
 */
public class CountrySelect extends Select {

    private static final long serialVersionUID = 1L;

    /** The Select comparator locale. */
    private Locale locale;

    // ----------------------------------------------------------- Constructors

    /**
     * Create a CountrySelect field with the given name.
     *
     * @param name the name of the field
     */
    public CountrySelect(String name) {
        super(name);
    }

    /**
     * Create a CountrySelect field with the given name and label.
     *
     * @param name the name of the field
     * @param label the label of the field
     */
    public CountrySelect(String name, String label) {
        super(name, label);
    }

    /**
     * Create a CountrySelect field with the given name and required status.
     *
     * @param name the name of the field
     * @param required the field required status
     */
    public CountrySelect(String name, boolean required) {
        super(name, required);
    }

    /**
     * Create a CountrySelect field with the given name, label and required status.
     *
     * @param name the name of the field
     * @param label the label of the field
     * @param required the field required status
     */
    public CountrySelect(String name, String label, boolean required) {
        super(name, label, required);
    }

    /**
     * Create a CountrySelect field with the given name, label and locale.
     *
     * @param name the name of the field
     * @param label the label of the field
     * @param locale the Locale of the filed
     */
    public CountrySelect(String name, String label, Locale locale) {
        super(name, label);
        this.locale = locale;
    }

    /**
     * Create a CountrySelect field with no name defined.
     * <p/>
     * <b>Please note</b> the control's name must be defined before it is valid.
     */
    public CountrySelect() {
        super();
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Return the locale for this control.
     *
     * @return the locale of this control.
     */
    public Locale getLocale() {
        if (locale != null) {
            return locale;

        } else {
            return getContext().getLocale();
        }
    }

    /**
     * Set the locale of this control to something else than the
     * <code>Context</code> locale.
     *
     * @see net.sf.click.Context#getLocale()
     *
     * @param locale the locale to set for this control
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }
 
    // --------------------------------------------------------- Public Methods

    /**
     * Bind the request value to the control.
     *
     * @see Select#bindRequestValue()
     */
    public void bindRequestValue() {

        loadOptionList();

        super.bindRequestValue();
    }

    /**
     * Render the HTML representation of the QuerySelect.
     * <p/>
     * If the Select option list is empty this method will load option list so
     * that it can be rendered.
     *
     * @see #toString()
     *
     * @param buffer the specified buffer to render the control's output to
     */
    public void render(HtmlStringBuffer buffer) {
        loadOptionList();
        super.render(buffer);
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Load the Country Select options if not defined, using all the available
     * countries. The option value will be the two letter uppercase ISO name of
     * the country as the value and the localized country name as the label.
     */
    protected void loadOptionList() {
        // Determine whether option list should be loaded
        if (getOptionList().size() == 1) {
            Option option = (Option) getOptionList().get(0);
            if (option.getValue().equals(Option.EMPTY_OPTION.getValue())) {
                // continue and load option list

            } else {
                // Don't load list
                return;
            }

        } else if (getOptionList().size() > 1) {
            // Don't load list
            return;
        }

        List optionList = new ArrayList();

        Locale[] availableLocales = Locale.getAvailableLocales();

        for (int i = 0; i < availableLocales.length; i++) {
            final String iso = availableLocales[i].getCountry();
            final String name = availableLocales[i].getDisplayCountry(getLocale());

            if (StringUtils.isNotEmpty(iso) && StringUtils.isNotEmpty(name)) {
                optionList.add(new Option(iso, name));
            }
        }

        Collections.sort(optionList, new OptionLabelComparator(getLocale()));

        if (isRequired() && getOptionList().isEmpty()) {
            add(Option.EMPTY_OPTION);
        }

        addAll(optionList);
    }

    // ---------------------------------------------------------- Inner Classes

    /**
     * Provides a comparator for Option labels with locale-sensitive behaviour.
     */
    static class OptionLabelComparator implements Comparator {

        /** The locale comparator. */
        private final Comparator comparator;

        /**
         * Creates a new OptionLabelComparator object.
         *
         * @param locale The Locale used for localized String comparison.
         */
        public OptionLabelComparator(Locale locale) {
            comparator = Collator.getInstance(locale);
        }

        /**
         * Compares the localized labels of two Options.
         *
         * @see Comparator#compare(Object, Object)
         *
         * @param o1 The first Option to compare.
         * @param o2 The second Option to compare.
         * @return The value returned by comparing the localized labels.
         */
        public final int compare(Object o1, Object o2) {
            Option lhs = (Option) o1;
            Option rhs = (Option) o2;

            return comparator.compare(lhs.getLabel(), rhs.getLabel());
        }
    }
}
