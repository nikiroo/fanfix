package be.nikiroo.utils;

/**
 * A handler when a trace message is sent or when a recoverable exception was
 * caught by the program.
 * 
 * @author niki
 */
public class TraceHandler {
	private final boolean showErrors;
	private final boolean showErrorDetails;
	private final int traceLevel;

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
	 *            show level 1 traces on stdout, or no traces at all
	 */
	public TraceHandler(boolean showErrors, boolean showErrorDetails,
			boolean showTraces) {
		this(showErrors, showErrorDetails, showTraces ? 1 : 0);
	}

	/**
	 * Create a default {@link TraceHandler}.
	 * 
	 * @param showErrors
	 *            show errors on stderr
	 * @param showErrorDetails
	 *            show more details when printing errors
	 * @param traceLevel
	 *            show traces of this level or lower (0 means "no traces",
	 *            higher means more traces)
	 */
	public TraceHandler(boolean showErrors, boolean showErrorDetails,
			int traceLevel) {
		this.showErrors = showErrors;
		this.showErrorDetails = showErrorDetails;
		this.traceLevel = Math.max(traceLevel, 0);
	}

	/**
	 * The trace level of this {@link TraceHandler}.
	 * 
	 * @return the level
	 */
	public int getTraceLevel() {
		return traceLevel;
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
	 * By default, will only be effective if {@link TraceHandler#showTraces} is
	 * true
	 * <p>
	 * A call to this method is equivalent to a call to
	 * {@link TraceHandler#trace(String, int)} with a level of 1.
	 * 
	 * @param message
	 *            the trace message
	 */
	public void trace(String message) {
		trace(message, 1);
	}

	/**
	 * A trace happened, show it.
	 * <p>
	 * By default, will only be effective if {@link TraceHandler#showTraces} is
	 * true and the level is lower or equal to {@link TraceHandler#traceLevel}.
	 * 
	 * @param message
	 *            the trace message
	 * @param level
	 *            the trace level
	 */
	public void trace(String message, int level) {
		if (traceLevel > 0 && level <= traceLevel) {
			System.out.println(message);
		}
	}
}
