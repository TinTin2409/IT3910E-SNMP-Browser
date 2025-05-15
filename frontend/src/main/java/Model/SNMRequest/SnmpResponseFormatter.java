package Model.SNMRequest;

import java.util.Calendar;
import java.util.Map;


public class SnmpResponseFormatter {

    public static String format(Object variable, String dataType, Map<String, Object> constraints) {
        //System.out.println("Formatting variable: " + variable + " of type: " + dataType);
        if (dataType == null) {
            return variable.toString();
        }

        switch (dataType.trim().toLowerCase()) {
            case "integer32":
            case "integer":
                return formatInteger(variable, constraints);

            case "dateandtime":
                return formatDataAbdTime(variable, constraints);

            default:
                return variable.toString();
        }
    }
    private static String formatInteger(Object variable, Map<String, Object> constraints) {
        //System.out.println("Formatting Integer with constraints: " + constraints);
        if (constraints != null && constraints.containsKey("enumeration")) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> enumeration = (Map<String, Integer>) constraints.get("enumeration");
            System.out.println("Enumeration: " + enumeration);
            for (Map.Entry<String, Integer> entry : enumeration.entrySet()) {
                System.out.println("Checking if " + entry.getValue() + " equals " + variable);
                if (entry.getValue().equals(Integer.parseInt(variable.toString()))) {
                    return "(" + variable + ") " + entry.getKey();
                }
            }
        }
        return variable.toString();
    }

    public static int[] octetStringToBytes(String response) {
        // Split string into its parts
        String[] bytes;
        bytes = response.split("[^0-9A-Fa-f]");

        // Initialize result
        int[] result;
        result = new int[bytes.length];

        // Convert bytes
        int counter;
        for (counter = 0; counter < bytes.length; counter++)
            result[counter] = Integer.parseInt(bytes[counter], 16);

        return (result);
    } //return a byte array

    public static String formatDataAbdTime(Object variable, Map<String, Object> constraints) {

        String responseHasDataAndTimeDataType = variable.toString();
        // Convert into array of bytes
        int[] bytes;
        bytes = octetStringToBytes(responseHasDataAndTimeDataType);

        // Maybe nothing specified
        if (bytes[0] == 0)
            return (null);

        // Extract parameters
        int year, month, day, hour, minute, second, deci_sec = 0, offset = 0;

        //Parse the bytes
        year = (bytes[0] * 256) + bytes[1];
        month = bytes[2];
        day = bytes[3];
        hour = bytes[4];
        minute = bytes[5];
        second = bytes[6];
        if (bytes.length >= 8)
            deci_sec = bytes[7];
        if (bytes.length >= 10) {
            offset = bytes[9] * 60;
            if (bytes.length >= 11)
                offset += bytes[10];
            if (bytes[8] == '-')
                offset = -offset;
            offset *= 60 * 1000;
        }

        // Get current DST and time zone offset
        Calendar calendar;
        int my_dst;
        int my_zone;
        calendar = Calendar.getInstance();
        my_dst = calendar.get(Calendar.DST_OFFSET);
        my_zone = calendar.get(Calendar.ZONE_OFFSET);


        // Compose result
        // Month to be converted into 0-based
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, deci_sec * 100);

        // Reset DST
        calendar.add(Calendar.MILLISECOND, my_dst);


        // If the offset is set, we have to convert the time using the offset of our time zone
        if (offset != 0) {
            int delta;
            delta = my_zone - offset;
            calendar.add(Calendar.MILLISECOND, delta);
        }

        // Return result
        return (calendar.getTime().toString());}}

