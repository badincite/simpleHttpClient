package com.chesapeakecontrols.simpleHttpClient;

import javax.baja.nre.annotations.*;
import javax.baja.sys.*;

@NiagaraType
@NiagaraProperty(name = "request",    type = "BHttpRequest",  defaultValue = "new BHttpRequest()")
@NiagaraProperty(name = "lastResult", type = "BHttpResult",   defaultValue = "new BHttpResult()", flags = Flags.READONLY)
@NiagaraAction  (name = "execute")
public class BHttpSender extends BComponent
{
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BHttpSender.class);

  @Override
  public void invoke(Action action) {
    if (action.equals(getExecute())) {
      BSimpleHttpClientService svc = (BSimpleHttpClientService) Services.lookup(BSimpleHttpClientService.TYPE);
      BHttpResult r;
      if (svc == null || !svc.getEnabled().getBoolean()) {
        r = new BHttpResult();
        r.setAll(false, 0, "Service not available or disabled", "", "", 0);
      } else {
        r = svc.doSend(getRequest());
      }
      setLastResult(r);
      return;
    }
    super.invoke(action);
  }
}
