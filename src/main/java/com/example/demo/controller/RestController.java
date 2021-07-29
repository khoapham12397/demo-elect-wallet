package com.example.demo.controller;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.SendResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.P2PTransaction;
import com.example.demo.entity.Student;
import com.example.demo.entity.TopupTransaction;
import com.example.demo.model.ActionResponse;
import com.example.demo.model.ChangePassResponse;
import com.example.demo.model.ChangePasswordRequest;
import com.example.demo.model.ChangePinRequest;
import com.example.demo.model.ChangeProfileRequest;
import com.example.demo.model.CreatePresentResponse;
import com.example.demo.model.GetP2PsRequest;
import com.example.demo.model.GetPresentRequest;
import com.example.demo.model.GetPresentResponse;
import com.example.demo.model.GetTopupsRequest;
import com.example.demo.model.RegisterUserRequest;
import com.example.demo.model.RegisterWalletRequest;
import com.example.demo.model.RemovePresentRequest;
import com.example.demo.model.ReturnPresent;
import com.example.demo.model.SendP2PRequest;
import com.example.demo.model.SendP2PResponse;
import com.example.demo.model.SendPresentRequest;
import com.example.demo.model.TopupDirectRequest;
import com.example.demo.model.TopupRequest;
import com.example.demo.model.TopupResponse;
import com.example.demo.model.UserProfileResponse;
import com.example.demo.repository.P2PRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.TopupRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ExampleService;
import com.example.demo.service.TransferService;
import com.example.demo.service.UserService;
import com.example.demo.util.GenerationUtil;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisHashCommands;

@org.springframework.web.bind.annotation.RestController
@CrossOrigin
public class RestController {
	
	@Autowired
	StatefulRedisConnection redisConnection;
	
	@Autowired
	UserRepository userRepository;

	@Autowired
	UserService userService;

	@Autowired
	TransferService transferService;
	
	@Autowired
	TopupRepository topupRepository;
	
	
	@Autowired
	P2PRepository p2pRepository;
	
	@GetMapping(value="/get", produces= {MediaType.APPLICATION_JSON_VALUE})
	public Map<String,Object> getX(){
		RedisHashCommands<String, Object> com = redisConnection.sync();
		return com.hgetall("user:lvhung");
	}
	@GetMapping(value="/getEx", produces= {MediaType.APPLICATION_JSON_VALUE})
	public UserProfileResponse getEx(){
		RedisHashCommands<String, Object> com = redisConnection.sync();
		UserProfileResponse res = userService.getUserProfile("086cd413-c19a-4339-b4b6-99109d54a220");
		
		return res;
	}
	@PostMapping(value ="/postEx", produces= {MediaType.APPLICATION_JSON_VALUE})
	public ChangePasswordRequest postEx(@RequestBody ChangePasswordRequest rqBody){
		//userService.changePassword(rqBody);
		return rqBody;
	}
	@GetMapping(value="/profile")
	public UserProfileResponse getProfile(@RequestParam String userId) {
		return userService.getUserProfile(userId);
	}
	
	@PostMapping(value ="/postEx", produces= {MediaType.APPLICATION_JSON_VALUE})
	public UserProfileResponse changeProfile(@RequestBody ChangeProfileRequest rqBody){
		
		return null;
	}
	@PostMapping(value="/topupDirect", produces = {MediaType.APPLICATION_JSON_VALUE})
	public TopupResponse topupDirect(@RequestBody TopupDirectRequest rq){
		Long txId= transferService.topupDirect(rq);
		
		TopupResponse res = new TopupResponse();
		res.setTimestamp(System.currentTimeMillis());
		if(txId>0L) {
			res.setCode(true);
			res.setMessage("Transaction Successful");
			res.setTransactionId(txId);
		}
		if(txId==0L) {
			res.setCode(false);
			res.setMessage("PIN is not correct");
		}
		if(txId==-1L) {
			res.setCode(false);
			res.setMessage("Wallet is not exist");
		}
		return res;
	}
	
	@PostMapping(value="/topup", produces = {MediaType.APPLICATION_JSON_VALUE})
	public TopupResponse topup(@RequestBody TopupRequest rq){
		//System.out.println(rq.getWalletId()+" topup "+ rq.getAmount()+" , mess: "+ rq.getMessage());
		Long txId= transferService.topup(rq);
		TopupResponse res = new TopupResponse();
		if(txId == 0L) {
			res.setCode(false);
			res.setMessage("Your wallet is not exist");
		}else {
			res.setCode(true);
			res.setMessage("Transaction Successful");
			res.setTimestamp(System.currentTimeMillis());
			res.setTransactionId(txId);
		}
		return res;
	}
	@PostMapping(value="/sendP2P", produces = {MediaType.APPLICATION_JSON_VALUE})
	public SendP2PResponse sendP2P(@RequestBody SendP2PRequest rq){
		System.out.println(rq.getSenderId()+ "send to "+ rq.getReceiverId()+" amount: "+ rq.getAmount());
		
		Long txId;
		SendP2PResponse res = new SendP2PResponse();
		try {
			txId = transferService.sendP2P(rq);
			res.setCode(true);
			res.setMessage("Transaction Successful");
			res.setTimestamp(System.currentTimeMillis());
			res.setTransactionId(txId);
		} catch (SQLException e) {
			res.setMessage("Transaction failed");
			res.setTimestamp(System.currentTimeMillis());
			e.printStackTrace();
		}
	
		return res;
	}
	
	
	
	@PostMapping(value="/createPresent", produces= {MediaType.APPLICATION_JSON_VALUE})
	public CreatePresentResponse createPresent(@RequestBody SendPresentRequest rq){
		CreatePresentResponse act =new CreatePresentResponse();
	 	if(transferService.createPresent(rq)) {
	 		act.setCode(true);
			act.setMessage("Successful");
	 	}else {
	 		act.setCode(false);
	 		act.setMessage("Balance is not enought");
	 	}
	 
		return act;
	}
	
	
	@PostMapping(value="/getPresent", produces= {MediaType.APPLICATION_JSON_VALUE})
	public GetPresentResponse getPresent(@RequestBody GetPresentRequest rq){
		
		GetPresentResponse res = new GetPresentResponse();
		Long amount = transferService.getPresent(rq);
		if(amount>0) {
			res.setCode(true);
			res.setMessage("Transaction successful");
			res.setAmount(amount);
		}else {
			res.setCode(false);
			res.setMessage("Fail to get Lixi");
			res.setAmount(amount);
		}
		return res;
	}
	
	@PostMapping(value="/removePresent", produces= {MediaType.APPLICATION_JSON_VALUE})
	public ActionResponse returnPresent(@RequestBody RemovePresentRequest rq){
		
		ActionResponse res = new ActionResponse();
		transferService.removePresent(rq.getPresentId());
		res.setCode(true);
		res.setMessage("Transaction Sucsessful");
		return res;
	}
	
	@PostMapping(value="/changePassword", produces= {MediaType.APPLICATION_JSON_VALUE})
	public ChangePassResponse changePassword(ChangePasswordRequest rq) {
		
		ChangePassResponse res = new ChangePassResponse();
		String x = userService.changePassword(rq);
		if(x!=null) {
			res.setCode(true);
			res.setMessage("Change Password Successful");
			res.setHashedPassword(x);
		}else {
			res.setCode(false);
			res.setMessage("Your old passwod is not correct.");
		}
		return res;
	}
	public ActionResponse registerWallet(RegisterWalletRequest rq) {
		ActionResponse res = new ActionResponse();
		userService.registerWallet(rq);
		res.setCode(true); res.setMessage("Register Wallet Successful");
		return res;
		
	}
	public ActionResponse registerUser(RegisterUserRequest rq) {
		ActionResponse res = new ActionResponse();
		userService.registerUser(rq);
		res.setCode(true); res.setMessage("Register Wallet Successful");
		return res;
		
	}
	@PostMapping(value="/changePin", produces= {MediaType.APPLICATION_JSON_VALUE})
	public ActionResponse changePin(ChangePinRequest rq) {
		
		ActionResponse res = new ActionResponse();
		if(userService.changePin(rq)) {
			res.setCode(true);
			res.setMessage("Change PIN Successful");
		}else {
			res.setCode(false);
			res.setMessage("Your old PIN is not correct.");
		}
		return res;
	}

	@PostMapping(value="/getTopupsOfUser", produces= {MediaType.APPLICATION_JSON_VALUE})
	public Map<String,Object> getTopupsOfUser(@RequestBody GetTopupsRequest rq){
		Map<String,Object> res = new HashMap<>();
		List<TopupTransaction> lst=null;
		if(rq.getStart() != null) 
			lst = topupRepository.findByUserAndTime(rq.getUserId(), rq.getStart(), rq.getEnd());
		else 
			lst = topupRepository.findAllByUserId(rq.getUserId());
			
		res.put("data", lst);
		return res;
	}
	
	@PostMapping(value="/getP2PsOfUser", produces= {MediaType.APPLICATION_JSON_VALUE})
	public Map<String,Object> getP2PsOfSender(@RequestBody GetP2PsRequest rq){
		String userId = rq.getUserId(); 
		Long start = rq.getStart();
		Long end= rq.getEnd();
		System.out.println("Find: start = "+ rq.getStart()+" , end="+rq.getEnd()+" type="+rq.getType());
		List<P2PTransaction> lst = null;
		switch(rq.getType()) {
		case 0:
			if(rq.getStart()!=null) lst= p2pRepository.findBySenderAndTime(rq.getUserId(), 
					rq.getStart(), rq.getEnd());
			else lst= p2pRepository.findBySenderId(rq.getUserId());
			break;
		case 1:
			if(rq.getStart()!=null) lst =  p2pRepository.findByReceiverAndTime(rq.getUserId(), 
					rq.getStart(), rq.getEnd());
			else lst= p2pRepository.findBySenderId(rq.getUserId());
			break;
		case 2:
			
			if(rq.getStart()!=null) {
				
				List<P2PTransaction> ls1= p2pRepository.findBySenderAndTime(userId, start, end);
				List<P2PTransaction> ls2= p2pRepository.findBySenderAndTime(userId, start, end);
				ls1.addAll(ls2);
				lst = ls1;
				
			}else {
				List<P2PTransaction> l1 =p2pRepository.findByReceiverId(userId);
				List<P2PTransaction> l2 =p2pRepository.findBySenderId(userId);
				l1.addAll(l2);
				lst = l1;
			}
			break;
		}
		Map<String,Object> res= new HashMap<>();
		res.put("data", lst);
		return res;
	}
	@Autowired
	StudentRepository studentRepo;
	
	@Autowired
	ExampleService exService;
	
	@GetMapping(value="/addEx", produces= {MediaType.APPLICATION_JSON_VALUE})
	public Map<String,Object> addEx(){
		
		System.out.println("Co nguoi vao day ");
		Map<String,Object> res = new HashMap<>();
		Student st= new Student();
		st.setId(GenerationUtil.generateId());
		st.setMajor("CS");
		st.setEmail("khoapm2@vng.com");
		st.setName("Pham Minh Khoa");
		exService.add(st);
		res.put("id",st.getId());
		return res;
	}
}
