package com.ttdo.gateway.helper.api.reactive;

import com.ttdo.gateway.helper.entity.ResponseContext;
import org.springframework.web.server.ServerWebExchange;

import java.io.IOException;

public interface ReactiveAuthenticationHelper {

    /**
     * 授权服务接口
     *
     * @param exchange
     * @return ResponseContent
     * @throws IOException
     */
    ResponseContext authentication(ServerWebExchange exchange) throws IOException;
}
