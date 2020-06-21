package main


import (
	"fmt"
	"strconv"
	"strings"
	"encoding/json"
	"encoding/pem"
	"crypto/x509"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)
var logger = shim.NewLogger("repaircc")
type TxnInfo struct {
	timestamp uint64
	chaincode string
	txn_id string
	creatorId string
	blockHeight uint64
	endorsers []string
	status bool
	//reads []TxnRead
	//writes []TxnWrite
}
type RepairProposal struct {
	repairId string
	txns TxnInfo[]
	status bool
}
// SimpleChaincode example simple Chaincode implementation
type SimpleChaincode struct {
}
func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response  {
	repairContext = false
	return shim.Success(nil)
}

// Transaction makes payment of X units from A to B
func (t *SimpleChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	
	function, args := stub.GetFunctionAndParameters()
	function  =strings.ToLower(function)
	// check role validity
	mymap,err := stub.GetCreator()
	if err != nil{
		return shim.Error(err.Error())
	}
	fmt.Println(mymap)
	//sid := &msp.SerializedIdentity{}
	//payload,_:= utils.GetHeader(mymap)
	sign,err1 := stub.GetSignedProposal()
	if err1 != nil{
		return shim.Error(err.Error())
	}
	//sign.Reset()
	sign.Signature = mymap
	fmt.Println(sign.Signature)
	var str string = sign.String()
	fmt.Println(str)
	var beg string = "-----BEGIN CERTIFICATE-----"
	var end string = "-----END CERTIFICATE-----"
	cerlVal:= strings.ReplaceAll(beg+ strings.Split(strings.Split(str,beg)[2],end)[0]+end,"\\n","\n")
	fmt.Println([]byte(cerlVal))
	block,_:= pem.Decode([]byte(cerlVal))
	if block == nil{
		return shim.Error(fmt.Sprintf("Pem parsing error %f\n",block.Bytes))
	}
	fmt.Println(block.Bytes)
	cert,err2 := x509.ParseCertificate(block.Bytes)
    	if err2 != nil{
		return shim.Error(err2.Error())
	}
	if len(role) != 2 {
        	return shim.Error(fmt.Sprintf("Role format not correct "))
	}
	fmt.Println(cert.Subject.CommonName)
	
	//if cert.Subject.CommonName != args[0]{
		//return shim.Error(fmt.Sprintf("Certificate doesn't match given role, exiting immediately"))
	//}
	
	/*id,_ := stub.GetCreator()
	cert,err := x509.ParseCertificate(id)
	if cert == nil || err != nil{
		return shim.Error(string(cert.RawSubject))
	}
	if len(role)!=2 {
		return shim.Error(string(id))
	}*/
	if function == "acceptrepair" {
		// accepts a repair from a client
		return t.acceptrepair(stub, cert.Subject.CommonName,rp_id,tx_id)
	}

	if function == "initiaterepair" {
		// initiates a repaor
		return t.initiaterepair(stub, args)
	}
	
	if function == "queryrepair" {
		return t.queryrepair(stub,args)
	}
	return shim.Error(fmt.Sprintf("Unknown action, check the first argument, must be one of 'acceptrepair', 'initiaterepair','queryrepair'. But got: %v", function))
}