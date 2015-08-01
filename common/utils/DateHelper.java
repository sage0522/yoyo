package yoyo.common.utils;

import java.util.Calendar;
import java.util.Date;

public class DateHelper {
	
	public static boolean isSameDate(Date date1,Date date2){
		Calendar dc1 = Calendar.getInstance();
		dc1.setTime(date1);
		Calendar dc2 = Calendar.getInstance();
		dc2.setTime(date2);
		return dc1.get(Calendar.YEAR) == dc2.get(Calendar.YEAR) && 
			   dc1.get(Calendar.MONTH) == dc2.get(Calendar.MONTH) &&
			   dc1.get(Calendar.DATE) == dc2.get(Calendar.DATE);
	}
	private static long efficiencTime;
	public static void StartEfficiency(){
		efficiencTime = System.currentTimeMillis();
		System.out.println("计时开始:"+efficiencTime);
	}
	public static void EndEfficiency(){
		long nowTime = System.currentTimeMillis();
		System.out.println("计时结束:"+(nowTime - efficiencTime));
		efficiencTime = nowTime;
	}
}
