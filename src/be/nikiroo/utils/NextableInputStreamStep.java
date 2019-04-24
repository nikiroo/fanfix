package be.nikiroo.utils;

public class NextableInputStreamStep {
	private int stopAt;
	private boolean disabled;
	private int pos;
	private int resumeLen;
	private int last = -1;
	private int skip;

	public NextableInputStreamStep(int byt) {
		stopAt = byt;
	}

	// do NOT stop twice on the same item
	public int stop(byte[] buffer, int pos, int len) {
		for (int i = pos; i < len; i++) {
			if (buffer[i] == stopAt) {
				if (i > this.last) {
					// we skip the sep
					this.skip = 1;
					
					this.pos = pos;
					this.resumeLen = len;
					this.last = i;
					return i;
				}
			}
		}

		return -1;
	}

	public int getResumeLen() {
		return resumeLen;
	}
	
	public int getSkip() {
		return skip;
	}

	public void clearBuffer() {
		this.last = -1;
		this.pos = 0;
		this.skip = 0;
		this.resumeLen = 0;
	}

	public boolean isEnabled() {
		return !disabled;
	}
}
