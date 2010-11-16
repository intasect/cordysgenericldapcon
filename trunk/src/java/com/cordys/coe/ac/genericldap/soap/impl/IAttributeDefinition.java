/*
 * Copyright 2007 Cordys R&D B.V. 
 *
 *   This file is part of the Cordys Generic LDAP Connector. 
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.cordys.coe.ac.genericldap.soap.impl;

public interface IAttributeDefinition
{

	/**
	 * This method gets the name of the attribute.
	 *
	 * @return  The name of the attribute.
	 */
	public abstract String getName();

	/**
	 * This method gets the type for the parameter.
	 *
	 * @return  The type for the parameter.
	 */
	public abstract EAttributeType getType();

	/**
	 * This method sets the name of the attribute.
	 *
	 * @param  name  The name of the attribute.
	 */
	public abstract void setName(String name);

	/**
	 * This method sets the type for the parameter.
	 *
	 * @param  type  The type for the parameter.
	 */
	public abstract void setType(EAttributeType type);

}