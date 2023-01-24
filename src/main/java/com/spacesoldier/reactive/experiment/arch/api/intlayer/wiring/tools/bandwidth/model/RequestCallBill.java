package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Objects;

@Data @Builder
public class RequestCallBill {
    private String billId;
    private String requestId;
    private String coinId;
    private OffsetDateTime requestStart;
    private OffsetDateTime requestFinish;


    @Override
    public boolean equals(Object thatObj) {
        if (this == thatObj) {
            return true;
        }
        if (thatObj == null || getClass() != thatObj.getClass()) {
            return false;
        }
        RequestCallBill that = (RequestCallBill) thatObj;
        return  Objects.equals(this.billId, that.billId); // &&
                //Objects.equals(this.requestId, that.requestId) &&
                //Objects.equals(this.coinId, that.coinId); // &&
//                Objects.equals(this.requestStart, that.requestStart) &&
//                Objects.equals(this.requestFinish, that.requestFinish);
    }
}
