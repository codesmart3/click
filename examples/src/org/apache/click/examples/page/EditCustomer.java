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
package org.apache.click.examples.page;

import javax.annotation.Resource;

import org.apache.click.Page;
import org.apache.click.control.Checkbox;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.examples.control.InvestmentSelect;
import org.apache.click.examples.domain.Customer;
import org.apache.click.examples.service.CustomerService;
import org.apache.click.extras.control.DateField;
import org.apache.click.extras.control.DoubleField;
import org.apache.click.extras.control.EmailField;
import org.apache.click.extras.control.IntegerField;
import org.apache.click.util.Bindable;
import org.springframework.stereotype.Component;

/**
 * Provides an edit Customer Form example. The Customer business object
 * is initially passed to this Page as a request attribute.
 * <p/>
 * Note the public visibility "referrer" HiddenField and the "id" field
 * have their value automatically set with any identically named request
 * parameters after the page is created.
 *
 * @author Malcolm Edgar
 */
@Component
public class EditCustomer extends BorderPage {

    // Public controls are automatically added to the page
    @Bindable protected Form form = new Form("form");
    @Bindable protected HiddenField referrerField = new HiddenField("referrer", String.class);

    // Public variables can automatically have their value set by request parameters
    @Bindable protected Integer id;

    private HiddenField idField = new HiddenField("id", Integer.class);

    @Resource(name="customerService")
    private CustomerService customerService;

    public EditCustomer() {
        form.add(referrerField);

        form.add(idField);

        FieldSet fieldSet = new FieldSet("customer");
        form.add(fieldSet);

        TextField nameField = new TextField("name", true);
        nameField.setMinLength(5);
        nameField.setFocus(true);
        fieldSet.add(nameField);

        fieldSet.add(new EmailField("email"));

        IntegerField ageField = new IntegerField("age");
        ageField.setMinValue(1);
        ageField.setMaxValue(120);
        ageField.setWidth("40px");
        fieldSet.add(ageField);

        DoubleField holdingsField = new DoubleField("holdings");
        holdingsField.setTextAlign("right");
        fieldSet.add(holdingsField);

        fieldSet.add(new InvestmentSelect("investments"));
        fieldSet.add(new DateField("dateJoined"));
        fieldSet.add(new Checkbox("active"));

        form.add(new Submit("ok", "  OK  ", this, "onOkClick"));
        form.add(new Submit("cancel", this, "onCancelClick"));
    }

    /**
     * When page is first displayed on the GET request.
     *
     * @see Page#onGet()
     */
    @Override
    public void onGet() {
        if (id != null) {
            Customer customer = customerService.getCustomerForID(id);

            if (customer != null) {
                form.copyFrom(customer);
            }
        }
    }

    public boolean onOkClick() {
        if (form.isValid()) {
            Integer id = (Integer) idField.getValueObject();
            Customer customer = customerService.getCustomerForID(id);

            if (customer == null) {
                customer = new Customer();
            }
            form.copyTo(customer);

            customerService.saveCustomer(customer);

            String referrer = referrerField.getValue();
            if (referrer != null) {
                setRedirect(referrer);
            } else {
                setRedirect(HomePage.class);
            }

            return true;

        } else {
            return true;
        }
    }

    public boolean onCancelClick() {
        String referrer = referrerField.getValue();
        if (referrer != null) {
            setRedirect(referrer);
        } else {
            setRedirect(HomePage.class);
        }
        return true;
    }

}
