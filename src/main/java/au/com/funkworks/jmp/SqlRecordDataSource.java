/**
 * Copyright (C) 2011 by Alvin Singh
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
package au.com.funkworks.jmp;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.funkworks.jmp.proxyhandler.ConnectionHandler;

public class SqlRecordDataSource implements DataSource {

	private DataSource realDataSource;

	private static final Logger log = LoggerFactory.getLogger(SqlRecordDataSource.class);

	public SqlRecordDataSource(DataSource realDataSource) {
		this.realDataSource = realDataSource;
	}

	public Connection getConnection() throws SQLException {
		Connection connection = realDataSource.getConnection();
		ConnectionHandler connHandler = new ConnectionHandler(connection);
		Connection proxyConnection = (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { Connection.class }, connHandler);
		log.debug(String.format("Created a new proxy connection for native connection : %s", connection.toString()));
		return proxyConnection;
	}

	public Connection getConnection(String username, String password) throws SQLException {
		final Connection connection = realDataSource.getConnection(username, password);
		ConnectionHandler connHandler = new ConnectionHandler(connection);
		Connection proxyConnection = (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { Connection.class }, connHandler);
		log.debug(String.format("Created a new proxy connection for native connection : %s", connection.toString()));
		return proxyConnection;
	}

	public int getLoginTimeout() throws SQLException {
		return realDataSource.getLoginTimeout();
	}

	public PrintWriter getLogWriter() throws SQLException {
		return realDataSource.getLogWriter();
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return realDataSource.isWrapperFor(iface);
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		realDataSource.setLoginTimeout(seconds);
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		realDataSource.setLogWriter(out);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return realDataSource.unwrap(iface);
	}

}
