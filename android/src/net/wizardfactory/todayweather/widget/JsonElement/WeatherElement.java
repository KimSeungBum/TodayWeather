package net.wizardfactory.todayweather.widget.JsonElement;

import android.util.Log;

import net.wizardfactory.todayweather.widget.Data.WeatherData;
import net.wizardfactory.todayweather.widget.Data.WidgetData;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This is result of weather server.
 * JsongString of result parsing current, shorts, shortest weather data.
 */
public class WeatherElement {
    // default parsing value
    public final static double DEFAULT_WEATHER_DOUBLE_VAL = -9999.99;
    public final static int DEFAULT_WEATHER_INT_VAL = -9999;

    private String regionName = null;
    private String cityName = null;
    private String townName = null;
    private WeatherShortElement[] weatherShorts = null;
    private WeatherShortestElement weatherShortest = null;
    private WeatherCurrentElement weatherCurrent = null;

    public String getRegionName() { return regionName; }

    public String getCityName() {
        return cityName;
    }

    public String getTownName() {
        return townName;
    }

    public WeatherShortElement[] getWeatherShorts() {
        return weatherShorts;
    }

    public WeatherShortestElement getWeatherShortest() {
        return weatherShortest;
    }

    public WeatherCurrentElement getWeatherCurrent() {
        return weatherCurrent;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public void setTownName(String townName) {
        this.townName = townName;
    }

    public void setWeatherShorts(WeatherShortElement[] weatherShorts) {
        this.weatherShorts = weatherShorts;
    }

    public void setWeatherShortest(WeatherShortestElement weatherShortest) {
        this.weatherShortest = weatherShortest;
    }

    public void setWeatherCurrent(WeatherCurrentElement weatherCurrent) {
        this.weatherCurrent = weatherCurrent;
    }

    // parsing weather data from json string.
    public static WeatherElement parsingWeatherElementString2Json(String jsonStr){
        WeatherElement retElement = new WeatherElement();

        try {
            JSONObject reader = new JSONObject(jsonStr);
            if(reader != null) {
                retElement.setRegionName(reader.getString("regionName"));
                retElement.setCityName(reader.getString("cityName"));
                retElement.setTownName(reader.getString("townName"));
                retElement.setWeatherShorts(WeatherShortElement.parsingShortElementString2Json(reader.getJSONArray("short").toString()));
                retElement.setWeatherShortest(WeatherShortestElement.parsingShortestElementString2Json(reader.getJSONObject("shortest").toString()));
                retElement.setWeatherCurrent(WeatherCurrentElement.parsingCurrentElementString2Json(reader.getJSONObject("current").toString()));
            }
            else{
                Log.e("WeatherElement", "Json string is NULL");
            }
        } catch (JSONException e) {
            Log.e("WeatherElement", "JSONException: " + e.getMessage());
            e.printStackTrace();
        }

        return retElement;
    }

    /*
        Aargument
        string date : yyyyMMdd
        string time : HHmm
     */
    public static Date makeDateFromStrDateAndTime(String date, String time){
        Date retDate = null;

        SimpleDateFormat transFormat = new SimpleDateFormat("yyyyMMddHHmm");
        try {
            retDate = transFormat.parse(date+time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return retDate;
    }

    // make widget data using this class members.
    public WidgetData makeWidgetData(){
        WidgetData retWidgetData =  null;

        if(weatherCurrent != null){
            retWidgetData = new WidgetData();
            // set location
            retWidgetData.setLoc(townName);

            // get current data
            WeatherData current = new WeatherData();
            current.setSky(weatherCurrent.getSky());
            current.setPty(weatherCurrent.getPty());
            current.setLgt(weatherCurrent.getLgt());
            current.setTemperature(weatherCurrent.getT1h());
            current.setMaxTemperature(findMaxTemperature(true));
            current.setMinTemperature(findMinTemperature(true));

            // get yesterday data
            WeatherData yesterday = new WeatherData();
            WeatherShortElement yesterdayElement = getYesterdayWeatherFromCurrentTime();
            if(yesterdayElement != null){
                yesterday.setSky(yesterdayElement.getSky());
                yesterday.setTemperature(yesterdayElement.getT3h());
                yesterday.setPty(yesterdayElement.getPty());
            }
            yesterday.setMaxTemperature(findMaxTemperature(false));
            yesterday.setMinTemperature(findMinTemperature(false));

            retWidgetData.setCurrentWeather(current);
            retWidgetData.setYesterdayWeather(yesterday);
        }
        else{
            Log.e("WeatherElement", "CurrentWeather is NULL!!");
        }

        return retWidgetData;
    }


    private double findMaxTemperature(boolean isToday){
        double retMaxTemperature = DEFAULT_WEATHER_DOUBLE_VAL;
        String cmpDateStr = null;

        if(isToday){
            cmpDateStr = weatherCurrent.getStrDate();
        }
        else{
            cmpDateStr = findYesterdayDateString();
        }

        if(cmpDateStr != null){
            if(weatherShorts != null && weatherShorts.length > 0) {
                for (int i = 0; i < weatherShorts.length; i++) {
                    if (weatherShorts[i].getStrDate().equals(cmpDateStr)) {
                        double tempTmx = weatherShorts[i].getTmx();
                        if (tempTmx != 0 && (retMaxTemperature < tempTmx)) {
                            retMaxTemperature = tempTmx;
                        }
                    }
                }
                if(retMaxTemperature == DEFAULT_WEATHER_DOUBLE_VAL){
                    retMaxTemperature = 0;
                }
            }
        }

        return retMaxTemperature;
    }

    private double findMinTemperature(boolean isToday){
        double retMinTemperature = -DEFAULT_WEATHER_DOUBLE_VAL;
        String cmpDateStr = null;

        if(isToday){
            cmpDateStr = weatherCurrent.getStrDate();
        }
        else{
            cmpDateStr = findYesterdayDateString();
        }

        if(cmpDateStr != null){
            if(weatherShorts != null && weatherShorts.length > 0) {
                for (int i = 0; i < weatherShorts.length; i++) {
                    if (weatherShorts[i].getStrDate().equals(cmpDateStr)) {
                        double tempTmn = weatherShorts[i].getTmn();
                        if (tempTmn != 0 && (retMinTemperature > tempTmn)) {
                            retMinTemperature = tempTmn;
                        }
                    }
                }
                if(retMinTemperature == -DEFAULT_WEATHER_DOUBLE_VAL){
                    retMinTemperature = 0;
                }
            }
        }

        return retMinTemperature;
    }


    //    Find yesterday weather base on current time.
     private WeatherShortElement getYesterdayWeatherFromCurrentTime(){
        WeatherShortElement retShortElement = null;

        if(weatherCurrent == null || weatherShorts == null || weatherShorts.length == 0){
            Log.e("WeatherElement", "cur or shorts element is NULL");
        }
        else {
            Date shortestDate = weatherCurrent.getDate();
            if (shortestDate != null) {
                // compare base time
                String yesterdayCmpStrDate = findYesterdayDateString();
                Date cmpDate = WeatherElement.makeDateFromStrDateAndTime(yesterdayCmpStrDate, weatherCurrent.getStrTime());

                int nearnestIdx = -1;
                long minInterval = 999999999;
                for (int i =0; i < weatherShorts.length; i++){
                    if(weatherShorts[i] != null){
                        long term = Math.abs(cmpDate.getTime() - weatherShorts[i].getDate().getTime());
                        if(minInterval > term){
                            nearnestIdx = i;
                            minInterval = term;
                        }
                        else{
                            // do nothing
                        }
                    }
                    else{
                        Log.e("WeatherElement", "short["+ i + "] element is NULL");
                    }
                }

                if(nearnestIdx != -1 ){
                    retShortElement = weatherShorts[nearnestIdx];
                }
            } else {
                Log.e("WeatherElement", "shortestDate Date is NULL");
            }
        }

        return retShortElement;
    }

    // find yesterday string base on current weather date.
    private String findYesterdayDateString() {
        String retYesterdayString = null;

        if (weatherCurrent != null){
            // today time - 1day
            Date yesterdayDate = new Date(weatherCurrent.getDate().getTime() - (1000 * 60 * 60 * 24));
            DateFormat sdFormat = new SimpleDateFormat("yyyyMMdd");

            // compare base time
            retYesterdayString = sdFormat.format(yesterdayDate);
        }

        return retYesterdayString;
    }
}
