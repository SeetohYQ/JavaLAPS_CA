package sg.edu.nus.javalapsteam9.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import sg.edu.nus.javalapsteam9.enums.Roles;
import sg.edu.nus.javalapsteam9.model.PublicHoliday;

public final class Util {
	
//	public static final int TEST_EMP_ID = 4;
	
	public static Date now() {
		return getInstance().getTime();
	}
	
	private static Calendar getInstance() {
		return Calendar.getInstance();
	}
	
	private static TimeZone getUtcTZone() {
		return TimeZone.getTimeZone("UTC");
	}
	
	public static Date getUtcDate(Date date) {
		Calendar cal = getInstance();
		cal.setTime(date);
		cal.add(Calendar.HOUR_OF_DAY, 8);
		return cal.getTime();
	}
	
	public static Date parseFromUtcDate(Date date) {
		Calendar cal = getInstance();
		cal.setTime(date);
		cal.add(Calendar.HOUR_OF_DAY, -8);
		return cal.getTime();
	}
	
	public static LocalDate parseDateToLocalDate(Date date, boolean isUtcRequired) {
		Calendar cal = getInstance();
		cal.setTime(date);
		if(isUtcRequired) {
			cal.setTimeZone(getUtcTZone());
		}
		return LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
	}
	
	private static Date parseLocalDateToDate(LocalDate localDate) {
		Calendar cal = getInstance();
		cal.set(Calendar.YEAR, localDate.getYear());
		cal.set(Calendar.MONTH, localDate.getMonthValue() - 1);
		cal.set(Calendar.DAY_OF_MONTH, localDate.getDayOfMonth());
		return cal.getTime();
	}
	
	public static Boolean isValidStartDate(Date date) {
		LocalDate locdate = parseDateToLocalDate(date, false);
		LocalDate today = parseDateToLocalDate(now(), false);
		return !locdate.isBefore(today);
	}
	
	public static Boolean isValidEndDate(Date startDate, Date endDate) {
		LocalDate startdate = parseDateToLocalDate(startDate, false);
		LocalDate enddate = parseDateToLocalDate(endDate, false);
		return (startdate.isBefore(enddate) || startdate.isEqual(enddate));
	}
	
	public static long calculatePeriodBetweenDates(Date startDate, Date endDate) {
		LocalDate startdate = parseDateToLocalDate(startDate, false);
		LocalDate enddate = parseDateToLocalDate(endDate, false).plusDays(1);
		return ChronoUnit.DAYS.between(startdate, enddate);
	}

	public static long calculatePeriodBetweenDatesExcludeHolidays(Date startDate, Date endDate, List<PublicHoliday> holidays) {

		long days = 0;
		LocalDate startdate = parseDateToLocalDate(startDate, false);
		LocalDate enddate = parseDateToLocalDate(endDate, false).plusDays(1);
		for (; startdate.isBefore(enddate); startdate = startdate.plusDays(1)) {
			switch (startdate.getDayOfWeek()) {
			case SATURDAY:
				startdate = startdate.plusDays(1);
				break;
			case SUNDAY:
				break;
			default:
				if(isPublicHoliday(startdate, holidays))
					break;
				++days;
			}
		}

		return days;
	}
	
	public static Boolean isHoliday(LocalDate ldate) {
		switch (ldate.getDayOfWeek()) {
		case SATURDAY:
		case SUNDAY:
			return Boolean.TRUE;
		default:
			return Boolean.FALSE;
		}
	}
	
	public static Boolean isPublicHoliday(final LocalDate date, List<PublicHoliday> holidays) {
		Date dt = parseLocalDateToDate(date);
		for(PublicHoliday holiday : holidays) {
			if(dt.after(holiday.getStartDate()) && date.isBefore(parseDateToLocalDate(holiday.getEndDate(), true).plusDays(1))) {
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}
	
	public static HashSet<Date> getAllDates(Date fromDate, Date toDate) {
		HashSet<Date> set = new HashSet<>();
		LocalDate startdate = parseDateToLocalDate(fromDate, false);
		LocalDate enddate = parseDateToLocalDate(toDate, false).plusDays(1);
		for (; startdate.isBefore(enddate); startdate = startdate.plusDays(1)) {
			set.add(parseLocalDateToDate(startdate));
		}
		return set;
	}
	
	public static String getHomeUrlByRole(String role) {
		String url = "";
		switch(Roles.valueOf(role)) {
		case ADMIN:
			url = "/admin/home";
			break;
		case MANAGER:
			url = "/manager/home";
			break;
		case STAFF:
			url = "/employee/home";
			break;
		}
		return url;
	}
	
}
