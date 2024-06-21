package org.scictrl.csshell;

import java.util.Iterator;


/**
 * An interface which describes a request.  A request can have a number of responses. Response
 * objects implement the <code>Response</code> interface.
 *
 * @author igor@scictrl.com
 * 
 * @param <C> connector implementation
 */
public interface Request<C extends AbstractConnector<?>> extends Iterable<Response<C>>
{
	/**
	 * Optional identification tag of the response. Interpretation depends on asynchronus methods
	 * which generated this response.
	 *
	 * @return identification tag.
	 */
	public Object getTag();
	/**
	 * Returns the Iterator for the response storage.
	 *
	 * @return Response iterator
	 */
	public Iterator<Response<C>> responses();

	/**
	 * Returns true if there are any responses available
	 *
	 * @return true if response available
	 */
	public boolean hasResponse();

	/**
	 * Returns the source of the request
	 *
	 * @return source of the request
	 */
	public Connection<C,?,?> getConnection();

	/**
	 * Returns <code>true</code> if request has been completed.
	 *
	 * @return <code>true</code> if request was completed.
	 */
	public boolean isCompleted();
	
	/**
	 * Returns the first response to this request.
	 *
	 * @return the first response
	 */
	public Response<C> getFirstResponse();
	
	/**
	 * Returns the last arrived response.
	 *
	 * @return the last response
	 */
	public Response<C> getLastResponse();

	/**
	 * Blocks call until last response is received. <br><b>NOTE: </b> call from this method is returned after events
	 * are dispatched on ResponseListeners.
	 *
	 * @return final value received with done event.
	 */
	public Response<C> waitUntilDone();
}

/* __oOo__ */
