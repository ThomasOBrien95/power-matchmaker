/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*MatchMaker.
 *
 * Power*MatchMaker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*MatchMaker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.matchmaker.dao.xml;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import ca.sqlpower.matchmaker.Project;
import ca.sqlpower.swingui.JDefaultButton;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

/**
 * An IO Handler that provides bidirectional communication with the SQL Power web site.
 */
public class WebsiteIOHandler implements IOHandler {

    private static final Logger logger = Logger.getLogger(WebsiteIOHandler.class);
    
    /**
     * Base URL where the SQL Power website lives. The various action paths will be appended
     * to this string to form the actual request URLs.
     */
    private static final String WEBSITE_BASE_URL = "http://localhost:9999/sqlpower_website/page/";

    private String sessionCookie = null;
    
    private String username = null;
    private String password = null;
    
    /**
     * The DAO that's using this IO Handler. Gets set when this handler is given
     * to the DAO.
     */
    private ProjectDAOXML dao;
    
    public List<Project> createProjectList() {
        try {
            while (!login(true)) {
                JOptionPane.showMessageDialog(null, "Couldn't retrieve projects", "Login failed", JOptionPane.WARNING_MESSAGE);
            }
            
            URL baseURL = new URL(WEBSITE_BASE_URL);
            URL url = new URL(baseURL, "get_pp_project_list");
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setRequestMethod("GET");
            urlc.setRequestProperty("Cookie", sessionCookie);
            urlc.setDoOutput(false);
            urlc.setDoInput(true);
            urlc.connect();

            Reader in = new InputStreamReader(urlc.getInputStream());
            StringBuilder responseString = new StringBuilder();
            char[] buf = new char[2000];
            while (in.read(buf) > 0) {
                responseString.append(buf);
            }
            in.close();
            urlc.disconnect();
            
            List<Project> projects = new ArrayList<Project>();
            Set<Long> oids = new HashSet<Long>();

            JSONArray response = new JSONArray(responseString.toString());
            for (int i = 0; i < response.length(); i++) {
                JSONObject pdesc = response.getJSONObject(i);
                
                long oid = pdesc.getLong("projectId");
                if (oids.contains(oid)) {
                    logger.error("Duplicate project from server!");
                } else {
                    String description = null;
                    if (!pdesc.isNull("projectDescription")) {
                        description = pdesc.getString("projectDescription");
                    }
                    Project p = new Project(oid, pdesc.getString("projectName"), description, dao);
                    projects.add(p);
                    oids.add(p.getOid());
                }
            }
            
            return projects;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OutputStream createOutputStream(Project project) {
        return new FancyOutputStream(project);
    }
    
    /**
     * A byte array output stream that sends its content to a remote HTTP server
     * once the output stream has been closed.
     */
    private class FancyOutputStream extends ByteArrayOutputStream {
        
        /**
         * The project that will be/has been written to this stream.
         */
        private final Project project;

        public FancyOutputStream(Project project) {
            this.project = project;
        }

        @Override
        public void close() throws IOException {
            super.close();
            
            try {
                if (!login(false)) {
                    JOptionPane.showMessageDialog(null, "Project not saved!", "Login failed", JOptionPane.WARNING_MESSAGE);
                    password = null;
                    return;
                }
                
                String xmlDoc = new String(toByteArray());
                StringBuilder sb = new StringBuilder();
                sb.append("projectXML=").append(URLEncoder.encode(xmlDoc, "UTF-8"));
                if (project.getOid() != null) {
                    sb.append("&projectId=").append(project.getOid());
                }
                sb.append("&projectName=").append(URLEncoder.encode(project.getName(), "UTF-8"));
                if (project.getDescription() != null) {
                    sb.append("&projectDescription=").append(URLEncoder.encode(project.getDescription(), "UTF-8"));
                }

                URL baseURL = new URL(WEBSITE_BASE_URL);
                URL url = new URL(baseURL, "save_pp_project");
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setRequestMethod("POST");
                urlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlc.setRequestProperty("content-length", String.valueOf(sb.length()));
                urlc.setRequestProperty("Cookie", sessionCookie);
                urlc.setDoOutput(true);
                urlc.setDoInput(true);
                urlc.connect();

                OutputStream out = urlc.getOutputStream();
                out.write(sb.toString().getBytes());
                out.flush();
                out.close();
                
                // have to read in order to send request!
                Reader in = new InputStreamReader(urlc.getInputStream());
                StringBuilder responseString = new StringBuilder();
                char[] buf = new char[2000];
                while (in.read(buf) > 0) {
                    responseString.append(buf);
                }
                in.close();
                urlc.disconnect();
                
                JSONObject response = new JSONObject(responseString.toString());
                boolean success = response.getBoolean("success");
                if (!success) {
                    throw new IOException("Failed to save: " + response.getString("message"));
                } else {
                    long oid = response.getLong("projectId");
                    project.setOid(oid);
                    logger.debug("Saved project " + project.getName() + " (oid=" + project.getOid() + ")");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private boolean login(boolean  alwaysShowPrompt) throws IOException {
        if (username == null || password == null || alwaysShowPrompt) {
            if (!showLoginPrompt()){
                return false;
            }
        }
        
        HttpURLConnection.setFollowRedirects(false);

        StringBuilder sb = new StringBuilder();
        sb.append("username=").append(URLEncoder.encode(username, "UTF-8"));
        sb.append("&password=").append(URLEncoder.encode(password, "UTF-8"));
        sb.append("&doLogin=").append(URLEncoder.encode("true", "UTF-8"));
        sb.append("&nextUri=THE_LOGIN_WORKED");

        URL baseURL = new URL(WEBSITE_BASE_URL);
        URL url = new URL(baseURL, "login");
        HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
        urlc.setRequestMethod("POST");
        urlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlc.setRequestProperty("content-length", String.valueOf(sb.length()));
        urlc.setDoOutput(true);
        urlc.setDoInput(true);
        urlc.connect();

        OutputStream out = urlc.getOutputStream();
        out.write(sb.toString().getBytes());
        out.flush();
        out.close();
        logger.debug("response headers: " + urlc.getHeaderFields());
        
        // have to read in order to send request!
        InputStream in = urlc.getInputStream();
        in.read();
        in.close();
        urlc.disconnect();

        
        String setCookie = urlc.getHeaderField("Set-Cookie");
        if (setCookie != null) {
            if (setCookie.indexOf(';') > 0) {
                sessionCookie = setCookie.substring(0, setCookie.indexOf(';'));
            } else {
                sessionCookie = setCookie;
            }
        } else {
            throw new IllegalStateException("Failed to log in (no set-cookie header found).");
        }
        
        // If the login worked, we will be redirected to the nextAction specified in the request
        String redirectLocation = urlc.getHeaderField("Location");
        if (redirectLocation != null && redirectLocation.contains("THE_LOGIN_WORKED")) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Breaks all the rules about MVC separation, and prompts the user to log in using a JDialog.
     * @return Whether or not the user accepted to login.
     */
    private boolean showLoginPrompt() {
        final JDialog d = new JDialog();
        final JTextField usernameField = new JTextField(username);
        final JPasswordField passwordField = new JPasswordField(password);
        JDefaultButton okButton = new JDefaultButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                username = usernameField.getText();
                password = new String(passwordField.getPassword());
                usernameField.putClientProperty("doLogin", Boolean.TRUE);
                d.dispose();
            }
            
        });
        
        cancelButton.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                d.dispose();
            }
            
        });
        
        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("pref,3dlu,30dlu:grow"));
        b.append("Username", usernameField);
        b.append("Password", passwordField);
        b.append(ButtonBarFactory.buildOKCancelBar(okButton,cancelButton), 3);
        b.setDefaultDialogBorder();
        passwordField.setPreferredSize(new Dimension(200, 20));
        
        d.setTitle("Please log in to the SQL Power Web Site");
        d.setModal(true);
        d.getRootPane().setDefaultButton(okButton);
        d.setContentPane(b.getPanel());
        d.pack();
        d.setLocationRelativeTo(null);
        d.setVisible(true);
        
        Boolean doLogin = (Boolean) usernameField.getClientProperty("doLogin");
        return doLogin != null && doLogin == true;
    }
    
    public void setDAO(ProjectDAOXML dao) {
        this.dao = dao;
    }

    public InputStream getInputStream(Project project) {
        try {
            if (!login(false)) {
                JOptionPane.showMessageDialog(null, "Project not updated!", "Login failed", JOptionPane.WARNING_MESSAGE);
                password = null;
                // If login failed, the next request will also fail and it will provide the appropriate error message
            }
            
            URL baseURL = new URL(WEBSITE_BASE_URL);
            URL url = new URL(baseURL, "load_pp_project?projectId="+project.getOid());
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setRequestMethod("GET");
            urlc.setRequestProperty("Cookie", sessionCookie);
            urlc.setDoOutput(false);
            urlc.setDoInput(true);
            urlc.connect();

            return urlc.getInputStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(Project project) {
        try {
            if (!login(false)) {
                JOptionPane.showMessageDialog(null, "Project not deleted!", "Login failed", JOptionPane.WARNING_MESSAGE);
                password = null;
                // If login failed, the next request will also fail and it will provide the appropriate error message
            }
            
            URL baseURL = new URL(WEBSITE_BASE_URL);
            URL url = new URL(baseURL, "delete_pp_project?projectId="+project.getOid());
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setRequestMethod("GET");
            urlc.setRequestProperty("Cookie", sessionCookie);
            urlc.setDoOutput(false);
            urlc.setDoInput(true);
            urlc.connect();

            // have to read in order to send request!
            Reader in = new InputStreamReader(urlc.getInputStream());
            StringBuilder responseString = new StringBuilder();
            char[] buf = new char[2000];
            while (in.read(buf) > 0) {
                responseString.append(buf);
            }
            in.close();
            urlc.disconnect();

            JSONObject response = new JSONObject(responseString.toString());
            boolean success = response.getBoolean("success");
            if (!success) {
                throw new IOException("Failed to delete: " + response.getString("message"));
            }             
        } catch (Exception e) {
            throw new RuntimeException(e);
        }        
    }
}
