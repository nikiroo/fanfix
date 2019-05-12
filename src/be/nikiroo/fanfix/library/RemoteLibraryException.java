package be.nikiroo.fanfix.library;

import java.io.IOException;

/**
 * Exceptions sent from remote to local.
 * 
 * @author niki
 */
public class RemoteLibraryException extends IOException {
	private static final long serialVersionUID = 1L;

	/**
	 * Wrap an {@link IOException} to allow it to pass across the network.
	 * 
	 * @param cause
	 *            the excption to wrap
	 */
	public RemoteLibraryException(IOException cause) {
		super(cause);
	}

	@Override
	public synchronized IOException getCause() {
		return (IOException) super.getCause();
	}
}
