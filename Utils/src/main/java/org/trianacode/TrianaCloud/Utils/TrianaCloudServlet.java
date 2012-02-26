/*
 *
 *  * Copyright - TrianaCloud
 *  * Copyright (C) 2012. Kieran Evans. All Rights Reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.trianacode.TrianaCloud.Utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Harrison
 * @version 1.0.0 Jan 15, 2011
 */
public class TrianaCloudServlet extends HttpServlet {

    public static final String HTTP_BASIC_AUTH_USER = "http-basic-auth-user";
    public static final String HTTP_BASIC_AUTH_PASSWORD = "http-basic-auth-password";

    protected static Log log = LogFactory.getLog("org.sintero.server.SinteroServlet");


    public void writeDocument(HttpServletResponse response, Document doc) throws IOException {
        response.setStatus(200);
        response.setContentType("text/xml");
        response.setHeader("Transfer-Encoding", "chunked");
        DomPain.transform(doc, response.getOutputStream());
    }

    public void writeResponse(HttpServletResponse response, int status, String title, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/html");
        response.getWriter().println(createResponse(title, message));
        response.getWriter().flush();
    }

    public void writeOk(HttpServletResponse response, String title, String message) throws IOException {
        response.setStatus(200);
        response.setContentType("text/html");
        response.getWriter().println(createResponse(title, message));
        response.getWriter().flush();
    }

    public void writeCreated(HttpServletResponse response, String title, String message) throws IOException {
        response.setStatus(201);
        response.setContentType("text/html");
        response.getWriter().println(createResponse(title, message));
        response.getWriter().flush();
    }

    public void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/html");
        response.getWriter().println(createError(message));
        response.getWriter().flush();
        response.getWriter().close();
    }

    public void write400Error(HttpServletResponse response, String message) throws IOException {
        response.setStatus(400);
        response.setContentType("text/html");
        response.getWriter().println(createError(message));
        response.getWriter().flush();
        response.getWriter().close();
    }

    public void write404Error(HttpServletResponse response, String message) throws IOException {
        response.setStatus(404);
        response.setContentType("text/html");
        response.getWriter().println(createError(message));
        response.getWriter().flush();
        response.getWriter().close();
    }

    public void write500Error(HttpServletResponse response, String message) throws IOException {
        response.setStatus(500);
        response.setContentType("text/html");
        response.getWriter().println(createError(message));
        response.getWriter().flush();
        response.getWriter().close();
    }

    public void writeThrowable(HttpServletResponse response, Throwable t) throws IOException {
        write500Error(response, createStackTrace(t));
    }

    protected String createStackTrace(Throwable t) {
        StringBuffer stack = new StringBuffer("Message:" + t.getMessage() + "<br/>");
        StackTraceElement[] trace = t.getStackTrace();
        for (StackTraceElement element : trace) {
            stack.append(element.toString()).append("<br/>");
        }
        return stack.toString();
    }

    public byte[] read(HttpServletRequest req) throws IOException {
        InputStream in = req.getInputStream();
        byte[] bytes = new byte[4096];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int c;
        while ((c = in.read(bytes)) != -1) {
            bout.write(bytes, 0, c);
        }
        return bout.toByteArray();
    }

    public String readString(HttpServletRequest req, String encoding) throws IOException {
        byte[] bytes = read(req);
        return new String(bytes, encoding);
    }

    public String readString(HttpServletRequest req) throws IOException {
        return readString(req, "UTF-8");
    }

    public String isolatePath(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            return "";
        }
        while (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }
        while (pathInfo.endsWith("/")) {
            pathInfo = pathInfo.substring(0, pathInfo.length() - 1);
        }
        return pathInfo;
    }


    public Map<String, String> getQuery(String queryString) {
        Map<String, String> q = new HashMap<String, String>();
        if (queryString == null || queryString.length() == 0) {
            return q;
        }
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf("=");
            if (eq > 0 && eq < pair.length()) {
                try {
                    q.put(pair.substring(0, eq), URLDecoder.decode(pair.substring(eq + 1, pair.length()), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return q;
    }

    protected void setAuthenticationDetails(HttpServletRequest request, HttpServletResponse response, Map<String, String> map) throws HttpException {
        String authInfo = request.getHeader("Authorization");
        if (authInfo == null) {
            response.addHeader("WWW-Authenticate", "Basic Realm=\"/\"");
            throw new HttpException("Please authenticate", HttpServletResponse.SC_UNAUTHORIZED);
        }
        String basic = "basic ";
        int val = authInfo.toLowerCase().indexOf(basic);
        if (val != -1) {
            authInfo = authInfo.substring(basic.length()).trim();
        } else {
            throw new HttpException(HttpServletResponse.SC_UNAUTHORIZED);
        }
        String auth = new String(Base64.decode(authInfo));
        int colon = auth.indexOf(":");
        if (colon > -1) {
            String user = auth.substring(0, colon);
            String pass = auth.substring(colon + 1, auth.length());
            log.info(" user entered username=" + user);
            log.info(" user entered password=" + pass);
            map.put(HTTP_BASIC_AUTH_USER, user);
            map.put(HTTP_BASIC_AUTH_PASSWORD, pass);
        } else {
            throw new HttpException(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }


    private String createError(String error) {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
                "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>Error</title>\n" +
                "</head>\n" +
                "<body><p>\n" +
                error +
                "\n" +
                "</p></body>\n" +
                "</html>";
    }

    private String createResponse(String title, String resp) {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
                "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>" + title + "</title>\n" +
                "</head>\n" +
                "<body><p>\n" +
                resp +
                "\n" +
                "</p></body>\n" +
                "</html>";
    }
}