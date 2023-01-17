package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class RoutingHelperImpl implements RoutingHelper{

    private String requestPriorityPrefix = "<[|";
    private String requestPrioritySuffix = "|]>";

    private String searchRegex = "<\\[\\|(.*?)\\|\\]>";
    private Pattern searchPattern = Pattern.compile(searchRegex);

    @Builder
    private RoutingHelperImpl(){

    }

    @Override
    public String defineRequestPriority(String requestId, RequestPriority priority) {
        StringBuilder output = new StringBuilder();

        output.append(requestPriorityPrefix);
        output.append(priority.str());
        output.append(requestPrioritySuffix);
        output.append(requestId);

        return output.toString();
    }

    @Override
    public Boolean requestIsPrioritised(String requestId) {
        return requestId.startsWith(requestPriorityPrefix);
    }

    @Override
    public RequestPriority parsePriorityForRequestID(String requestId) {
        RequestPriority output = RequestPriority.BACKGROUND;

        Integer priority = -1;



        if (requestIsPrioritised(requestId)){
            String priorityDataStr = getPriorityString(requestId);

            if (priorityDataStr != null){
                try {
                    // first of all let's look if we have priority as integer
                    priority = Integer.parseInt(priorityDataStr);
                } catch (Exception e){
                    // probably we have a string priority parameter
                    //log.info("[REQUEST]: request priority is not an integer " + matcher.group(1));
                }

                if (priority == -1){
                    // ok, let's check if the priority is in form of a string
                    output = RequestPriority.parsePriorityFromStr(priorityDataStr);
                } else {
                    // good, let's get it from Integer
                    output = RequestPriority.parsePriorityInt(priority);
                }
            }


        }

        return output;
    }

    @Nullable
    private String getPriorityString(String requestId) {
        String priorityDataStr = null;
        Matcher matcher = searchPattern.matcher(requestId);

        if (matcher.find()){
            priorityDataStr = matcher.group(1);
        }
        return priorityDataStr;
    }

    @Override
    public String removePriorityFromRequestID(String requestId) {
        String output = requestId;

        if (requestIsPrioritised(requestId)){
            StringBuilder priorityDataStr = new StringBuilder(requestPriorityPrefix);
            priorityDataStr.append(getPriorityString(requestId));
            priorityDataStr.append(requestPrioritySuffix);
            output = requestId.substring(priorityDataStr.toString().length());
        }

        return output;
    }

}
