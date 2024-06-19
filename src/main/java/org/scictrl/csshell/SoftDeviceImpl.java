package org.scictrl.csshell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Device interface, which allows building custom device from channels.
 *
 * @author igor@scictrl.com
 */
public class SoftDeviceImpl implements Device {
	
	

	private String name;
	private String type;
	private List<String> channelNames = new ArrayList<String>(3);
	private Map<String,Connection<AbstractConnector<?>, ?, ?>> channels =  new HashMap<String, Connection<AbstractConnector<?>,?, ?>>(3);

	/**
	 * <p>Constructor for SoftDeviceImpl.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link java.lang.String} object
	 */
	public SoftDeviceImpl(String name, String type) {
		this.name=name;
		this.type=type;
	}
	
	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

	/** {@inheritDoc} */
	@Override
	public String getType() {
		return type;
	}

	/** {@inheritDoc} */
	@Override
	public String[] getChannelNames() {
		return channelNames.toArray(new String[channelNames.size()]);
	}

	/** {@inheritDoc} */
	@Override
	public Connection<AbstractConnector<?>, ?, ?> getChannel(String name) {
		return channels.get(name);
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public Connection<AbstractConnector<?>, ?, ?>[] getChannels() {
		return channels.values().toArray(new Connection[channels.size()]);
	}
	
	/**
	 * <p>addChannel.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param chan a {@link org.scictrl.csshell.Connection} object
	 */
	public void addChannel(String name, Connection<AbstractConnector<?>, ?, ?> chan) {
		
		if (!channels.containsKey(name)) {
			channelNames.add(name);
			Collections.sort(channelNames);
		}
		
		channels.put(name, chan);
		
	}

}
