package org.ron.servlet;

import java.sql.SQLDataException;

public class NoResultException
extends SQLDataException
{
	private static final long serialVersionUID = -5323407982815518740L;

	public NoResultException(String message)
	{
		super(message);
	}
}
