package org.scictrl.csshell;

import java.util.EventListener;


/**
 * Each object that implements the <code>AsynchronousAccess</code>
 * interafce, must provide listener registration / deregistration methods for
 * listeners of this type. Whenever an asynchronous method (set / get / actions etc)
 * is invoked, a request object is issued as the return value of that method. Whenever
 * a response is provided for that request by the underlying implementation, all
 * response listeners are notified. If a listener is interested in the completion
 * of a specific request, it must first enquire the event object to see if the event
 * notification is being delivered for that specific request.
 *
 * @author igor@scictrl.com
 * 
 * @param <C> connector implementation
 */
public interface ResponseListener<C extends AbstractConnector<?>> extends EventListener
{
	/**
	 * Event notification specifying that the request state has
	 * changed. This happens in all cases where the request is modified by the
	 * underlying implementation.  Examples are: the arrival of new response,
	 * timeout or error condition, successful completion of the request etc.
	 *
	 * @param event the event carrying the new response and the request for
	 *        which the notification is being delivered
	 */
	public void responseReceived(ResponseEvent<C> event);
}

/* __oOo__ */
