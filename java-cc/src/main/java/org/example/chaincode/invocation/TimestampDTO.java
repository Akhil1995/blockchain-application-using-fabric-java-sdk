package org.example.chaincode.invocation;

public class TimestampDTO {
	private long seconds;
	public long getSeconds() {
		return seconds;
	}
	public void setSeconds(long seconds) {
		this.seconds = seconds;
	}
	public long getNanos() {
		return nanos;
	}
	public void setNanos(long nanos) {
		this.nanos = nanos;
	}
	@Override
	public String toString() {
		return "TimestampDTO [nanos=" + nanos + ", seconds=" + seconds + "]";
	}
	private long nanos;
}
