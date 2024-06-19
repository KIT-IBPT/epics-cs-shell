package org.scictrl.csshell;

/**
 * Default response implementation
 *
 * @author igor@scictrl.com
 */
public class ResponseImpl<C extends AbstractConnector<?>> implements Response<C>
{
	private Request<C> request;
	private Object tag;
	private boolean success;
	private Exception error;
	private boolean last;
	private Connection<C,?,?> source;
	private Poop<?,?> poop;

	/**
	 * Creates a new ResponseImpl object.
	 *
	 * @param source response source
	 * @param r the request this response is the response of
	 * @param success <code>true</code> if response is a success
	 * @param error response error
	 * @param last <code>true</code> if this is the last response.
	 * @param tag a {@link java.lang.Object} object
	 * @param poop a {@link org.scictrl.csshell.Poop} object
	 */
	public ResponseImpl(Connection<C,?,?> source, Request<C> r,
	    Object tag, boolean success, Exception error,
	    Poop<?,?> poop, boolean last)
	{
		this.source = source;
		this.request = r;
		this.tag = tag;
		this.poop = poop;
		this.success = success;
		this.error = error;
		this.last = last;

	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.Response#getError()
	 */
	/** {@inheritDoc} */
	@Override
	public Exception getError()
	{
		return error;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.Response#getRequest()
	 */
	/** {@inheritDoc} */
	@Override
	public Request<C> getRequest()
	{
		return request;
	}

	/*
	 *  (non-Javadoc)
	 * @see org.epics.css.dal.Response#getIdTag()
	 */
	/** {@inheritDoc} */
	@Override
	public Object getTag()
	{
		return tag;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.Response#isLast()
	 */
	/** {@inheritDoc} */
	@Override
	public boolean isLast()
	{
		return last;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.Response#getSource()
	 */
	/** {@inheritDoc} */
	@Override
	public Connection<C,?,?> getConnection()
	{
		return source;
	}
	
	/** {@inheritDoc} */
	@Override
	public Poop<?,?> getPoop() {
		return poop;
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isSuccess() {
		return success;
	};

	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder(256);
		sb.append("Response{");
		sb.append(source.getName());
		sb.append(',');
		sb.append(tag);
		sb.append('}');
		
		return sb.toString();
	}
}

/* __oOo__ */
