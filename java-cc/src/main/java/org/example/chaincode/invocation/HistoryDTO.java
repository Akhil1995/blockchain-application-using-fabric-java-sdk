package org.example.chaincode.invocation;

public class HistoryDTO {
	private String tx_id;
	private String value;
	private TimestampDTO timestamp;
	public TimestampDTO getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(TimestampDTO timestamp) {
		this.timestamp = timestamp;
	}
	public String getTx_id() {
		return tx_id;
	}
	public void setTx_id(String tx_id) {
		this.tx_id = tx_id;
	}
	@Override
	public String toString() {
		return "HistoryDTO [tx_id=" + tx_id + ", value=" + value + ", timestamp=" + timestamp + ", hashCode()="
				+ hashCode() + ", toString()=" + super.toString() + "]";
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public HistoryDTO() {};
}
