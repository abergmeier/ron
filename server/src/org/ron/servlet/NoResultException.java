package org.ron.servlet;

import java.sql.SQLDataException;

public class NoResultException
extends SQLDataException
{
	public NoResultException(String message)
	{
		super(message);
	}
}
