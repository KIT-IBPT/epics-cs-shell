package org.scictrl.csshell;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Default implementation of request object. It conveniently stores
 * responses up to the capacity and notifies request listener about new
 * responses.
 *
 * @author igor@scictrl.com
 * 
 * @param <C> connector implementation
 */
public class RequestImpl<C extends AbstractConnector<?>> implements Request<C>
{
	/**
	 * Collected responses.
	 */
	protected LinkedList<Response<C>> responses;
	/**
	 * Source.
	 */
	protected Connection<C,?,?> source;
	/**
	 * Listener.
	 */
	protected ResponseListener<C> listener = null;
	/**
	 * Tag for events to be distinguished.
	 */
	protected Object tag;
	private int capacity = 1;
	
	
	/**
	 * Creates new instance. Default response capacity is 1.
	 *
	 * @param source the source of responses
	 * @param l response listener
	 * @see #getCapacity()
	 * @param tag a {@link java.lang.Object} object
	 */
	public RequestImpl(Connection<C,?,?> source, ResponseListener<C> l, Object tag)
	{
		this(source, l, tag, 1);
	}

	/**
	 * Creates new instance with defined capacity for responses.
	 *
	 * @param source the source of reponses
	 * @param l listener
	 * @param capacity number of last responses stored, if 0 all responses are stored.
	 * @see #getCapacity()
	 * @param tag a {@link java.lang.Object} object
	 */
	public RequestImpl(Connection<C,?,?> source, ResponseListener<C> l, Object tag, int capacity)
	{
		if (source == null) {
			throw new NullPointerException("source");
		}

		if (l == null) {
			throw new NullPointerException("l");
		}

		if (capacity < 0) {
			throw new IllegalArgumentException(
			    "Capacity must be larger than 0, not " + capacity + ".");
		}

		responses = new LinkedList<Response<C>>();
		this.source = source;
		listener = l;
		this.capacity = capacity;
		this.tag=tag;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.Request#getSource()
	 */
	/** {@inheritDoc} */
	@Override
	public Connection<C,?,?> getConnection()
	{
		return source;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.Request#hasResponse()
	 */
	/** {@inheritDoc} */
	@Override
	public synchronized boolean hasResponse()
	{
		return responses.size() > 0;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.Request#responses()
	 */
	/** {@inheritDoc} */
	@Override
	public Iterator<Response<C>> responses()
	{
		return responses.iterator();
	}
	
	/** {@inheritDoc} */
	@Override
	public Object getTag() {
		return tag;
	}

	/**
	 * Adds new response to this request object and dispatches it to
	 * listener.
	 *
	 * @param r new response to be dispatched
	 * @throws java.lang.NullPointerException if response is null
	 * @throws java.lang.IllegalArgumentException if source of response and source of this request is not equal
	 */
	public void addResponse(Response<C> r)
	{
		if (r == null) {
			throw new NullPointerException("r");
		}

		if (r.getConnection() != source) {
			throw new IllegalArgumentException(
			    "Can not dispatch response which has different source identifeable.");
		}

		synchronized (this) {
			responses.add(r);
			while (capacity > 0 && responses.size() > capacity) {
				responses.removeFirst();
			}
		}
		
		if (listener != null) {
			ResponseEvent<C> e = new ResponseEvent<C>(source, this, r);
			listener.responseReceived(e);
		}
		
		 
		if (r.isLast()) {
			synchronized (this) {
				this.notifyAll();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.Request#isCompleted()
	 */
	/** {@inheritDoc} */
	@Override
	public synchronized boolean isCompleted()
	{
		if (responses.size() > 0) {
			return responses.getLast().isLast();
		}

		return false;
	}

	/**
	 * Capacity number defines how many of last responses is stored in
	 * this request.  0 means that all are stored.
	 *
	 * @return Returns the capacity.
	 */
	public int getCapacity()
	{
		return capacity;
	}

	/**
	 * <p>getResponseListener.</p>
	 *
	 * @return a {@link org.scictrl.csshell.ResponseListener} object
	 */
	public ResponseListener<C> getResponseListener()
	{
		return listener;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.Request#getFirstResponse()
	 */
	/** {@inheritDoc} */
	@Override
	public synchronized Response<C> getFirstResponse() {
		return responses.getFirst();
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.Request#getLastResponse()
	 */
	/** {@inheritDoc} */
	@Override
	public synchronized Response<C> getLastResponse() {
		return responses.getLast();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	/** {@inheritDoc} */
	@Override
	public Iterator<Response<C>> iterator() {
		return responses();
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * Blocks call until last response is received. <br><b>NOTE: </b> call from this method is returned after events
	 * are dispatched on ResponseListeners.
	 */
	@Override
	public Response<C> waitUntilDone() {
		while (isCompleted() == false){
			synchronized(this)
			{
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		return getLastResponse();
	}	
}

/* __oOo__ */
