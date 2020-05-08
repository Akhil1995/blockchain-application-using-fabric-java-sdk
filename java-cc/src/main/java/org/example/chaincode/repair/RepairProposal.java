package org.example.chaincode.repair;

import org.example.chaincode.invocation.TxnInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RepairProposal {
	private String repairId;
	private List<TxnInfo> txns;
	public List<TxnInfo> getListOfTxns() {
		return txns;
	}
	public void setListOfTxns(List<TxnInfo> listOfTxns) {
		this.txns = listOfTxns;
	}
	public String getRepairId() {
		return repairId;
	}
	public RepairProposal() {
		// TODO Auto-generated constructor stub
		this.repairId = UUID.randomUUID().toString();
		this.txns = new ArrayList<>();
	}
}
