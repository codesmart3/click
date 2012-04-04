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
package org.apache.click.fileupload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.fileupload.FileItem;

/**
 * Mock implementation of <tt>org.apache.commons.fileupload.FileItem</tt>.
 */
public class MockFileItem implements FileItem {

	private static final long serialVersionUID = 1L;

	@Override
	public void delete() {
	}

	@Override
	public byte[] get() {
		return null;
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public String getFieldName() {
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}

	@Override
	public long getSize() {
		return 0;
	}

	@Override
	public String getString() {
		return null;
	}

	@Override
	public String getString(String arg0) throws UnsupportedEncodingException {
		return null;
	}

	@Override
	public boolean isFormField() {
		return false;
	}

	@Override
	public boolean isInMemory() {
		return false;
	}

	@Override
	public void setFieldName(String arg0) {
	}

	@Override
	public void setFormField(boolean arg0) {
	}

	@Override
	public void write(File arg0) throws Exception {
	}

}
