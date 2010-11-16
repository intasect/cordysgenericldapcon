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