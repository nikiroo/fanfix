package be.nikiroo.utils.test;

/**
 * A {@link TestCase} that can be run with {@link TestLauncher}.
 * 
 * @author niki
 */
abstract public class TestCase {
	/**
	 * The type of {@link Exception} used to signal a failed assertion or a
	 * force-fail.
	 * 
	 * @author niki
	 */
	class AssertException extends Exception {
		private static final long serialVersionUID = 1L;

		public AssertException(String reason) {
			super(reason);
		}
	}

	private String name;

	/**
	 * Create a new {@link TestCase}.
	 * 
	 * @param name
	 *            the test name
	 */
	public TestCase(String name) {
		this.name = name;
	}

	/**
	 * Setup the test (called before the test is run).
	 * 
	 * @throws Exception
	 *             in case of error
	 */
	public void setUp() throws Exception {
	}

	/**
	 * Tear-down the test (called when the test has been ran).
	 * 
	 * @throws Exception
	 *             in case of error
	 */
	public void tearDown() throws Exception {
	}

	/**
	 * The test name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Actually do the test.
	 * 
	 * @throws Exception
	 *             in case of error
	 */
	abstract public void test() throws Exception;

	/**
	 * Force a failure.
	 * 
	 * @throws AssertException
	 *             every time
	 */
	public void fail() throws AssertException {
		fail(null);
	}

	/**
	 * Force a failure.
	 * 
	 * @param reason
	 *            the failure reason
	 * @throws AssertException
	 *             every time
	 */
	public void fail(String reason) throws AssertException {
		throw new AssertException("Failed!" + //
				reason != null ? "\n" + reason : "");
	}

	/**
	 * Check that 2 {@link Object}s are equals.
	 * 
	 * @param expected
	 *            the expected value
	 * @param actual
	 *            the actual value
	 * 
	 * @throws AssertException
	 *             in case they differ
	 */
	public void assertEquals(Object expected, Object actual)
			throws AssertException {
		assertEquals(null, expected, actual);
	}

	/**
	 * Check that 2 {@link Object}s are equals.
	 * 
	 * @param the
	 *            error message to display if they differ
	 * @param expected
	 *            the expected value
	 * @param actual
	 *            the actual value
	 * 
	 * @throws AssertException
	 *             in case they differ
	 */
	public void assertEquals(String errorMessage, Object expected, Object actual)
			throws AssertException {

		if (errorMessage == null) {
			errorMessage = generateAssertMessage(expected, actual);
		}

		if ((expected == null && actual != null)
				|| (expected != null && !expected.equals(actual))) {
			throw new AssertException(errorMessage);
		}
	}

	/**
	 * Check that 2 {@link Object}s are equals.
	 * 
	 * @param expected
	 *            the expected value
	 * @param actual
	 *            the actual value
	 * 
	 * @throws AssertException
	 *             in case they differ
	 */
	public void assertEquals(long expected, long actual) throws AssertException {
		assertEquals(new Long(expected), new Long(actual));
	}

	/**
	 * Check that 2 {@link Object}s are equals.
	 * 
	 * @param the
	 *            error message to display if they differ
	 * @param expected
	 *            the expected value
	 * @param actual
	 *            the actual value
	 * 
	 * @throws AssertException
	 *             in case they differ
	 */
	public void assertEquals(String errorMessage, long expected, long actual)
			throws AssertException {
		assertEquals(errorMessage, new Long(expected), new Long(actual));
	}

	/**
	 * Check that 2 {@link Object}s are equals.
	 * 
	 * @param expected
	 *            the expected value
	 * @param actual
	 *            the actual value
	 * 
	 * @throws AssertException
	 *             in case they differ
	 */
	public void assertEquals(boolean expected, boolean actual)
			throws AssertException {
		assertEquals(new Boolean(expected), new Boolean(actual));
	}

	/**
	 * Check that 2 {@link Object}s are equals.
	 * 
	 * @param the
	 *            error message to display if they differ
	 * @param expected
	 *            the expected value
	 * @param actual
	 *            the actual value
	 * 
	 * @throws AssertException
	 *             in case they differ
	 */
	public void assertEquals(String errorMessage, boolean expected,
			boolean actual) throws AssertException {
		assertEquals(errorMessage, new Boolean(expected), new Boolean(actual));
	}

	/**
	 * Check that 2 {@link Object}s are equals.
	 * 
	 * @param expected
	 *            the expected value
	 * @param actual
	 *            the actual value
	 * 
	 * @throws AssertException
	 *             in case they differ
	 */
	public void assertEquals(double expected, double actual)
			throws AssertException {
		assertEquals(new Double(expected), new Double(actual));
	}

	/**
	 * Check that 2 {@link Object}s are equals.
	 * 
	 * @param the
	 *            error message to display if they differ
	 * @param expected
	 *            the expected value
	 * @param actual
	 *            the actual value
	 * 
	 * @throws AssertException
	 *             in case they differ
	 */
	public void assertEquals(String errorMessage, double expected, double actual)
			throws AssertException {
		assertEquals(errorMessage, new Double(expected), new Double(actual));
	}

	/**
	 * Generate the default assert message for 2 different values that were
	 * supposed to be equals.
	 * 
	 * @param expected
	 *            the expected value
	 * @param actual
	 *            the actual value
	 * 
	 * @return the message
	 */
	public static String generateAssertMessage(Object expected, Object actual) {
		return String.format("" //
				+ "Assertion failed!\n" //
				+ "Expected value: [%s]\n" //
				+ "Actual value: [%s]", expected, actual);
	}
}
