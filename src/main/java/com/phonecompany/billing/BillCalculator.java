package com.phonecompany.billing;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BillCalculator implements TelephoneBillCalculator{

    private final int MINUTE_PRICE_IN_PEAK = 1;
    private final double MINUTE_PRICE_OUTSIDE_PEAK = 0.5D;
    private final double REDUCED_MINUTE_PRICE = 0.2D;
    private final int MINUTES_FOR_REDUCED_PRICE = 5;

    @Override
    public BigDecimal calculate(String phoneLog) {
        List<PhoneCall> phoneCallList = getPhoneCalls(phoneLog);
        return getTotalPhoneCallCost(phoneCallList);
    }

    /**
     * Parse the string from the csv file to PhoneCall objects
     * @param phoneLog String with phone log from csv file
     * @return List of parsed phone calls
     */
    private List<PhoneCall> getPhoneCalls(String phoneLog){
        List<PhoneCall> phoneCallList = new ArrayList<>();

        //Split phone log by lines
        String[] phoneLogRows = phoneLog.split("\n");
        for(String row : phoneLogRows){
            String[] rowToParse = row.split(",");

            long phoneNumber = parseLongNumber(rowToParse[0]);
            Date startDate = parseDate(rowToParse[1]);
            Date endDate = parseDate(rowToParse[2]);

            phoneCallList.add(new PhoneCall(phoneNumber, startDate, endDate));
        }

        return phoneCallList;
    }

    /**
     * Helper method to parse long numbers
     * @param number String with long number
     * @return Parsed long number if successful, otherwise zero
     */
    private long parseLongNumber(String number){
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Helper method to parse date
     * @param date String with date in format (dd-MM-yyyy HH:mm:ss)
     * @return Parsed date if successful, otherwise null
     */
    private Date parseDate(String date){
        try {
            return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Get most called number from phone calls list
     * @param phoneCallList List with phone calls
     * @return Number, that is most called
     */
    private long getMostCalledNumber(List<PhoneCall> phoneCallList){
        //Create hashmap with occurrence counter
        HashMap<Long, Integer> numberCounter = new HashMap<>();
        int maxCount = 0;
        long maxCountNumber = 0L;

        for (PhoneCall call : phoneCallList){
            //Add one to number calls counter
            numberCounter.compute(call.getNumber(), (key, value) -> (value == null) ? 1 : value + 1);
            int numberCount = numberCounter.get(call.getNumber());

            //Check if counter of number call is larger or if counter is same and number have larger arithmetic value
            if(numberCount > maxCount || (numberCount == maxCount && call.getNumber() > maxCountNumber)){
                maxCount = numberCount;
                maxCountNumber = call.getNumber();
            }
        }

        return maxCountNumber;
    }

    /**
     * Get duration of phone call
     * @param call Phone call object
     * @return Duration of phone call in minutes rounded up
     */
    private int getCallDurationInMinutes(PhoneCall call){
        return (int) Math.ceil((call.getEndDate().getTime() - call.getStartDate().getTime()) / 1000.0 / 60.0);
    }

    /**
     * Helper method to get hour date
     * @param date Date object
     * @return Hour of the date
     */
    private int getHourFromDate(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Compute bill for call
     * @param call Phone call object
     * @return Price of phone call
     */
    private BigDecimal getPhoneCallCost(PhoneCall call){
        int callStartHour = getHourFromDate(call.getStartDate());
        int callDuration = getCallDurationInMinutes(call);

        double priceToUse = (callStartHour >= 8 && callStartHour < 16) ? MINUTE_PRICE_IN_PEAK : MINUTE_PRICE_OUTSIDE_PEAK;

        if(callDuration > MINUTES_FOR_REDUCED_PRICE){
            return BigDecimal.valueOf(priceToUse * MINUTES_FOR_REDUCED_PRICE + (callDuration - MINUTES_FOR_REDUCED_PRICE) * REDUCED_MINUTE_PRICE);
        }else {
            return BigDecimal.valueOf(priceToUse * callDuration);
        }
    }

    /**
     * Get bill for phone call list
     * @param phoneCallList List of phone call objects
     * @return Price for list of phone call
     */
    private BigDecimal getTotalPhoneCallCost(List<PhoneCall> phoneCallList){
        BigDecimal total = new BigDecimal(0);
        long mostCalledNumber = getMostCalledNumber(phoneCallList);

        for (PhoneCall phoneCall : phoneCallList){
            //Most called number is free
            if(phoneCall.getNumber() == mostCalledNumber){
                continue;
            }

            total = total.add(getPhoneCallCost(phoneCall));
        }

        return total;
    }

}
