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
package net.sf.click.examples.page.introduction;

import java.util.List;

import net.sf.click.Page;
import net.sf.click.control.Column;
import net.sf.click.control.Table;
import net.sf.click.examples.page.BorderPage;

/**
 * Provides an simple Table usage example Page.
 *
 * @author Malcolm Edgar
 */
public class SimpleTablePage extends BorderPage {

    public Table table = new Table();

    // ------------------------------------------------------------ Constructor

    public SimpleTablePage() {
        table.setClass(Table.CLASS_ITS);

        table.addColumn(new Column("id"));
        table.addColumn(new Column("name"));
        table.addColumn(new Column("email"));
        table.addColumn(new Column("investments"));
    }

    // --------------------------------------------------------- Event Handlers

    /**
     * @see Page#onRender()
     */
    public void onRender() {
        List list = getCustomerService().getCustomersSortedByName(10);
        table.setRowList(list);
    }
}
