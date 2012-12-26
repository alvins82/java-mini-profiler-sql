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
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.funkworks.jmp.MiniProfiler;
import au.com.funkworks.jmp.MiniProfiler.Step;

public final class StatementHandler implements InvocationHandler {

	private static final Logger log = LoggerFactory.getLogger(PreparedStatementHandler.class);

	private final Statement statement;

	public StatementHandler(Statement statement) {
		this.statement = statement;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws SQLException {
		String methodName = method.getName();

		log.trace(String.format("Executed method : %s", methodName));
		Step step = null;

		try {
			if (methodName.startsWith("execute") || methodName.equals("addBatch")) {
				step = MiniProfiler.step((String) args[0]);
			}
			return method.invoke(statement, args);
		} catch (IllegalArgumentException e) {
			throw new SQLException(e.getCause());
		} catch (IllegalAccessException e) {
			throw new SQLException(e.getCause());
		} catch (InvocationTargetException e) {
			throw new SQLException(e.getCause());
		} finally {
			if (step != null) {
				step.close();
			}
		}
	}

}
