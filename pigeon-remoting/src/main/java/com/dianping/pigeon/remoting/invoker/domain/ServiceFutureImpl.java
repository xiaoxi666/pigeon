/**
 * Dianping.com Inc.
 * Copyright (c) 2003-2013 All Rights Reserved.
 */
package com.dianping.pigeon.remoting.invoker.domain;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.dianping.dpsf.async.ServiceFuture;
import com.dianping.pigeon.extension.ExtensionLoader;
import com.dianping.pigeon.log.LoggerLoader;
import com.dianping.pigeon.monitor.Monitor;
import com.dianping.pigeon.monitor.MonitorLogger;
import com.dianping.pigeon.remoting.common.domain.InvocationResponse;
import com.dianping.pigeon.remoting.common.exception.InvalidParameterException;
import com.dianping.pigeon.remoting.common.exception.RpcException;
import com.dianping.pigeon.remoting.common.util.Constants;
import com.dianping.pigeon.remoting.invoker.util.InvokerUtils;

public class ServiceFutureImpl extends CallbackFuture implements ServiceFuture {

	private static final Logger logger = LoggerLoader.getLogger(ServiceFutureImpl.class);

	private static final MonitorLogger monitorLogger = ExtensionLoader.getExtension(Monitor.class).getLogger();

	private long timeout = Long.MAX_VALUE;

	private Thread callerThread;

	public ServiceFutureImpl(long timeout) {
		super();
		this.timeout = timeout;
		callerThread = Thread.currentThread();
	}

	@Override
	public Object _get() throws InterruptedException {
		return _get(this.timeout);
	}

	@Override
	public Object _get(long timeoutMillis) throws InterruptedException {
		InvocationResponse response = null;
		monitorLogger.logEvent("PigeonCall.future", "", "timeout=" + timeoutMillis);
		try {
			response = super.get(timeoutMillis);
		} catch (Exception e) {
			RpcException rpcEx = null;
			if (e instanceof RpcException) {
				rpcEx = (RpcException) e;
			} else {
				rpcEx = new RpcException(e);
			}
			logger.error(rpcEx);
			monitorLogger.logError(rpcEx);
			throw rpcEx;
		}
		if (response.getMessageType() == Constants.MESSAGE_TYPE_SERVICE) {
			return response.getReturn();
		} else if (response.getMessageType() == Constants.MESSAGE_TYPE_EXCEPTION) {
			RpcException cause = InvokerUtils.toRpcException(response);
			logger.error("error with future call", cause);
			monitorLogger.logError("error with future call", cause);
			throw cause;
		} else if (response.getMessageType() == Constants.MESSAGE_TYPE_SERVICE_EXCEPTION) {
			RuntimeException cause = InvokerUtils.toApplicationRuntimeException(response);
			// logger.error("error with remote business future call", cause);
			// monitorLogger.logError("error with remote business future call",
			// cause);
			throw cause;
		} else {
			RpcException e = new InvalidParameterException("unsupported response with message type:"
					+ response.getMessageType());
			monitorLogger.logError(e);
			throw e;
		}

	}

	@Override
	public Object _get(long timeout, TimeUnit unit) throws InterruptedException {
		return _get(unit.toMillis(timeout));
	}

	protected void processContext() {
		Thread currentThread = Thread.currentThread();
		if (currentThread == callerThread) {
			super.processContext();
		}
	}

}
