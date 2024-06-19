package org.scictrl.csshell;


/**
 * <p>Device interface.</p>
 *
 * @author igor@scictrl.com
 */
public interface Device {

	/**
	 * <p>getName.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getName();

	/**
	 * <p>getType.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getType();

	/**
	 * <p>getChannelNames.</p>
	 *
	 * @return an array of {@link java.lang.String} objects
	 */
	public String[] getChannelNames();
	
	/**
	 * <p>getChannel.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.Connection} object
	 */
	public Connection<AbstractConnector<?>, ?, ?> getChannel(String name);

	/**
	 * <p>getChannels.</p>
	 *
	 * @return an array of {@link org.scictrl.csshell.Connection} objects
	 */
	public Connection<AbstractConnector<?>, ?, ?>[] getChannels();

}
