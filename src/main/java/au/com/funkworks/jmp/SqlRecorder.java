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
package au.com.funkworks.jmp;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.funkworks.jmp.proxyhandler.ConnectionHandler;

final class SqlRecorder implements Driver {

	private static final Logger log = LoggerFactory.getLogger(SqlRecorder.class);

	private final String driverClass;

	private Driver driver;

	public SqlRecorder(String driverClass) {
		if (driverClass == null || driverClass.equals("")) {
			throw new SqlRecorderException("driver class cannot be empty");
		}

		log.info(String.format("Starting to register drivers: %s", driverClass));
		this.driverClass = driverClass;
		proxyDriver();
		log.info("Finished startup..");
	}

	public Connection connect(String s, Properties properties) throws SQLException {
		Connection connection = driver.connect(s, properties);
		ConnectionHandler connHandler = new ConnectionHandler(connection);
		Connection proxyConnection = (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { Connection.class }, connHandler);
		log.debug(String.format("Created a new proxy connection for native connection : %s", connection.toString()));
		return proxyConnection;
	}

	public boolean acceptsURL(String s) throws SQLException {
		return driver.acceptsURL(s);
	}

	public DriverPropertyInfo[] getPropertyInfo(String s, Properties properties) throws SQLException {
		return driver.getPropertyInfo(s, properties);
	}

	public int getMajorVersion() {
		return driver.getMajorVersion();
	}

	public int getMinorVersion() {
		return driver.getMinorVersion();
	}

	public boolean jdbcCompliant() {
		return driver.jdbcCompliant();
	}

	private synchronized void proxyDriver() {
		try {

			// Load the driver we want to proxy
			Class.forName(driverClass);

			log.info("Starting to identify currently registered jdbc drivers..");
			Enumeration<Driver> allRegDrivers = DriverManager.getDrivers();

			// Now enumerate thro the registered drivers and deregister all the
			// drivers so that SqlRecorder becomes the first driver in queue.
			// Add the other drivers to the end of the queue
			log.info(String.format("Processing user input driver: %s", driverClass));
			Driver driver = null;
			List<Driver> unregisteredDrivers = new ArrayList<Driver>();
			while (allRegDrivers.hasMoreElements()) {
				driver = allRegDrivers.nextElement();
				log.info(String.format("Found registered jdbc driver: %s ", driver.getClass().getName()));
				if (driver.getClass().getName().equals(driverClass)) {
					this.driver = driver;
					log.info(String.format("Matched existing driver: registered driver: %s, user input: %s. Deregistering driver", driver.getClass().getName(), driverClass));
				}
				DriverManager.deregisterDriver(driver);
				unregisteredDrivers.add(driver);// Keep track of unreg drivers
			}

			// This case would occur if the driver has been deregistered and not
			// registered again till this point.
			if (this.driver == null) {
				this.driver = (Driver) Class.forName(driverClass).newInstance();
			}
			DriverManager.registerDriver(this);

			// Now reregister the unregistered drivers. They will come after
			// this proxy driver.
			// The driver we are proxying will not be registered
			for (Driver unregisteredDriver : unregisteredDrivers) {
				if (!unregisteredDriver.getClass().getName().equals(driverClass)) {
					DriverManager.registerDriver(unregisteredDriver);
				}
			}
		} catch (Exception e) {
			throw new SqlRecorderException("Unexpected error when registering drivers", e);
		}
	}
}
