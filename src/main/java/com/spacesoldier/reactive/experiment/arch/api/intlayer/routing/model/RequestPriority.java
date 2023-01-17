package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model;

import java.util.HashMap;
import java.util.Map;

public enum RequestPriority {
    USER_LEVEL(0),                  // absolute top priority to speed up processing of user-level objects
    REGULAR_LEVEL(1),               // allows to use objects in real priority queues
    BACKGROUND(Integer.MAX_VALUE);  // absolute low priority for internal background processing

    private static Map<Integer, RequestPriority> valueIntMap = new HashMap<>(){{
        put(0, USER_LEVEL);
        put(1, REGULAR_LEVEL);
        put(Integer.MAX_VALUE, BACKGROUND);
    }};

    private static Map<RequestPriority, String> valueStrMap = new HashMap<>(){{
        put(USER_LEVEL,"USER");
        put(REGULAR_LEVEL,"NORM");
        put(BACKGROUND,"BACK");
    }};

    private static Map<String, RequestPriority> strValMap = new HashMap<>(){{
        put("USER", USER_LEVEL);
        put("NORM", REGULAR_LEVEL);
        put("BACK", BACKGROUND);
    }};

    public static RequestPriority parsePriorityFromStr(String strPriority){
        RequestPriority output = BACKGROUND;

        if (strValMap.containsKey(strPriority)){
            output = strValMap.get(strPriority);
        } else {
            if (strPriority.startsWith("NORM")){
                output = REGULAR_LEVEL;
                String priorityStrVal = strPriority.substring("NORM".length());
                Integer levelVal = -1;
                try {
                    levelVal = Integer.parseInt(priorityStrVal);
                } catch (Exception e){

                }
                if (levelVal > 0){
                    output.setPriority(levelVal);
                } else {
                    if (levelVal == 0){
                        output = USER_LEVEL;
                        output.setPriority(0);
                    }
                }
            }
        }

        return output;
    }

    public Integer priority;

    private static final Map<Integer,String> naming = new HashMap<>(){{
        put(0, "user level");
        put(1, "regular level");
        put(Integer.MAX_VALUE, "background");
    }};

    public String str(){
        return valueStrMap.get(this);
    }

    public String description(){
        String output = null;
        if (this == REGULAR_LEVEL) {
            output = naming.get(priority)+" "+priority;
        }

        return output;
    }

    public Integer priorityLevelDown(){
        if (this == REGULAR_LEVEL){
            if (priority < Integer.MAX_VALUE){
                priority+=1;
            }
        }
        return priority;
    }

    public Integer priorityLevelUp(){
        if (this == REGULAR_LEVEL){
            if (priority > 1){
                priority-=1;
            }
        }
        return priority;
    }

    private void setPriority(Integer newPriority){
        if (this == REGULAR_LEVEL){
            if (newPriority > 0){
                priority = newPriority;
            }
        }
    }

    private RequestPriority(Integer priority){
        this.priority = priority;
    }

    public static RequestPriority parsePriorityInt(Integer priority){
        RequestPriority output = BACKGROUND;

        if (valueIntMap.containsKey(priority)){
            output = valueIntMap.get(priority);
        } else {
            if (priority > 0){
                output = REGULAR_LEVEL;
                output.setPriority(priority);
            }
        }

        return output;
    }


}
