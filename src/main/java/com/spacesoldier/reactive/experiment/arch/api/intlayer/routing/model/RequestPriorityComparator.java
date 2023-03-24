package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model;

import java.util.Comparator;

public class RequestPriorityComparator implements Comparator<RequestPriority> {
    @Override
    public int compare(RequestPriority o1, RequestPriority o2) {
        int result = 0;

        if (o1 == RequestPriority.REGULAR_LEVEL && o2 == RequestPriority.REGULAR_LEVEL){
            result = o1.getPriority().compareTo(o2.getPriority());
        }

        return result;
    }
}
