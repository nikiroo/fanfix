package be.nikiroo.utils;

/**
 * A handler when a trace message is sent or when a recoverable exception was
 * caught by the program.
 * 
 * @author niki
 */
public class TraceHandler {
	private boolean showErrors;
	private boolean showTraces;
	private boolean showErrorDetails;

	/**
	 * Create a default {@link TraceHandler} that will print errors on stderr
	 * (without details) and no traces.
	 */
	public TraceHandler() {
		this(true, false, false);
	}

	/**
	 * Create a default {@link TraceHandler}.
	 * 
	 * @param showErrors
	 *            show errors on stderr
	 * @param showErrorDetails
	 *            show more details when printing errors
	 * @param showTraces
	 *            show traces on stdout
	 */
	public TraceHandler(boolean showErrors, boolean showErrorDetails,
			boolean showTraces) {
		this.showErrors = showErrors;
		this.showErrorDetails = showErrorDetails;
		this.showTraces = showTraces;
	}

	/**
	 * An exception happened, log it.
	 * 
	 * @param e
	 *            the exception
	 */
	public void error(Exception e) {
		if (showErrors) {
			if (showErrorDetails) {
				e.printStackTrace();
			} else {
				error(e.getMessage());
			}
		}
	}

	/**
	 * An error happened, log it.
	 * 
	 * @param message
	 *            the error message
	 */
	public void error(String message) {
		if (showErrors) {
			System.err.println(message);
		}
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
		if (showTraces) {
			System.out.println(message);
		}
	}
}
