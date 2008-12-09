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
package net.sf.click.examples.page.tree;

import net.sf.click.Page;
import net.sf.click.control.PageLink;
import net.sf.click.examples.page.introduction.AdvancedForm;
import net.sf.click.examples.page.introduction.AdvancedTable;
import net.sf.click.examples.page.introduction.ControlListenerPage;
import net.sf.click.examples.page.introduction.HelloWorld;
import net.sf.click.examples.page.introduction.SimpleForm;
import net.sf.click.examples.page.velocity.SimpleTable;
import net.sf.click.extras.tree.Tree;
import net.sf.click.extras.tree.TreeNode;
import net.sf.click.util.HtmlStringBuffer;
import org.springframework.util.ClassUtils;

/**
 * Demonstrates how to customize the rendering of tree nodes.
 * <p/>
 * In this example tree nodes render links to Pages.
 *
 * @author Bob Schellink
 */
public class PageLinkTreePage extends PlainTreePage {

    public static final String TREE_NODES_SESSION_KEY = "pageLinkTreeNodes";

    // --------------------------------------------------------- Protected Methods

    /**
     * Creates and return a new tree instance.
     */
    protected Tree createTree() {
        return new Tree("tree") {

            protected void renderValue(HtmlStringBuffer buffer, TreeNode treeNode) {
                Object nodeValue = treeNode.getValue();
                Class cls = null;
                if(nodeValue instanceof Class) {
                    cls = (Class) nodeValue;
                }

                // If node value is a Page class, render a PageLink, otherwise
                // render the node value
                if (cls != null && Page.class.isAssignableFrom(cls)) {
                    String shortName = ClassUtils.getShortName(cls);
                    PageLink link = new PageLink(shortName, cls);
                    buffer.append(link);
                } else {
                    buffer.append(nodeValue);
                }
            }
        };
    }

    /**
     * Build the tree model and stores it in the session. This model represents
     * nodes which link to other example Pages.
     */
    protected TreeNode createNodes() {

        //Create a node representing the root directory with the specified
        //parameter as the value. Because an id is not specified, a random
        //one will be generated by the node. By default the root node is
        //not rendered by the tree. This can be changed by calling
        //tree.setRootNodeDisplayed(true).
        TreeNode root = new TreeNode("Pages");

        //Create a new directory, setting the root directory as its parent. Here
        //we do specify a id as the 2nd argument, so no id is generated.
        TreeNode general = new TreeNode("Intro", "1", root);

        boolean supportsChildNodes = false;
        
        new TreeNode(HelloWorld.class, "1.1", general, supportsChildNodes);
        new TreeNode(ControlListenerPage.class, "1.2", general, supportsChildNodes);

        TreeNode forms = new TreeNode("Forms", "2", root);
        new TreeNode(SimpleForm.class, "2.1", forms, supportsChildNodes);
        new TreeNode(AdvancedForm.class, "2.2", forms, supportsChildNodes);
        
        TreeNode tables = new TreeNode("Tables", "3", root);
        new TreeNode(SimpleTable.class, "3.1", tables, supportsChildNodes);
        new TreeNode(AdvancedTable.class, "3.2", tables, supportsChildNodes);

        return root;
    }

    /**
     * Return the string under which the nodes are stored in the session.
     * 
     * @return the string under which the nodes are stored in the session
     */
    protected String getSessionKey() {
        return TREE_NODES_SESSION_KEY;
    }
}
