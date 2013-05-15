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
package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.FooService;

import java.util.Properties;

public class FooProviderTypeDyn2 implements FooService {
	
	private int intProp = 2;
	private boolean boolProp = true;
	private String strProp = "foo";
	private String[] strAProp = new String[] {"foo", "bar"};
	private int[] intAProp = new int[] {1, 2, 3};

	public boolean foo() {
		intAProp = null;
		return true;
	}

	public Properties fooProps() {
		Properties p = new Properties();
		p.put("intProp", new Integer(intProp));
		p.put("boolProp", new Boolean(boolProp));
		p.put("strProp", strProp);
		p.put("strAProp", strAProp);
		p.put("intAProp", intAProp);
		return p;
	}
	
	public boolean getBoolean() { return true; }

	public double getDouble() { return 1.0; }

	public int getInt() { return 1; }

	public long getLong() { return 1; }

	public Boolean getObject() { return new Boolean(true); }

}
