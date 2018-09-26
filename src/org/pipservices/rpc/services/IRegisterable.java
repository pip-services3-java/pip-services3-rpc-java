package org.pipservices.rpc.services;

/**
 * Interface to perform on-demand registrations. 
 */
public interface IRegisterable {
	/**
	 * Perform required registration steps.
	 */
	void register();
}
