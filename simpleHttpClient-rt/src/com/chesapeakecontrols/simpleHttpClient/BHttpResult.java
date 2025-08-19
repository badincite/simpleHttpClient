package com.chesapeakecontrols.simpleHttpClient;

import javax.baja.nre.annotations.*;
import javax.baja.sys.*;

@NiagaraType
@NiagaraProperty(name = "ok",       type = "BBoolean",  defaultValue = "BBoolean.FALSE", flags = Flags.READONLY)
@NiagaraProperty(name = "status",   type = "BInteger",  defaultValue = "BInteger.make(0)", flags = Flags.READONLY)
@NiagaraProperty(name = "reason",   type = "BString",   defaultValue = "BString.make(\"\")", flags = Flags.READONLY)
@NiagaraProperty(name = "headers",  type = "BString",   defaultValue = "BString.make(\"\")", flags = Flags.READONLY)
@NiagaraProperty(name = "body",     type = "BString",   defaultValue = "BString.make(\"\")", flags = Flags.READONLY)
@NiagaraProperty(name = "duration", type = "BRelTime",  defaultValue = "BRelTime.makeSeconds(0)", flags = Flags.READONLY)
public class BHttpResult extends BComponent
{
  public void setAll(boolean okVal, int statusVal, String reasonVal,
                     String headersVal, String bodyVal, long millis)
  {
    setOk(BBoolean.make(okVal));
    setStatus(BInteger.make(statusVal));
    setReason(BString.make(reasonVal == null ? "" : reasonVal));
    setHeaders(BString.make(headersVal == null ? "" : headersVal));
    setBody(BString.make(bodyVal == null ? "" : bodyVal));
    setDuration(BRelTime.makeMillis(millis));
  }

  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BHttpResult.class);
}
