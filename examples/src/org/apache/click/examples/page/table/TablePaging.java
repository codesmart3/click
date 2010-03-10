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
package org.apache.click.examples.page.table;

import java.util.List;

import javax.annotation.Resource;

import org.apache.click.control.Column;
import org.apache.click.control.Table;
import org.apache.click.examples.domain.Customer;
import org.apache.click.examples.page.BorderPage;
import org.apache.click.examples.service.CustomerService;
import org.apache.click.util.Bindable;
import org.apache.click.util.DataProvider;
import org.springframework.stereotype.Component;

/**
 * Provides an demonstration of Table control paging.
 */
@Component
public class TablePaging extends BorderPage {

    private static final long serialVersionUID = 1L;

    @Bindable protected Table table = new Table();

    @Resource(name="customerService")
    private CustomerService customerService;

    public TablePaging() {
        // Setup customers table
        table.setClass(Table.CLASS_ISI);
        table.setPageSize(4);
        table.setShowBanner(true);
        table.setSortable(true);

        Column column = new Column("name");
        column.setWidth("140px;");
        table.addColumn(column);

        column = new Column("email");
        column.setAutolink(true);
        column.setWidth("230px;");
        table.addColumn(column);

        column = new Column("age");
        column.setTextAlign("center");
        column.setWidth("40px;");
        table.addColumn(column);

        column = new Column("holdings");
        column.setFormat("${0,number,#,##0.00}");
        column.setTextAlign("right");
        column.setWidth("100px;");
        table.addColumn(column);

        table.setDataProvider(new DataProvider<Customer>() {
            public List<Customer> getData() {
                return customerService.getCustomers();
            }
        });
    }

}
