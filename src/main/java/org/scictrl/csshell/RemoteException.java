package org.scictrl.csshell;



/**
 *
 * A remote exception.
 *
 * @author igor@scictrl.com
 */
public class RemoteException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Remote instance.
	 */
	private Object instance;

	/* (non-Javadoc)
	 * @see CommonException#CommonException()
	 */
	/**
	 * <p>Constructor for RemoteException.</p>
	 */
	public RemoteException()
	{
		super();
	}

	/* (non-Javadoc)
	 * @see CommonException#CommonException(java.lang.Object, java.lang.String)
	 */
	/**
	 * <p>Constructor for RemoteException.</p>
	 *
	 * @param instance a {@link java.lang.Object} object
	 * @param s a {@link java.lang.String} object
	 */
	public RemoteException(Object instance, String s)
	{
		this(instance, s, null);
	}

	/* (non-Javadoc)
	 * @see CommonException#CommonException(java.lang.Object, java.lang.String, java.lang.Throwable)
	 */
	/**
	 * <p>Constructor for RemoteException.</p>
	 *
	 * @param instance a {@link java.lang.Object} object
	 * @param message a {@link java.lang.String} object
	 * @param t a {@link java.lang.Throwable} object
	 */
	public RemoteException(Object instance, String message, Throwable t)
	{
		super(message,t);
		this.instance=instance;
	}
	
	/**
	 * <p>Getter for the field <code>instance</code>.</p>
	 *
	 * @return a {@link java.lang.Object} object
	 */
	public Object getInstance() {
		return instance;
	}
}

/* __oOo__ */
