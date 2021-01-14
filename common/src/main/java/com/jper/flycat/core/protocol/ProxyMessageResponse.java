package com.jper.flycat.core.protocol;

import lombok.Getter;
import lombok.Setter;

/**
 * @author ywxiang
 * @date 2021/1/14 下午7:18
 */
@Getter
@Setter
public class ProxyMessageResponse {
    private int result;
    private String message;

    public ProxyMessageResponse(int result, String message) {
        this.result = result;
        this.message = message;
    }
}
