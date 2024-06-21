package org.scictrl.csshell;

import java.util.EventObject;


/**
 * A base class for events used in asynchronous mode notifications.
 * Whenever a class declares the <code>AsynchronousAccess</code> interface, it
 * must provide listener registration and deregistration methods for
 * <code>ResponseListeenrs</code> to which events of this type are delivered.
 * These events contain the reference to the request object for which they are
 * being dispatched, along with the associated response that is being deliverd
 * by this event notification.<p>This class must be subclassed to have its
 * abstract method implemented; in  addition, the subtype may provide
 * additional access to the containing request /  response pair or other
 * functions specific to the underlying implementation.</p>
 *
 * @author igor@scictrl.com
 * 
 * @param <C> connector implementation
 * 
 */
public class ResponseEvent<C extends AbstractConnector<?>> extends EventObject
{
	private static final long serialVersionUID = 1L;
	/**
	 * Variable holding a reference to the request object. The request
	 * contained by this field is the request for which a new response is
	 * being delivered.
	 */
	protected Request<C> request = null;
	/**
	 * Variable holding a reference to the response object. 
	 */
	protected Response<C> response = null;

	/**
	 * Creates a new instance of the event, by specifying the
	 * <code>AsynchronousAccess</code> source that generated the event and the
	 * request object which caused the notification to occur.
	 *
	 * @param source the source firing the event
	 * @param req request the status of which has changed
	 * @param res a {@link org.scictrl.csshell.Response} object
	 */
	public ResponseEvent(Object source, Request<C> req, Response<C> res)
	{
		super(source);
		assert (req != null);
		this.request = req;
		response = res;
	}

	/**
	 * Returns the request specified as a constructor parameter. This
	 * event instance is delivering a response for the request returned by
	 * this  method.
	 *
	 * @return Object the request object
	 */
	public Request<C> getRequest()
	{
		return request;
	}

	/**
	 * Implementations of this method must return the response object
	 * that is  causing this notification to be delivered. The response object
	 * should  contain the reason for this event, for example new value
	 * delivered, timeout, error, successful completion etc.
	 *
	 * @return Object the response object that is causing this event
	 */
	public Response<C> getResponse()
	{
		return response;
	}

	/**
	 * Returns <code>true</code> if this event is the last event in the
	 * series. In  other words, <code>true</code> indicates that no more
	 * events are forthcoming from the given request.
	 *
	 * @return <code>true</code> if no more events will be delivered for the
	 *         request contained in this event
	 */
	public boolean isLast()
	{
		return response.isLast();
	}
	
	/**
	 * <p>isSuccess.</p>
	 *
	 * @return a boolean
	 */
	public boolean isSuccess()
	{
		return response.isSuccess();
	}
}

/* __oOo__ */
