package sg.edu.nus.javalapsteam9.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import sg.edu.nus.javalapsteam9.model.LeaveApplication;
import sg.edu.nus.javalapsteam9.model.User;
import sg.edu.nus.javalapsteam9.service.EmailService;
import sg.edu.nus.javalapsteam9.service.ManagerService;
import sg.edu.nus.javalapsteam9.service.StaffService;
import sg.edu.nus.javalapsteam9.util.SecurityUtil;
import sg.edu.nus.javalapsteam9.validation.CustomFieldError;

@Controller
@RequestMapping("/manager")
public class ManagerController {

	private static final String HOME = "/manager/home";

	@Autowired
	private ManagerService managerService;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private StaffService staffService;
	
	@GetMapping("/home")
	public String outstandingLeaves(Model model) {

		// To update method to retrieve both "APPLIED" and "OUTSTANDING" leaves
		// Also, to show leaves for employees whose reportsTo = manager's staffId (not
		// done yet)
		List<LeaveApplication> leaves = managerService.findAllOutstandingLeaves();
		model.addAttribute("leaves", leaves);
		model.addAttribute("homeurl", HOME);
		model.addAttribute("role", SecurityUtil.getCurrentLoggedUserRole());
		return "manager/home";
	}

	@GetMapping("/details/{id}")
	public String leaveDetails(Model model, @PathVariable("id") Integer id) {

		LeaveApplication leave = managerService.findLeaveById(id);
		model.addAttribute("leave", leave);
		model.addAttribute("homeurl", HOME);
		model.addAttribute("role", SecurityUtil.getCurrentLoggedUserRole());
		return "manager/leave_details";
	}

	@GetMapping("/approve/{id}")
	public String approveLeave(@PathVariable("id") Integer id, @RequestParam(name = "comment", required = false) String comment) {
		
		managerService.approveLeave(id, comment);
		LeaveApplication leave = managerService.findLeaveById(id);
		sendApprovalEmail(leave);
		
		return "redirect:/manager/home";
	}
	
	@PostMapping("/approve")
	public String approveLeaveTry(LeaveApplication leave) {
		
		managerService.approveLeave(leave.getId(), leave.getComment());
		sendApprovalEmail(leave);
		
		return "redirect:/manager/home";
	}

	@PostMapping("/reject")
	public String rejectLeave(@Valid @ModelAttribute("leave") LeaveApplication leave, BindingResult result, Model model) {

		model.addAttribute("homeurl", HOME);
		if (leave.getComment() == null || leave.getComment().isEmpty()) {
			CustomFieldError err = new CustomFieldError("leave", "comment", "Please provide comments for rejection");
			result.addError(err);
		}

		if (result.hasFieldErrors("comment")) {
			return "manager/leave_details";
		}

		managerService.rejectLeave(leave.getId(), leave.getComment());
		sendRejectionEmail(leave);
		
		return "redirect:/manager/home";
	}

	@GetMapping("/sub_leave_history")
	public String viewSubLeaveHist(Model model) {
		List<List<LeaveApplication>> subLeaves = managerService.getSubLeaveHistory();
		model.addAttribute("subLeaves", subLeaves);
		List<User> subordinates = managerService.getSub();
		model.addAttribute("homeurl", HOME);
		model.addAttribute("subordinates", subordinates);
		model.addAttribute("role", SecurityUtil.getCurrentLoggedUserRole());
		return "manager/sub_leave_history";
	}

	@GetMapping("/sub_leave_history/view/{leaveid}")
	public String viewLeaveId(Model model, @PathVariable("leaveid") Integer leaveId) {
		LeaveApplication leave = managerService.findLeaveById(leaveId);
		model.addAttribute("form", leave);
		model.addAttribute("homeurl", HOME);
		model.addAttribute("role", SecurityUtil.getCurrentLoggedUserRole());
		return "manager/view_leave";
	}

	@GetMapping("/movementregister/{month}")
	public String viewStaffMovementForSelectedMonth(Model model, @RequestParam("month") String selectedMonth) {

		List<String> months = new ArrayList<String>();
		months.add("January");
		months.add("February");
		months.add("March");
		months.add("April");
		months.add("May");
		months.add("June");
		months.add("July");
		months.add("August");
		months.add("September");
		months.add("October");
		months.add("November");
		months.add("December");
		model.addAttribute("months", months);

		LocalDate currentDate = LocalDate.now();
		int currentYear = currentDate.getYear();

		int startMonthInNum = months.indexOf(selectedMonth) + 1;
		int endMonthInNum = months.indexOf(selectedMonth) + 1;

		model.addAttribute("movements",
				managerService.getLeavesByMonthYear(startMonthInNum, currentYear, endMonthInNum, currentYear));
		model.addAttribute("selectedMonth", selectedMonth);

		return "manager/movement_register";
	}

	@GetMapping("/movementregister")
	public String viewStaffMovement(Model model) {

		List<String> months = new ArrayList<String>();
		months.add("January");
		months.add("February");
		months.add("March");
		months.add("April");
		months.add("May");
		months.add("June");
		months.add("July");
		months.add("August");
		months.add("September");
		months.add("October");
		months.add("November");
		months.add("December");
		model.addAttribute("months", months);

		int startMonthInNum, endMonthInNum;

		LocalDate currentDate = LocalDate.now();
		String currentMonth = currentDate.getMonth().toString();
		String currentMonthFormatted;
		currentMonthFormatted = currentMonth.substring(0, 1)
				+ currentMonth.substring(1, currentMonth.length()).toLowerCase();
		startMonthInNum = months.indexOf(currentMonthFormatted) + 1;
		endMonthInNum = months.indexOf(currentMonthFormatted) + 1;
		int currentYear = currentDate.getYear();

		model.addAttribute("selectedMonth", currentMonthFormatted);
		model.addAttribute("currentYear", currentYear);
		model.addAttribute("movements",
				managerService.getLeavesByMonthYear(startMonthInNum, currentYear, endMonthInNum, currentYear));

		return "manager/movement_register";
	}
	
	public String sendApprovalEmail(LeaveApplication leave) {
		
		User staff = staffService.findStaffByLeaveId(leave.getId());
		String comments = leave.getComment();

		if (comments == "") {
			comments = "Your leave application (id: " + leave.getId() + ") has been approved. \n\nManager's comments: N/A\n\nClick here to view: http://localhost:8080/laps/employee/leave/view/" + leave.getId();
		}
		else {
			 comments = "Your leave application has been approved. \n\nManager's comments: " + leave.getComment() + "\n\nClick here to view: http://localhost:8080/laps/employee/leave/view/" + leave.getId();
		}
		
		emailService.sendSimpleMessage(staff.getEmail(),"Your leave application (id: " + leave.getId() + ") has been processed.",comments);
		
		return "redirect:/manager/home";
	}

	public String sendRejectionEmail(LeaveApplication leave) {
		
		User staff = staffService.findStaffByLeaveId(leave.getId());
		String comments = "Your leave application (id: " + leave.getId() + ") has been rejected. \n\nManager's comments: " + leave.getComment()+ "\n\nClick here to view: http://localhost:8080/laps/employee/leave/view/" + leave.getId();
		emailService.sendSimpleMessage(staff.getEmail(),"Your leave application (id: " + leave.getId() + ") has been processed",comments);
		
		return "redirect:/manager/home";
	}
}
