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
package org.apache.click.extras.security;

import javax.servlet.http.HttpServletRequest;

/**
 * Provides a Role based access controller class. This access controller uses the
 * JEE servlet container to determine whether an authenticated user has access
 * to a specified role.
 *
 * @author Malcolm Edgar
 */
public class RoleAccessController implements AccessController {

    /**
     * Return true if the user is in the specified security access role.
     *
     * @see AccessController#hasAccess(HttpServletRequest, String)
     *
     * @param request the user request
     * @param role the security access role to check
     * @return true if the user is in the specified role
     */
    public boolean hasAccess(HttpServletRequest request, String role) {
        return request.isUserInRole(role);
    }

}