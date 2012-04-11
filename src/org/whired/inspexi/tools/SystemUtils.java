/*
 * Copyright 2002-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.whired.inspexi.tools;

/**
 * <p>
 * Helpers for <code>java.lang.System</code>.
 * </p>
 * <p>
 * If a system property cannot be read due to security restrictions, the corresponding field in this class will be set to <code>null</code> and a message will be written to <code>System.err</code>.
 * </p>
 * 
 * @author Based on code from Avalon Excalibur
 * @author Based on code from Lucene
 * @author Stephen Colebourne
 * @author <a href="mailto:sdowney@panix.com">Steve Downey</a>
 * @author Gary Gregory
 * @author Michael Becke
 * @author Tetsuya Kaneuchi
 * @author Rafal Krupinski
 * @since 1.0
 * @version $Id: SystemUtils.java,v 1.33 2004/02/25 00:25:29 ggregory Exp $
 */
public class SystemUtils {

	// System property constants
	// -----------------------------------------------------------------------
	// These MUST be declared first. Other constants depend on this.
	public static final String OS_NAME = getSystemProperty("os.name");

	/**
	 * <p>
	 * The <code>user.country</code> or <code>user.region</code> System Property. User's country code, such as <code>GB</code>. First in JDK version 1.2 as <code>user.region</code>. Renamed to <code>user.country</code> in 1.4
	 * </p>
	 * <p>
	 * Defaults to <code>null</code> if the runtime does not have security access to read this property or the property does not exist.
	 * </p>
	 * <p>
	 * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of sync with that System property.
	 * </p>
	 * 
	 * @since 2.0
	 * @since Java 1.2
	 */
	public static final String USER_COUNTRY = (getSystemProperty("user.country") == null ? getSystemProperty("user.region") : getSystemProperty("user.country"));

	// Operating system checks
	// -----------------------------------------------------------------------
	// These MUST be declared after those above as they depend on the
	// values being set up
	// OS names from http://www.vamphq.com/os.html
	// Selected ones included - please advise commons-dev@jakarta.apache.org
	// if you want another added or a mistake corrected

	/**
	 * <p>
	 * Is <code>true</code> if this is AIX.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public static final boolean IS_OS_AIX = getOSMatches("AIX");

	/**
	 * <p>
	 * Is <code>true</code> if this is HP-UX.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public static final boolean IS_OS_HP_UX = getOSMatches("HP-UX");

	/**
	 * <p>
	 * Is <code>true</code> if this is Irix.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public static final boolean IS_OS_IRIX = getOSMatches("Irix");

	/**
	 * <p>
	 * Is <code>true</code> if this is Linux.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public static final boolean IS_OS_LINUX = getOSMatches("Linux") || getOSMatches("LINUX");

	/**
	 * <p>
	 * Is <code>true</code> if this is Mac.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public static final boolean IS_OS_MAC = getOSMatches("Mac");

	/**
	 * <p>
	 * Is <code>true</code> if this is Mac.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public static final boolean IS_OS_MAC_OSX = getOSMatches("Mac OS X");

	/**
	 * <p>
	 * Is <code>true</code> if this is OS/2.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public static final boolean IS_OS_OS2 = getOSMatches("OS/2");

	/**
	 * <p>
	 * Is <code>true</code> if this is Solaris.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public static final boolean IS_OS_SOLARIS = getOSMatches("Solaris");

	/**
	 * <p>
	 * Is <code>true</code> if this is SunOS.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public static final boolean IS_OS_SUN_OS = getOSMatches("SunOS");

	/**
	 * <p>
	 * Is <code>true</code> if this is a POSIX compilant system, as in any of AIX, HP-UX, Irix, Linux, MacOSX, Solaris or SUN OS.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.1
	 */
	public static final boolean IS_OS_UNIX = IS_OS_AIX || IS_OS_HP_UX || IS_OS_IRIX || IS_OS_LINUX || IS_OS_MAC_OSX || IS_OS_SOLARIS || IS_OS_SUN_OS;

	/**
	 * <p>
	 * Is <code>true</code> if this is Windows.
	 * </p>
	 * <p>
	 * The field will return <code>false</code> if <code>OS_NAME</code> is <code>null</code>.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public static final boolean IS_OS_WINDOWS = getOSMatches("Windows");

	/**
	 * <p>
	 * SystemUtils instances should NOT be constructed in standard programming. Instead, the class should be used as <code>SystemUtils.FILE_SEPARATOR</code>.
	 * </p>
	 * <p>
	 * This constructor is public to permit tools that require a JavaBean instance to operate.
	 * </p>
	 */
	public SystemUtils() {
		// no init.
	}

	/**
	 * <p>
	 * Decides if the operating system matches.
	 * </p>
	 * 
	 * @param osNamePrefix the prefix for the os name
	 * @return true if matches, or false if not or can't determine
	 */
	private static boolean getOSMatches(String osNamePrefix) {
		if (OS_NAME == null) {
			return false;
		}
		return OS_NAME.toLowerCase().startsWith(osNamePrefix.toLowerCase());
	}

	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Gets a System property, defaulting to <code>null</code> if the property cannot be read.
	 * </p>
	 * <p>
	 * If a <code>SecurityException</code> is caught, the return value is <code>null</code> and a message is written to <code>System.err</code>.
	 * </p>
	 * 
	 * @param property the system property name
	 * @return the system property value or <code>null</code> if a security problem occurs
	 */
	private static String getSystemProperty(String property) {
		try {
			return System.getProperty(property);
		}
		catch (SecurityException ex) {
			// we are not allowed to look at this property
			System.err.println("Caught a SecurityException reading the system property '" + property + "'; the SystemUtils property value will default to null.");
			return null;
		}
	}
}
