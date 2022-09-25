package com.oslash.integration.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.services.people.v1.model.Person;
import com.google.common.base.Preconditions;
import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.manager.config.ManagerConfiguration;
import com.oslash.integration.models.User;
import io.micrometer.core.instrument.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.oslash.integration.resolver.IntegrationResolver.integrationResolver;


/**
 * TODO: Add support to fetch auth code by email and refresh token
 * and initiate job for logged in users
 * - session handling
 */
@Profile("manager")
@Controller
public class UserController {

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    AppConfiguration appConfiguration;

    @Autowired
    ManagerConfiguration manager;

    @GetMapping(value = {"/"})
    public void landingPage(HttpServletResponse response) throws Exception {
        response.sendRedirect("/signup");
    }

    @GetMapping(value = {"/signup"})
    public void signUp(HttpServletResponse response) throws Exception {
        GoogleAuthorizationCodeRequestUrl url = integrationResolver().authorizationCodeFlow().newAuthorizationUrl();
        String redirectURL = url.setRedirectUri(integrationResolver().callBackUrl()).setAccessType(integrationResolver().accessType()).build();
        response.sendRedirect(redirectURL);
    }

    @GetMapping(value = {"/oauth"})
    public @ResponseBody Person oAuthCallback(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String authCode = request.getParameter("code");
        Preconditions.checkArgument(StringUtils.isNotEmpty(authCode), "Invalid auth code");
        GoogleTokenResponse tokenResponse = integrationResolver().authorizationCodeFlow().newTokenRequest(authCode).setRedirectUri(integrationResolver().callBackUrl()).execute();
        Person person = integrationResolver().getPerson(tokenResponse);
        User userDetails = integrationResolver().saveUserDetails(tokenResponse, person);
        manager.scheduleJobForUser(userDetails);
        return person;
    }

}
