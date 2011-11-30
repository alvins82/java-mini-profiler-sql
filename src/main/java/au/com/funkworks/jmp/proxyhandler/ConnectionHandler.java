/**
 * Copyright (C) 2011 by Gokul Krishnan, Alvin Singh
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package au.com.funkworks.jmp.proxyhandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConnectionHandler implements InvocationHandler {

	private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

	private final Connection connection;

	public ConnectionHandler(Connection connection) {
		this.connection = connection;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws SQLException {
		Object o;
		try {
			o = method.invoke(connection, args);
		} catch (IllegalArgumentException e) {
			throw new SQLException(e.getCause());
		} catch (IllegalAccessException e) {
			throw new SQLException(e.getCause());
		} catch (InvocationTargetException e) {
			throw new SQLException(e.getCause());
		}

		String methodName = method.getName();

		log.debug(String.format("Executed method in ConnectionHandler : %s", methodName));
		if (isStatement(methodName)) {
			return checkAndCreateStatement(o);
		} else if (isPreparedStatement(methodName)) {
			return checkAndCreatePreparedStatement(args, o);
		} else if (isCallableStatement(methodName)) {
			return checkAndCreateCallableStatement(args, o);
		}
		return o;
	}

	private Object checkAndCreateCallableStatement(Object[] args, Object o) {
		CallableStatement callableStmt = (CallableStatement) o;
		PreparedStatementHandler pStmtHandler = new PreparedStatementHandler(callableStmt, (String) args[0]);
		CallableStatement proxyStmt = (CallableStatement) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { CallableStatement.class }, pStmtHandler);
		log.debug("Created new proxy callable statement");
		return proxyStmt;
	}

	private Object checkAndCreatePreparedStatement(Object[] args, Object o) {
		PreparedStatement pStmt = (PreparedStatement) o;
		PreparedStatementHandler pStmtHandler = new PreparedStatementHandler(pStmt, (String) args[0]);
		PreparedStatement proxyStmt = (PreparedStatement) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { PreparedStatement.class }, pStmtHandler);
		log.debug("Created new proxy prepared statement");
		return proxyStmt;
	}

	private Object checkAndCreateStatement(Object o) {
		Statement stmt = (Statement) o;
		StatementHandler stmtHandler = new StatementHandler(stmt);
		Statement proxyStmt = (Statement) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { Statement.class }, stmtHandler);
		log.debug("Created new proxy statement");
		return proxyStmt;
	}

	private boolean isCallableStatement(String methodName) {
		return methodName.startsWith("prepareCall");
	}

	private boolean isPreparedStatement(String methodName) {
		return methodName.startsWith("prepareStatement");
	}

	private boolean isStatement(String methodName) {
		return methodName.startsWith("createStatement");
	}
}
