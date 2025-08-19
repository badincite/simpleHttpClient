package com.chesapeakecontrols.simpleHttpClient;

import javax.baja.nre.annotations.*;
import javax.baja.sys.*;

@NiagaraType
@NiagaraProperty(name = "method",          type = "BString",   defaultValue = "BString.make(\"POST\")",
                 facets = "{@see \"Allowed: GET, POST, PUT, DELETE, PATCH\"}")
@NiagaraProperty(name = "url",             type = "BString",   defaultValue = "BString.make(\"https://localhost\")",
                 flags = Flags.SUMMARY)
@NiagaraProperty(name = "headers",         type = "BString",   defaultValue = "BString.make(\"\")",
                 facets = "{@see \"Header lines 'Key: Value' separated by \\n\"}")
@NiagaraProperty(name = "contentType",     type = "BString",   defaultValue = "BString.make(\"application/json\")")
@NiagaraProperty(name = "body",            type = "BString",   defaultValue = "BString.make(\"\")")
@NiagaraProperty(name = "username",        type = "BString",   defaultValue = "BString.make(\"\")")
@NiagaraProperty(name = "password",        type = "BPassword", defaultValue = "BPassword.DEFAULT")
@NiagaraProperty(name = "verifyTls",       type = "BBoolean",  defaultValue = "BBoolean.TRUE")
@NiagaraProperty(name = "followRedirects", type = "BBoolean",  defaultValue = "BBoolean.TRUE")
public class BHttpRequest extends BComponent
{
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BHttpRequest.class);
}
