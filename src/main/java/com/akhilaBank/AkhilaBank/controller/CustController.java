package com.akhilaBank.AkhilaBank.controller;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.akhilaBank.AkhilaBank.DAO.CustomerDAO;
import com.akhilaBank.AkhilaBank.DAO.CustomerDTO;
import com.akhilaBank.AkhilaBank.DAO.HistoryDAO;
import com.akhilaBank.AkhilaBank.DAO.OTP;
import com.akhilaBank.AkhilaBank.models.Customer;
import com.akhilaBank.AkhilaBank.models.TransferHistory;
import com.akhilaBank.AkhilaBank.services.CustomerService;

@Controller
public class CustController {
	@Autowired
	private CustomerService customerService;

	@Autowired
	private CustomerDAO custDao;

	@Autowired
	private HistoryDAO historyDao;

	@Autowired
	private JavaMailSender javaMailSender;

	@GetMapping
	public String viewHomepage() {
		return "index";
	}

	@GetMapping("/register")
	public String showRegistrationForm(Model model) {
		model.addAttribute("customer", new Customer());

		return "signup_form";
	}

	public void sendEmail(String mailTo, String subject, String message) {
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		try {
		MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
		messageHelper.setFrom("tsadmin@gmail.com");
		messageHelper.setTo(mailTo);
		messageHelper.setSubject(subject);
		messageHelper.setText("Ok", message);
		}catch(MessagingException exception) {
			exception.printStackTrace();
		}
		javaMailSender.send(mimeMessage);
	}

	public String opt() {
		Random r = new Random();
		HashSet<Integer> set = new HashSet<Integer>();
		while (set.size() < 1) {
			int ran = r.nextInt(99) + 100000;
			set.add(ran);
		}
		int len = 6;
		String random = String.valueOf(len);
		for (int random1 : set) {
			System.out.println(random1);
			random = Integer.toString(random1);

		}
		return random;
	}

	@PostMapping("/process_register")
	public String processRegister(Customer customer, Model model) {
		customer.setPassword(customer.getPan_number());
		String accountNumber = "2" + new SimpleDateFormat("ddMMyyyySSS").format(Calendar.getInstance().getTime());
		String ifsc_code = new String("AKHI0000123");
		// @SuppressWarnings("deprecation")
		Double balance = new Double(4000.0);
		customer.setAccountnumber(accountNumber);
		customer.setIfsc_code(ifsc_code);
		customer.setBalance(balance);
		String otp = opt();
		customer.setOtp(otp);
		customer.setStatus(false);
		customerService.AddCustomer(customer);
		sendEmail(customer.getEmail(),"Account Details and OTP details","Your Account Number "+accountNumber+ " Your OTP - "+otp);
		model.addAttribute("accountNumber", accountNumber);
		model.addAttribute("ifsc_code", ifsc_code);
		model.addAttribute("balance", balance);
		model.addAttribute("otp",new OTP());

		return "register_success";
	}
	@PostMapping("/validateOTP/{account}")
	public String validateOTP(Model model,OTP otps,@PathVariable String account) {
		Customer customer = custDao.findByAccountNumber(account);
		String message = null;
		if(customer.getOtp().equalsIgnoreCase(otps.getOtp())) {
			customer.setStatus(true);
			custDao.save(customer);
			message = "OTP validated Successfully";
		}else {
			custDao.delete(customer);
			message = "OTP validation failed Please register again";
			custDao.delete(customer);
		}
		model.addAttribute("message",message);
		return "otpSuccess";
	}

	@GetMapping("/home")
	public String homepage(Principal principal, Model model) {
		Customer customer = customerService.getCustomerDetails(principal.getName());
		model.addAttribute("Customer", customer);
		return "home";
	}

	@GetMapping("/viewprofile")
	public String viewpage(Principal principal, Model model) {
		Customer customer = customerService.getCustomerDetails(principal.getName());
		model.addAttribute("Customer", customer);

		return "view_profile";
	}

	@GetMapping("/viewbalance")
	public String viewbalance(Principal principal, Model model) {
		Customer customer = customerService.getCustomerDetails(principal.getName());
		model.addAttribute("Customer", customer);

		return "viewbalance";
	}

	@GetMapping("/transfer")
	public String transfer(Model model) {
		model.addAttribute("Transfer", new CustomerDTO());
		return "transfer";
	}

	@PostMapping("/transfer_process")
	public String transfer_process(CustomerDTO customerDTO, Model model, Principal principal) {
		Customer sender = customerService.getCustomerDetails(principal.getName());
		Customer checkAccount = custDao.findByAccountNumber(customerDTO.getAccountNumber());
		String message = null;
		if (checkAccount != null && !sender.getAccountnumber().equalsIgnoreCase(checkAccount.getAccountnumber())) {
			if (sender.getBalance() >= customerDTO.getBalance()) {
				checkAccount.setBalance(checkAccount.getBalance() + customerDTO.getBalance());
				custDao.save(checkAccount);
				sender.setBalance(sender.getBalance() - customerDTO.getBalance());
				custDao.save(sender);
				TransferHistory history = new TransferHistory(sender.getAccountnumber(),
						checkAccount.getAccountnumber(), customerDTO.getBalance());
				historyDao.save(history);
				message = "Funds transfer Successful";
			} else {
				message = "Insufficient Funds";
			}
		} else {
			message = "Account Details not found";
		}
		model.addAttribute("message", message);

		return "transfer_sccess";
	}

	@PostMapping("/update")
	public String updateCust(Customer customer, Principal principal, Model model) {
		Customer cust = customerService.getCustomerDetails(principal.getName());
		cust.setDob(customer.getDob());
		cust.setMobile(customer.getMobile());
		cust.setEmail(customer.getEmail());
		custDao.save(cust);

		return "redirect:/home";
	}

}
