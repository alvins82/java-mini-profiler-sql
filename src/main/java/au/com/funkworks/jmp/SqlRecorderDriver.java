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

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class SqlRecorderDriver implements Driver {

	private static SqlRecorder recorder;

	static {
		try {
			init();
		} catch (SQLException e) {
			throw new SqlRecorderException("Unable to register SqlRecorder driver", e);
		}
	}

	private static void init() throws SQLException {
		String driverClassName = null;
		List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
		for (String inputArg : inputArgs) {
			if (inputArg.startsWith("-DdriverClassName")) {
				int index = inputArg.indexOf('=');
				driverClassName = inputArg.substring(index+1);
				break;
			}
		}		
		if (driverClassName == null) {
			throw new SQLException("No driverClassName found in java vm args. Please specify one with -DdriverClassName=xyz");
		}
		recorder = new SqlRecorder(driverClassName);
	}

	public Connection connect(String url, Properties info) throws SQLException {
		return recorder.connect(url, info);
	}

	public boolean acceptsURL(String url) throws SQLException {
		return recorder.acceptsURL(url);
	}

	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return recorder.getPropertyInfo(url, info);
	}

	public int getMajorVersion() {
		return recorder.getMajorVersion();
	}

	public int getMinorVersion() {
		return recorder.getMinorVersion();
	}

	public boolean jdbcCompliant() {
		return recorder.jdbcCompliant();
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

}
