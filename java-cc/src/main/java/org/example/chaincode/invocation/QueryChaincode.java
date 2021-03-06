/****************************************************** 
 *  Copyright 2018 IBM Corporation 
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at 
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License.
 */ 
package org.example.chaincode.invocation;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.example.client.CAClient;
import org.example.client.ChannelClient;
import org.example.client.FabricClient;
import org.example.config.Config;
import org.example.user.UserContext;
import org.example.util.Util;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.BlockInfo.EnvelopeInfo;
import org.hyperledger.fabric.sdk.BlockInfo.EnvelopeType;
import org.hyperledger.fabric.sdk.BlockInfo.TransactionEnvelopeInfo;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * 
 * @author Balaji Kadambi
 *
 */

public class QueryChaincode {

	private static Gson gson = new Gson();
	public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n";
    public static final String END_CERT = "\n-----END CERTIFICATE-----\n";
	private static Map<String,TxnInfo> transactionMap = new HashMap<>();
	private static List<String> ccs = new ArrayList<>();
	// sort all transactions as according to the timestamp they were received in
	private static Map<Long,TxnInfo> sortedMap = new TreeMap<>();
	// write key set
	private static Map<String,TxnWrite> writeTxnSet = new HashMap<>();
	// read key set
	private static Map<String,TxnRead> readTxnSet = new HashMap<>();
	// create certificate parser
	private static String parseCertificateOfEndorser(String cert) {
		// parse this certificate
		byte[] decodedCert = Base64.getMimeDecoder().decode(cert.replaceAll(BEGIN_CERT, "").replaceAll(END_CERT, ""));
		try {
			X509Certificate x509 = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decodedCert));
			try {
				LdapName ldapDN = new LdapName(x509.getSubjectX500Principal().getName());
				for(Rdn rdn: ldapDN.getRdns()) {
					if(rdn.getType().equalsIgnoreCase("CN")) {
						return (String)rdn.getValue();
					}
				    System.out.println(rdn.getType() + " -> " + rdn.getValue());
				}
			} catch (InvalidNameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//return x509.getSubjectX500Principal().getName();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	private static void printBlockInfo(TransactionEnvelopeInfo txenin) {
		for(BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo actinfo : txenin.getTransactionActionInfos()) {
			List<String> callArgs = new ArrayList<>();
			// add list of arguments used when the chaincode was called
			
			// get list of endorsers
			for(int j=0;j<actinfo.getEndorsementsCount();j++) {
				String commonName = parseCertificateOfEndorser(actinfo.getEndorsementInfo(j).getId());
			}
			actinfo.getTxReadWriteSet().getNsRwsetInfos().forEach(rwset->{
				try {
					// add all reads/writes that happened to this 
					// big problem here is we need to know all the values that were read in a transaction
					rwset.getRwset().getReadsList().forEach(read->{
						if(read.getVersion().getBlockNum() > 0L) {
							TxnRead txnRead = new TxnRead(read.getKey(),read.getVersion().getBlockNum());
							// if the value is already present in the cache, re use it
							System.out.println(read);
							if(writeTxnSet.containsKey( txnRead.getKey()+txnRead.getBlockNumber())) {
								txnRead.setValueRead(writeTxnSet.get(txnRead.getKey()+txnRead.getBlockNumber()).getValueWritten());
							}
							// Else, fetch the written value from the version block
							else {
								//TxnWrite txnW= getWriteCorrespondingToRead(channel, peer, txnRead.getBlockNumber(), usercontext, read.getKey());
							}
						}
					});
					rwset.getRwset().getWritesList().forEach(write->{
						byte[] writeLen = new byte[write.getValue().size()];
						System.out.println(write);
						write.getValue().copyTo(writeLen, 0);
						String writeStr = new String(writeLen);
					});
				} catch (InvalidProtocolBufferException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		}
	}
	private static void printBlockchainInVariousMethods(Channel channel, Peer peer, User uc) throws ProposalException, InvalidArgumentException {
		for(int i=0;i<channel.queryBlockchainInfo().getHeight();i++) {
			BlockInfo blk = channel.queryBlockByNumber(i);
			blk.getEnvelopeInfos().forEach(env->{
				if(env.getType() == EnvelopeType.TRANSACTION_ENVELOPE) {
					TransactionEnvelopeInfo txenin = (TransactionEnvelopeInfo) env;
					printBlockInfo(txenin);
				}
			});
			try {
				BlockInfo blk1 = channel.queryBlockByHash(blk.getPreviousHash());
				blk1.getEnvelopeInfos().forEach(env->{
					if(env.getType() == EnvelopeType.TRANSACTION_ENVELOPE) {
						TransactionEnvelopeInfo txenin = (TransactionEnvelopeInfo) env;
						printBlockInfo(txenin);
					}
				});
			}
			catch(ProposalException | InvalidArgumentException e) {
				// do nothing
				System.out.println("print this");
			}
		}
	}
	private static TxnWrite getWriteCorrespondingToRead(Channel channel,Peer peer,long blockNumber,User usercontext, String key) {
		final TxnWrite txnWrite = new TxnWrite();
		try {
			BlockInfo blk = channel.queryBlockByNumber(peer, blockNumber,usercontext);
			for(EnvelopeInfo en:blk.getEnvelopeInfos()) {
				if(en.getType() == EnvelopeType.TRANSACTION_ENVELOPE) {
					TransactionEnvelopeInfo txenin = (TransactionEnvelopeInfo) en;
					for(BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo actinfo : txenin.getTransactionActionInfos()) {
						// get list of endorsers
						actinfo.getTxReadWriteSet().getNsRwsetInfos().forEach(rwset->{
							try {
								// add all reads/writes that happened to this
								rwset.getRwset().getWritesList().forEach(write->{
									if(write.getKey().equals(key)) {
										byte[] writeLen = new byte[write.getValue().size()];
										write.getValue().copyTo(writeLen, 0);
										String writeStr = new String(writeLen);
										txnWrite.setKey(key);
										txnWrite.setBlockNumber(blk.getBlockNumber());
										txnWrite.setValueWritten(writeStr);
										writeTxnSet.put(write.getKey()+blk.getBlockNumber(),txnWrite);
									}
								});
								return ;
							} catch (InvalidProtocolBufferException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						});
					}
				}
			}
		}catch(InvalidArgumentException ex) {
			ex.printStackTrace();
		}catch(ProposalException ex) {
			ex.printStackTrace();
		}
		return txnWrite;
	} 
	// timestamp based approach? or block number based approach?
	
	private static List<TxnInfo> getTxnInfoFromBlock(Channel channel, User usercontext,Peer peer,String tx_id) {
		List<TxnInfo> listOfTransactions = new ArrayList<>();
		try {
			BlockInfo blk = channel.queryBlockByTransactionID(peer, tx_id, usercontext);
			for(EnvelopeInfo en: blk.getEnvelopeInfos()) {
				if(en.getType() == EnvelopeType.TRANSACTION_ENVELOPE) {
					TransactionEnvelopeInfo txenin = (TransactionEnvelopeInfo) en;
					printBlockInfo(txenin);
					for(BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo actinfo : txenin.getTransactionActionInfos()) {
						List<String> callArgs = new ArrayList<>();
						// add list of arguments used when the chaincode was called
						for(int j=0;j<actinfo.getChaincodeInputArgsCount();j++) {
							callArgs.add(new String(actinfo.getChaincodeInputArgs(j)));
						}
						TxnInfo txn_info = new TxnInfo(tx_id,txenin.getTimestamp().getTime(),callArgs,actinfo.getChaincodeIDName());
						txn_info.setBlockHeight(blk.getBlockNumber());
						txn_info.setCreatorId(parseCertificateOfEndorser(en.getCreator().getId()));
						// get list of endorsers
						for(int j=0;j<actinfo.getEndorsementsCount();j++) {
							String commonName = parseCertificateOfEndorser(actinfo.getEndorsementInfo(j).getId());
							if(commonName != null)
								txn_info.getEndorserList().add(commonName);
						}
						actinfo.getTxReadWriteSet().getNsRwsetInfos().forEach(rwset->{
							try {
								// add all reads/writes that happened to this 
								// big problem here is we need to know all the values that were read in a transaction
								rwset.getRwset().getReadsList().forEach(read->{
									if(read.getVersion().getBlockNum() > 0L) {
										TxnRead txnRead = new TxnRead(read.getKey(),read.getVersion().getBlockNum());
										// if the value is already present in the cache, re use it
										if(writeTxnSet.containsKey( txnRead.getKey()+txnRead.getBlockNumber())) {
											txnRead.setValueRead(writeTxnSet.get(txnRead.getKey()+txnRead.getBlockNumber()).getValueWritten());
										}
										// Else, fetch the written value from the version block
										else {
											TxnWrite txnW= getWriteCorrespondingToRead(channel, peer, txnRead.getBlockNumber(), usercontext, read.getKey());
											writeTxnSet.put(txnW.getKey()+txnW.getBlockNumber(),txnW);
											txnRead.setValueRead(txnW.getValueWritten());
										}
										txn_info.getReadlist().add(txnRead);
									}
								});
								rwset.getRwset().getWritesList().forEach(write->{
									byte[] writeLen = new byte[write.getValue().size()];
									write.getValue().copyTo(writeLen, 0);
									String writeStr = new String(writeLen);
									TxnWrite txnWrite = new TxnWrite(write.getKey(),blk.getBlockNumber());
									txnWrite.setValueWritten(writeStr);
									txn_info.getWritelist().add(txnWrite);
									writeTxnSet.put(write.getKey()+blk.getBlockNumber(),txnWrite);
								});
							} catch (InvalidProtocolBufferException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						});
						sortedMap.put(txn_info.getTimestamp(), txn_info);
						transactionMap.put(tx_id, txn_info);
						listOfTransactions.add(txn_info);
					}
				}
			}
		}catch(InvalidArgumentException ex) {
			ex.printStackTrace();
		}catch(ProposalException ex) {
			ex.printStackTrace();
		}
		return listOfTransactions;
	}
	private static void pollBlocksForTxns(BlockInfo blk, Set<String> keySet,Channel channel,Peer peer,User usercontext, String chaincode) {
		for(EnvelopeInfo en: blk.getEnvelopeInfos()) {
			if(en.getType() == EnvelopeType.TRANSACTION_ENVELOPE) {
				TransactionEnvelopeInfo txenin = (TransactionEnvelopeInfo) en;
				for(BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo actinfo : txenin.getTransactionActionInfos()) {
					// get list of endorsers
					if(chaincode.equals(actinfo.getChaincodeIDName())) {
						actinfo.getTxReadWriteSet().getNsRwsetInfos().forEach(rwset->{
							try {
								// add all reads/writes that happened to this
								boolean check = false;
								if(rwset.getRwset().getWritesCount() > 0 || rwset.getRwset().getReadsCount()>0) {
									for(int x =0;x<rwset.getRwset().getReadsCount();x++) {
										if(keySet.contains(rwset.getRwset().getReads(x).getKey())) {
											check = true;
										}
									}
								}
								if(check) {
									getTxnInfoFromBlock(channel, usercontext, peer, txenin.getTransactionID());
								}
							} catch (InvalidProtocolBufferException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						});
					}
				}
			}
		}
	}
	public static void main(String args[]) {
		try {
            //Util.cleanUp();
			String caUrl = Config.CA_ORG1_URL;
			CAClient caClient = new CAClient(caUrl, null);
			// Enroll Admin to Org1MSP
			String ccName = args[0];
			String fcnName = "query";
			String tx_id = args[1];
			String[] sArgs = new String[args.length-2];
			for(int i=2;i<args.length;i++)
				sArgs[i-2] = args[i];
			UserContext adminUserContext = new UserContext();
			adminUserContext.setName(Config.ADMIN);
			adminUserContext.setAffiliation(Config.ORG1);
			adminUserContext.setMspId(Config.ORG1_MSP);
			caClient.setAdminUserContext(adminUserContext);
			adminUserContext = caClient.enrollAdminUser(Config.ADMIN, Config.ADMIN_PASSWORD);
			FabricClient fabClient = new FabricClient(adminUserContext);
			
			ChannelClient channelClient = fabClient.createChannelClient(Config.CHANNEL_NAME);
			Channel channel = channelClient.getChannel();
			Peer peer = fabClient.getInstance().newPeer(Config.ORG1_PEER_0, Config.ORG1_PEER_0_URL);
			EventHub eventHub = fabClient.getInstance().newEventHub("eventhub01", "grpc://localhost:7053");
			Orderer orderer = fabClient.getInstance().newOrderer(Config.ORDERER_NAME, Config.ORDERER_URL);
			channel.addPeer(peer);
			channel.addEventHub(eventHub);
			channel.addOrderer(orderer);
			channel.initialize();
			Logger.getLogger(QueryChaincode.class.getName()).log(Level.INFO, "Querying chaincode ...");
			User usercontext = Util.readUserContext(Config.ORG1, sArgs[0]);
			if(usercontext==null) {
				Logger.getLogger(InvokeChaincode.class.getName()).log(Level.SEVERE,"User not registered");
				return;
			}
			// get the keys written by this transaction...
			getTxnInfoFromBlock(channel,usercontext,peer, tx_id);
			TxnInfo first_txn = transactionMap.get(tx_id);
			Set<String> keySet = new HashSet<>();
			first_txn.getWritelist().forEach(write->{
				keySet.add(write.getKey());
			});
			printBlockchainInVariousMethods(channel,peer,usercontext);
			long blockNumber = first_txn.getBlockHeight();
			for(long i=blockNumber;i<channel.queryBlockchainInfo().getHeight();i++) {
				BlockInfo inf = channel.queryBlockByNumber(peer, i, usercontext);
				pollBlocksForTxns(inf, keySet,channel,peer,usercontext,first_txn.getChaincode());
			}
			System.out.println("Execution order: ");
			List<TxnInfo> finalList = new ArrayList<>();
			finalList.addAll(sortedMap.values());
			String final_json= gson.toJson(finalList);
			// order all transactions into a json
			System.out.println(final_json);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
