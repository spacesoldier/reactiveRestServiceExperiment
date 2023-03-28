package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.providers;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.channels.FluxChannel;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// some sort of dynamic storage for wires
// which could be used for connecting the reactive streams
// with a number of subscribers
public class FluxChannelProvider {
    @Builder
    private FluxChannelProvider(){

    }
    private Map<String, FluxChannel> requestStreams = new ConcurrentHashMap<>();

    private final String unitName = "flux manager";
    private final Logger logger = LoggerFactory.getLogger(unitName);

    public FluxChannel newStream(String streamName){
        FluxChannel stream = null;
        if (!requestStreams.containsKey(streamName)){
            stream = new FluxChannel(streamName);
            requestStreams.put(streamName, stream);
        }
        return stream;
    }

    private FluxChannel streamOnDemand(String streamName){
        FluxChannel result = null;
        if (!requestStreams.containsKey(streamName)){
            result = newStream(streamName);
            logger.info(String.format("New stream: %s", streamName));
        } else {
            result = requestStreams.get(streamName);
        }
        return result;
    }

    // get the object sink for publishing the items
    public Consumer getSink(String streamName){
        return streamOnDemand(streamName).getStreamInput();
    }

    // get the object flux for sending it to the clients
    public Flux getStream(String streamName){
        return streamOnDemand(streamName).getStreamToSubscribe();
    }

    public Consumer getExistingSink(String channelName){
        Consumer sink = null;
        if (requestStreams.containsKey(channelName)){
            sink = requestStreams.get(channelName).getStreamInput();
        }

        return sink;
    }
}
