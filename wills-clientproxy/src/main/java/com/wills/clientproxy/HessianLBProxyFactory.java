package com.wills.clientproxy;

import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianHttpConnectionFactory;
import com.caucho.hessian.client.HessianMetaInfoAPI;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.client.HessianURLConnectionFactory;
import com.caucho.hessian.io.*;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.logging.Logger;
 
/**
 * 修改自 HessianLBProxyFactory，实现调用过程中的LB
 * 
 * @author huangsiping
 *
 */
public class HessianLBProxyFactory  implements HessianProxyFactory {
  protected static Logger log
    = Logger.getLogger(HessianLBProxyFactory.class.getName());

  private ClassLoader _loader;
  
  private SerializerFactory _serializerFactory;

  private HessianConnectionFactory _connFactory;
  
  private HessianRemoteResolver _resolver;

  private String _user;
  private String _password;
  private String _basicAuth;

  private boolean _isOverloadEnabled = false;

  private boolean _isHessian2Reply = true;
  private boolean _isHessian2Request = false;

  private boolean _isChunkedPost = true;
  private boolean _isDebug = false;

  private long _readTimeout = -1;
  private long _connectTimeout = -1;
  
  private static HessianLBProxyFactory instance = new HessianLBProxyFactory();

  public static HessianLBProxyFactory getInstance() {
		return instance;
	}
  
  /**
   * Creates the new proxy factory.
   */
  public HessianLBProxyFactory()
  {
	  HessianLBProxyFactory(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates the new proxy factory.
   */
  private void HessianLBProxyFactory(ClassLoader loader)
  {
    _loader = loader;
    _resolver = new HessianLBProxyResolver(this);
  }

  /**
   * Sets the user.
   */
  public void setUser(String user)
  {
    _user = user;
    _basicAuth = null;
  }

  /**
   * Sets the password.
   */
  public void setPassword(String password)
  {
    _password = password;
    _basicAuth = null;
  }

  public String getBasicAuth()
  {
    if (_basicAuth != null)
      return _basicAuth;

    else if (_user != null && _password != null)
      return "Basic " + base64(_user + ":" + _password);

    else
      return null;
  }

  /**
   * Sets the connection factory to use when connecting
   * to the Hessian service.
   */
  public void setConnectionFactory(HessianConnectionFactory factory)
  {
    _connFactory = factory;
  }

  /**
   * Returns the connection factory to be used for the HTTP request.
   */
  public HessianConnectionFactory getConnectionFactory()
  {
    if (_connFactory == null) {
      _connFactory = createHessianConnectionFactory();
      _connFactory.setHessianProxyFactory(this);
    }
    
    return _connFactory;
  }

  /**
   * Sets the debug
   */
  public void setDebug(boolean isDebug)
  {
    _isDebug = isDebug;
  }

  /**
   * Gets the debug
   */
  public boolean isDebug()
  {
    return _isDebug;
  }

  /**
   * Returns true if overloaded methods are allowed (using mangling)
   */
  public boolean isOverloadEnabled()
  {
    return _isOverloadEnabled;
  }

  /**
   * set true if overloaded methods are allowed (using mangling)
   */
  public void setOverloadEnabled(boolean isOverloadEnabled)
  {
    _isOverloadEnabled = isOverloadEnabled;
  }

  /**
   * Set true if should use chunked encoding on the request.
   */
  public void setChunkedPost(boolean isChunked)
  {
    _isChunkedPost = isChunked;
  }

  /**
   * Set true if should use chunked encoding on the request.
   */
  public boolean isChunkedPost()
  {
    return _isChunkedPost;
  }

  /**
   * The socket timeout on requests in milliseconds.
   */
  public long getReadTimeout()
  {
    return _readTimeout;
  }

  /**
   * The socket timeout on requests in milliseconds.
   */
  public void setReadTimeout(long timeout)
  {
    _readTimeout = timeout;
  }

  /**
   * The socket connection timeout in milliseconds.
   */
  public long getConnectTimeout()
  {
    return _connectTimeout;
  }

  /**
   * The socket connect timeout in milliseconds.
   */
  public void setConnectTimeout(long timeout)
  {
    _connectTimeout = timeout;
  }

  /**
   * True if the proxy can read Hessian 2 responses.
   */
  public void setHessian2Reply(boolean isHessian2)
  {
    _isHessian2Reply = isHessian2;
  }

  /**
   * True if the proxy should send Hessian 2 requests.
   */
  public void setHessian2Request(boolean isHessian2)
  {
    _isHessian2Request = isHessian2;

    if (isHessian2)
      _isHessian2Reply = true;
  }

  /**
   * Returns the remote resolver.
   */
  public HessianRemoteResolver getRemoteResolver()
  {
    return _resolver;
  }

  /**
   * Sets the serializer factory.
   */
  public void setSerializerFactory(SerializerFactory factory)
  {
    _serializerFactory = factory;
  }

  /**
   * Gets the serializer factory.
   */
  public SerializerFactory getSerializerFactory()
  {
    if (_serializerFactory == null)
      _serializerFactory = new SerializerFactory(_loader);

    return _serializerFactory;
  }

  protected HessianConnectionFactory createHessianConnectionFactory()
  {

    String className = HessianHttpConnectionFactory.class.getName();
    
    HessianConnectionFactory factory = null;
      
    try {
      if (className != null) {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
	Class<?> cl = Class.forName(className, false, loader);

	factory = (HessianConnectionFactory) cl.newInstance();

	return factory;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return new HessianURLConnectionFactory();
  }
  /**
   * Creates a new proxy with the specified URL.  The API class uses
   * the java.api.class value from _hessian_
   *
   * @param url the URL where the client object is located.
   *
   * @return a proxy to the object with the specified interface.
   */
  public Object create(String url)
    throws MalformedURLException, ClassNotFoundException
  {
    HessianMetaInfoAPI metaInfo;

    metaInfo = (HessianMetaInfoAPI) create(HessianMetaInfoAPI.class, url);

    String apiClassName =
      (String) metaInfo._hessian_getAttribute("java.api.class");

    if (apiClassName == null)
      throw new HessianRuntimeException(url + " has an unknown api.");

    Class<?> apiClass = Class.forName(apiClassName, false, _loader);

    return create(apiClass, url);
  }

 
  
  public Object create(Class api,ClusterNodeManager cm)
		    throws MalformedURLException
		  {
	  
	  
	    if (api == null)
	        throw new NullPointerException("api must not be null for HessianProxyFactory.create()");
	      InvocationHandler handler = null;
	     
	      handler = new HessianLBProxy(cm, this, api); 

	      return Proxy.newProxyInstance(_loader,
	                                    new Class[] { api,
	                                                  HessianRemoteObject.class },
	                                    handler);
	   
		  }
 

  /**
   * Creates a new proxy with the specified URL.  The returned object
   * is a proxy with the interface specified by api.
   *
   * <pre>
   * String url = "http://localhost:8080/ejb/hello");
   * HelloHome hello = (HelloHome) factory.create(HelloHome.class, url);
   * </pre>
   *
   * @param api the interface the proxy class needs to implement
   * @param url the URL where the client object is located.
   *
   * @return a proxy to the object with the specified interface.
   */


  public AbstractHessianInput getHessianInput(InputStream is)
  {
    return getHessian2Input(is);
  }

  public AbstractHessianInput getHessian1Input(InputStream is)
  {
    AbstractHessianInput in;

    if (_isDebug)
      is = new HessianDebugInputStream(is, new PrintWriter(System.out));

    in = new HessianInput(is);

    in.setRemoteResolver(getRemoteResolver());

    in.setSerializerFactory(getSerializerFactory());

    return in;
  }

  public AbstractHessianInput getHessian2Input(InputStream is)
  {
    AbstractHessianInput in;

    if (_isDebug)
      is = new HessianDebugInputStream(is, new PrintWriter(System.out));

    in = new Hessian2Input(is);

    in.setRemoteResolver(getRemoteResolver());

    in.setSerializerFactory(getSerializerFactory());

    return in;
  }

  public AbstractHessianOutput getHessianOutput(OutputStream os)
  {
    AbstractHessianOutput out;

    if (_isHessian2Request)
      out = new Hessian2Output(os);
    else {
      HessianOutput out1 = new HessianOutput(os);
      out = out1;

      if (_isHessian2Reply)
        out1.setVersion(2);
    }

    out.setSerializerFactory(getSerializerFactory());

    return out;
  }

  /**
   * JNDI object factory so the proxy can be used as a resource.
   */
  public Object getObjectInstance(Object obj, Name name,
                                  Context nameCtx, Hashtable<?,?> environment)
    throws Exception
  {
    Reference ref = (Reference) obj;

    String api = null;
    String url = null;
    String user = null;
    String password = null;

    for (int i = 0; i < ref.size(); i++) {
      RefAddr addr = ref.get(i);

      String type = addr.getType();
      String value = (String) addr.getContent();

      if (type.equals("type"))
        api = value;
      else if (type.equals("url"))
        url = value;
      else if (type.equals("user"))
        setUser(value);
      else if (type.equals("password"))
        setPassword(value);
    }

    if (url == null)
      throw new NamingException("`url' must be configured for HessianProxyFactory.");
    // XXX: could use meta protocol to grab this
    if (api == null)
      throw new NamingException("`type' must be configured for HessianProxyFactory.");

    Class apiClass = Class.forName(api, false, _loader);

    return create(apiClass, url);
  }

  /**
   * Creates the Base64 value.
   */
  private String base64(String value)
  {
    StringBuffer cb = new StringBuffer();

    int i = 0;
    for (i = 0; i + 2 < value.length(); i += 3) {
      long chunk = (int) value.charAt(i);
      chunk = (chunk << 8) + (int) value.charAt(i + 1);
      chunk = (chunk << 8) + (int) value.charAt(i + 2);

      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append(encode(chunk >> 6));
      cb.append(encode(chunk));
    }

    if (i + 1 < value.length()) {
      long chunk = (int) value.charAt(i);
      chunk = (chunk << 8) + (int) value.charAt(i + 1);
      chunk <<= 8;

      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append(encode(chunk >> 6));
      cb.append('=');
    }
    else if (i < value.length()) {
      long chunk = (int) value.charAt(i);
      chunk <<= 16;

      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append('=');
      cb.append('=');
    }

    return cb.toString();
  }

  public static char encode(long d)
  {
    d &= 0x3f;
    if (d < 26)
      return (char) (d + 'A');
    else if (d < 52)
      return (char) (d + 'a' - 26);
    else if (d < 62)
      return (char) (d + '0' - 52);
    else if (d == 62)
      return '+';
    else
      return '/';
  }

@Override
public Object create(Class api, String url) throws MalformedURLException {
	// TODO Auto-generated method stub
	return null;
}


}

