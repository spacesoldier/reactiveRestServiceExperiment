package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.providers;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.channels.MonoChannel;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// some sort of dynamic storage for wires
// which could be used for connecting the reactive requests with responses

public class MonoChannelProvider {

    @Builder
    private MonoChannelProvider(){

    }
    private Map<String, MonoChannel> requestWires = Collections.synchronizedMap(new HashMap<>());

    private final String unitName = "mono wiring manager";
    private final Logger logger = LoggerFactory.getLogger(unitName);

    public MonoChannel newWire(String wireId){
        MonoChannel stream = null;
        if (!requestWires.containsKey(wireId)){
            stream = new MonoChannel(wireId);
            requestWires.put(wireId, stream);
        }
        return stream;
    }

    private MonoChannel wireOnDemand(String wireId){
        MonoChannel result = null;
        if (!requestWires.containsKey(wireId)){
            result = newWire(wireId);
            // logger.info(String.format("New request wiring: %s", wireId));
        } else {
            result = requestWires.get(wireId);
        }
        return result;
    }

    // get the object sink for publishing the items
    public Consumer getInput(String wireId){
        Consumer output = null;

        MonoChannel wire = wireOnDemand(wireId);
        if (wire != null){
            output = wire.getMonoInput();
        }
        return output;
    }

    // get the mono for sending it to the clients
    // and after the request is processed we dispose the mono from our scope
    public Mono getOutput(String wireId){
        return wireOnDemand(wireId)
                .getMonoToSubscribe()
                .doOnSuccess(removeWire(wireId));
    }

    private Consumer removeWire(String wireId) {

        return input -> {
            logger.info("remove");
            requestWires.remove(wireId);
        };
    }
}
