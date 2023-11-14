package com.dbon.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@RestController
@RequestMapping("/kclient")
public class KClientController {

    Logger logger = LoggerFactory.getLogger(KClientController.class);

    @Value("${request.serverHost:}")
    String serverHost;

    @Value("${request.updateUrl:}")
    String updateStateURL;

    @Value("${request.readUrl:}")
    String readStateURL;

    @Autowired
    RestTemplate restTemplate;

    @PostMapping("/kstate/{sessionId}/update")
    public void update(@PathVariable(value="sessionId") final String sessionId, @RequestBody String input) {

        logger.info("Updating... [{}]: [{}]", sessionId, input);

        HttpEntity<String> entity = new HttpEntity<String>(input);

        try {
            String url = updateStateURL.replace("{}", findServerHostName(sessionId));

            restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
            logger.info("Success [{}]: [{}]", sessionId, input);
        }
        catch (Exception e) {
            logger.error("Failure when call kstate[{}]...", sessionId, e);
            throw new RestClientException("Failure when call kstate!", e);
        }
    }

    @PostMapping("/kstate/{sessionId}/read")
    public String read(@PathVariable(value="sessionId") final String sessionId) {

        logger.info("Reading... [{}]", sessionId);

        HttpEntity<String> entity = new HttpEntity<String>("");

        try {
            String url = readStateURL.replace("{}", findServerHostName(sessionId));

            String response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
            logger.info("Success [{}]: [{}]", sessionId, response);
            return response;
        }
        catch (Exception e) {
            logger.error("Failure when call kstate[{}]...", sessionId, e);
            throw new RestClientException("Failure when call kstate!", e);
        }
    }

    private String findServerHostName(String sessionId) throws UnknownHostException {

        String host = null;
        InetAddress results[] = InetAddress.getAllByName(serverHost);

        for (int i=0; i<results.length; i++) {

            // show the Internet Address as name/address
            logger.info("DNS: {}", results[i].getHostName() + "/" + results[i].getHostAddress());

            String hostName = getHostNameByIP(results[i].getHostAddress());
            if(hostName.contains("-"+sessionId+".")) {
                host = hostName;
                break;
            }
        }

        return host;
    }

    private String getHostNameByIP(final String ip)
    {
        String hostName = null;
        String[] ipBytes = ip.split("\\.");
        if (ipBytes.length == 4)
        {
            try
            {
                final Hashtable<String, String> env = new Hashtable<>();
                env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

                DirContext ctx = new InitialDirContext(env);
                String searchBy = ipBytes[3] + "." + ipBytes[2] + "." + ipBytes[1] + "." + ipBytes[0] + ".in-addr.arpa";
                Attributes attrs = ctx.getAttributes(searchBy, new String[]{"PTR",});

                for (final NamingEnumeration<? extends Attribute> en = attrs.getAll(); en.hasMoreElements();)
                {
                    Attribute attr = en.next();
                    String attrId = attr.getID();
                    for (Enumeration<?> attrVals = attr.getAll(); attrVals.hasMoreElements();)
                    {
                        String value = attrVals.nextElement().toString();
                        logger.info("{}: found attribute {}: {}", ip, attrId, value);

                        if ("PTR".equals(attrId))
                        {
                            int len = value.length();
                            if (value.charAt(len - 1) == '.')
                            {
                                value = value.substring(0, len - 1);    //  remove trailing dot
                            }
                            hostName = value;
                        }
                    }
                }

                ctx.close();
            }
            catch (NamingException e) {
                logger.error("{}: no hostname found, we will try with InetAddress...", ip, e);
            }
        }

        if (hostName == null)
        {
            try
            {
                logger.info("{}: trying with InetAddress...", ip);
                hostName = InetAddress.getByName(ip).getCanonicalHostName();
            }
            catch (UnknownHostException e1) {
                logger.info("{}: fail back to IP", ip);
                hostName = ip;
            }
        }

        return hostName;
    }
}