/*
Copyright IBM Corp. 2016 All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

		 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package main


import (
	"fmt"
	"strconv"
	"strings"
	"encoding/json"
	"encoding/pem"
	"crypto/x509"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	"github.com/hyperledger/fabric/common/util"
	pb "github.com/hyperledger/fabric/protos/peer"
)
var logger = shim.NewLogger("assign")
var supplier string = "Supplier"
var manufacturer string = "Manufacturer"
var dealer string = "Dealer"
var car string = "Car"
var retire string = "Retired"

// SimpleChaincode example simple Chaincode implementation
type SimpleChaincode struct {
}

type AirBag struct {
	ID int
	OwnerID string
	OwnerRole string 
}
type Car struct {
	ID string
	AirBagID int
}
func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response  {
	return shim.Success(nil)
}

// Transaction makes payment of X units from A to B
func (t *SimpleChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	
	function, args := stub.GetFunctionAndParameters()
	function  =strings.ToLower(function)
	// check role validity
	var role []string
	role = strings.Split(args[0],".")
	//var err error
	// check and map roles here
	//serializedID,err  := stub.GetCreator()
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
	if cert.Subject.CommonName != args[0]{
		return shim.Error(fmt.Sprintf("Certificate doesn't match given role, exiting immediately"))
	}
	
	/*id,_ := stub.GetCreator()
	cert,err := x509.ParseCertificate(id)
	if cert == nil || err != nil{
		return shim.Error(string(cert.RawSubject))
	}
	if len(role)!=2 {
		return shim.Error(string(id))
	}*/
	if function == "addairbag" {
		// Adds an airbag to the ledger
		return t.addairbag(stub, args)
	}

	if function == "transferairbag" {
		// transfers an airbag to another manufacturer
		return t.transferairbag(stub, args)
	}
	if function == "mountairbag" {
		// mounts an airbag into a car
		return t.mountairbag(stub, args)
	}

	if function == "replaceairbag" {
		// replaces an airbag in a car
		return t.replaceairbag(stub, args)
	}
	if function == "recallairbag" {
		// Recalls an airbag
		return t.recallairbag(stub, args)
	}
	if function == "checkairbag" {
		// Checks an airbag in a car
		return t.checkairbag(stub, args)
	}
	if function == "query" {
		// Checks an airbag in a car
		return t.queryHistory(stub, args)
	}
	return shim.Error(fmt.Sprintf("Unknown action, check the first argument, must be one of 'addairbag', 'transferairbag','mountairbag','replaceairbag','recallairbag', or 'checkairbag'. But got: %v", function))
}


func (t *SimpleChaincode) addairbag(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var A, B string
	var role []string
	var abid int
	var err error
	var str []byte
	var nAB AirBag

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 3, function followed by Role and AirBagID")
	}
	A = args[0]
	B = args[1]
	role = strings.Split(A,".")
	// State guard
	if role[0] != supplier {
		return shim.Error("Only Supplier Role can invoke addairbag function")
	}
	Avalbytes, err := stub.GetState(B)
	fmt.Printf("%s\n",string(Avalbytes))
	if Avalbytes != nil {
		return shim.Error("Airbag ID already present, use new ID")
	}

	abid,err = strconv.Atoi(B)
	if err != nil {
		return shim.Error("Expecting integer value for ID")
	}
	if abid<1000 || abid>9999{
		return shim.Error("Value must be from 1000 to 9999")
	}
	// Write the state back to the ledger
	nAB = AirBag{abid,role[1],supplier}
	str,err = json.Marshal(nAB)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(strconv.Itoa(abid), []byte(str))
	if err != nil {
		return shim.Error(err.Error())
	}
	// adding a sample invocation to check th functionality
	ccArgs := util.ToChaincodeArgs("set","simple",string(str))
	resp:= stub.InvokeChaincode("sacc",ccArgs,"mychannel")
	fmt.Printf("%s\n",resp.Message)
    return shim.Success([]byte("Airbag successfully added"))
}

func (t *SimpleChaincode) queryHistory(stub shim.ChaincodeStubInterface, args []string) pb.Response{
	if len(args) != 2{
		return shim.Error("Incorrect Number of arguments...")
	}
	result:=make([]string,0)
	histd,err:= stub.GetHistoryForKey(args[1])
	if err != nil{
		fmt.Println(err)
		return shim.Error(err.Error())
	}
	for histd.HasNext() {
		mod,err1 := histd.Next()
		if err1 != nil{
			return shim.Error(err1.Error())
		}
		fmt.Println(mod.TxId)
		fmt.Println(mod.Timestamp)
		out_bytes,_ := json.Marshal(mod)
		result = append(result,string(out_bytes))
	}
	outputBytes,_ := json.Marshal(&result)
	return shim.Success(outputBytes)
}

func (t *SimpleChaincode) transferairbag(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	// must be an invoke
	var A, B string    // Entities
	var role []string
	var newownerrole []string
	var abid int          // Airbag IDs
	var err error
	var str []byte
	var nAB AirBag

	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting 3, function followed by Role and AirBagID")
	}
	A = args[0]
	B = args[1]
	role = strings.Split(A,".")
	newownerrole = strings.Split(B,".")
	// function and role guard
	if role[0] != supplier && role[0] != manufacturer && role[0] != dealer{
		return shim.Error("Only Suppliers, Manufacturers and Dealers can call this API")
	}
	// check id validity
	abid,err = strconv.Atoi(args[2])
	if err != nil {
		return shim.Error("Expecting integer value for ID")
	}
	// see if airbag is available
	Avalbytes, err := stub.GetState(strconv.Itoa(abid))
	if Avalbytes == nil {
		return shim.Error("Airbag ID invalid")
	}
	err = json.Unmarshal(Avalbytes,&nAB)
	if err != nil {
		return shim.Error("Something went wrong")
	}
	// check owner to verify ownership
	if nAB.OwnerID != role[1] || nAB.OwnerRole != role[0]{
		return shim.Error("User not the owner of AirBag")
	}
	if nAB.OwnerRole == supplier && newownerrole[0] != manufacturer {
		return shim.Error("Suppliers can only transfer to Manufacturers")
	}
	if nAB.OwnerRole == manufacturer && newownerrole[0] != dealer {
		return shim.Error("Manufacturers can only transfer to Dealers")
	}
	if nAB.OwnerRole == dealer && newownerrole[0] != dealer {
		return shim.Error("Dealers can only transfer to Dealers")
	}
	nAB.OwnerRole = newownerrole[0]
	nAB.OwnerID = newownerrole[1]
	str,err = json.Marshal(nAB)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(strconv.Itoa(abid), []byte(str))
	if err != nil {
		return shim.Error(err.Error())
	}
	Cvalbytes,_:= stub.GetState(strconv.Itoa(abid))
	fmt.Println(string(Cvalbytes))
    return shim.Success([]byte("Airbag successfully transferred"));
}

func (t *SimpleChaincode) mountairbag(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	// must be an invoke
	var A, B string    // Entities
	var role []string
	var abid int          // Airbag IDs
	var err error
	var str []byte
	var str1 []byte
	var nCar Car
	var nAB AirBag

	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting 3, function followed by Role and AirBagID")
	}
	A = args[0]
	B = args[1]
	role = strings.Split(A,".")
	// function and role guard
	if role[0] != manufacturer {
		return shim.Error("Only Manufacturers can call this API")
	}
	// check ID validity
	abid,err = strconv.Atoi(B)
	if err != nil {
		return shim.Error("Expecting integer value for ID")
	}
	// get airbag
	Avalbytes, err := stub.GetState(strconv.Itoa(abid))
	if Avalbytes == nil {
		return shim.Error("Airbag ID invalid")
	}
	err = json.Unmarshal(Avalbytes,&nAB)
	if err != nil {
		return shim.Error("Something went wrong")
	}
	// check owner, if owner is right
	if nAB.OwnerID != role[1] || nAB.OwnerRole != role[0]{
		return shim.Error("User not the owner of AirBag")
	}
	// check if car is in the blockchain
	Bvalbytes,err := stub.GetState(args[2])
	fmt.Printf("%s\n",string(Bvalbytes))
	if Bvalbytes != nil{
		return shim.Error("Car not new, airbag already mounted previously [OR] Car ID conflicts with AirBagId, rename Car")
	}
	nAB.OwnerID = args[2]
	nAB.OwnerRole = car
	nCar = Car{args[2],abid}
	str,err = json.Marshal(nAB)
	if err != nil {
		return shim.Error(err.Error())
	}
	str1,err = json.Marshal(nCar)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(strconv.Itoa(abid), []byte(str))
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(nCar.ID, []byte(str1))
	if err != nil {
		return shim.Error(err.Error())
	}
    return shim.Success([]byte("Airbag  successfully mounted onto Car "));
}

func (t *SimpleChaincode) replaceairbag(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	// must be an invoke
	var A, B string    // Entities
	var role []string
	var abid int          // Airbag IDs
	var oldabid int
	var err error
	var str []byte
	var str1 []byte
	var str2 []byte
	var nCar Car
	var nAB AirBag

	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting 3, function followed by Role and AirBagID")
	}
	A = args[0]
	B = args[1]
	role = strings.Split(A,".")
	// function and role guard
	if role[0] != dealer {
		return shim.Error("Only Dealers can call this API")
	}
	abid,err = strconv.Atoi(B)
	if err != nil {
		return shim.Error("Expecting integer value for ID")
	}
	Avalbytes, err := stub.GetState(strconv.Itoa(abid))
	if Avalbytes == nil {
		return shim.Error("Airbag ID invalid")
	}
	err = json.Unmarshal(Avalbytes,&nAB)
	if err != nil {
		return shim.Error("Something went wrong")
	}
	// check owner, if owner is right
	if nAB.OwnerID != role[1] || nAB.OwnerRole != role[0]{
		return shim.Error("User not the owner of AirBag")
	}
	// check if car is in the blockchain
	Bvalbytes,err := stub.GetState(args[2])
	if Bvalbytes == nil{
		return shim.Error("Error getting Car, Car not available in system")
	}
	// change owner to car
	nAB.OwnerID = args[2]
	nAB.OwnerRole = car
	err = json.Unmarshal(Bvalbytes,&nCar)
	if err != nil {
		return shim.Error(err.Error())
	}
	oldabid = nCar.AirBagID
	// retire this AirBag
	var oldAB AirBag
	CValbytes,err := stub.GetState(strconv.Itoa(oldabid))
	if CValbytes == nil{
		return shim.Error("Something went wrong")
	}
	err = json.Unmarshal(CValbytes,&oldAB)
	if err != nil{
		return shim.Error(err.Error())
	}
	oldAB.OwnerRole = retire
	oldAB.OwnerID = ""
	// change AirBagID in Car
	nCar.AirBagID = abid
	// update states for all 3
	str,err = json.Marshal(nAB)
	if err != nil {
		return shim.Error(err.Error())
	}
	str1,err = json.Marshal(nCar)
	if err != nil {
		return shim.Error(err.Error())
	}
	str2,err = json.Marshal(oldAB)
	if err != nil{
		return shim.Error(err.Error())
	}
	err = stub.PutState(strconv.Itoa(abid), []byte(str))
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(nCar.ID, []byte(str1))
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(strconv.Itoa(oldabid), []byte(str2))
	if err != nil {
		return shim.Error(err.Error())
	}
    return shim.Success([]byte("Airbag successfully replaced"));
}


func (t *SimpleChaincode) recallairbag(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	// must be an invoke
	var A, B string    // Entities
	var role []string
	var abid int          // Airbag IDs
	var err error
	var str []byte
	var nAB AirBag

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 3, function followed by Role and AirBagID")
	}
	A = args[0]
	B = args[1]
	role = strings.Split(A,".")
	// function and role guard
	if role[0] != manufacturer {
		return shim.Error("Only Manufacturers can call this API")
	}
	abid,err = strconv.Atoi(B)
	if err != nil {
		return shim.Error("Expecting integer value for ID")
	}
	Avalbytes, err := stub.GetState(strconv.Itoa(abid))
	fmt.Printf("%s\n",string(Avalbytes))
	if Avalbytes == nil {
		return shim.Error("Airbag ID invalid")
	}
	err = json.Unmarshal(Avalbytes,&nAB)
	if err != nil {
		return shim.Error("Something went wrong")
	}
	if nAB.OwnerRole == car{
		// if airbag is mounted, recall airbag from car and delete car
		Bvalbytes,err := stub.GetState(nAB.OwnerID)
		fmt.Printf("%s\n",string(Bvalbytes))
		if Bvalbytes == nil {
			return shim.Error("Something went wrong")
		}
		// delete car
		err = stub.DelState(nAB.OwnerID)
		if err != nil {
			return shim.Error("Failed to delete state")
		}
	} else {
		// check owner
		// check owner, if owner is right
		if nAB.OwnerID != role[1] || nAB.OwnerRole != role[0]{
			return shim.Error("User not the owner of AirBag")
		}
	}
	nAB.OwnerRole = retire
	nAB.OwnerID = ""
	// retire airbag
	str,err = json.Marshal(nAB)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(strconv.Itoa(abid), []byte(str))
	if err != nil {
		return shim.Error(err.Error())
	}
    return shim.Success([]byte("Airbag successfully recalled"));
}

func (t *SimpleChaincode) checkairbag(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	// must be an invoke
	var A string    // Entities
	var role []string
	var err error

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1, function followed by Role")
	}
	A = args[0]
	role = strings.Split(A,".")
	// function and role guard
	if role[0] != car {
		return shim.Error("Only Cars can call this API")
	}
	// get Car
	Bvalbytes,err := stub.GetState(role[1])
	fmt.Printf("%s\n",string(Bvalbytes))
	if Bvalbytes == nil  || err != nil {
		return shim.Success([]byte("false"))
	}else{
		return shim.Success([]byte("true"))
	}
}

func main() {
	err := shim.Start(new(SimpleChaincode))
	if err != nil {
	}
}
