package org.scictrl.csshell;



/**
 * This interface describes a response to a request. A response to request can be for example
 * a change of value, an error, ...
 *
 * @author igor@scictrl.com
 */
public interface Response<C extends AbstractConnector<?>>
{
	/**
	 * Returns <code>Request</code> object, which initiated this
	 * response.
	 *
	 * @return initial <code>Request</code>
	 */
	public Request<C> getRequest();

	/**
	 * Returns <code>true</code> if this is last response in series of
	 * responses.
	 *
	 * @return <code>true</code> if it is last response
	 */
	public boolean isLast();

	/**
	 * Returns <code>true</code> if the request was successfully completed.
	 *
	 * @return returns true if response is a success
	 */
	public boolean isSuccess();

	/**
	 * Optional identification tag of the response. Interpretation depends on asynchronus methods
	 * which generated this response.
	 *
	 * @return identification tag.
	 */
	public Object getTag();

	/**
	 * If result of the request is an error this method will return it. Otherwise it
	 * will return <code>null</code>
	 *
	 * @return error
	 */
	public Exception getError();

	/**
	 * Returns the source of the response, same as in associated Request.
	 *
	 * @return response source
	 */
	public Connection<C,?,?> getConnection();

	/**
	 * Returns the condition of the response
	 *
	 * @return response condition
	 */
	public Poop<?, ?> getPoop();

}

/* __oOo__ */
