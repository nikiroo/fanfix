package be.nikiroo.utils;

/**
 * A handler when a trace message is sent or when a recoverable exception was
 * caught by the program.
 * 
 * @author niki
 */
public class TraceHandler {
	private boolean showErrorDetails;
	private boolean showTraces;

	/**
	 * Show more details (usually equivalent to the value of DEBUG).
	 * 
	 * @return TRUE or FALSE
	 */
	public boolean isShowErrorDetails() {
		return showErrorDetails;
	}

	/**
	 * Show more details (usually equivalent to the value of DEBUG).
	 * 
	 * @param showErrorDetails
	 *            TRUE or FALSE
	 */
	public void setShowErrorDetails(boolean showErrorDetails) {
		this.showErrorDetails = showErrorDetails;
	}

	/**
	 * Show DEBUG traces.
	 * 
	 * @return TRUE or FALSE
	 */
	public boolean isShowTraces() {
		return showTraces;
	}

	/**
	 * Show DEBUG traces.
	 * 
	 * @param showTraces
	 *            TRUE or FALSE
	 */
	public void setShowTraces(boolean showTraces) {
		this.showTraces = showTraces;
	}

	/**
	 * An exception happened, log it.
	 * 
	 * @param e
	 *            the exception
	 */
	public void error(Exception e) {
		if (isShowErrorDetails()) {
			e.printStackTrace();
		} else {
			error(e.getMessage());
		}
	}

	/**
	 * An error happened, log it.
	 * 
	 * @param message
	 *            the error message
	 */
	public void error(String message) {
		System.err.println(message);
	}

	/**
	 * A trace happened, show it.
	 * <p>
	 * Will only be effective if {@link TraceHandler#isShowTraces()} is true.
	 * 
	 * @param message
	 *            the trace message
	 */
	public void trace(String message) {
		if (isShowTraces()) {
			System.out.println(message);
		}
	}
}
