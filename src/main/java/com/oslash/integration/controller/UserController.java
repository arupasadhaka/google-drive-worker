package com.oslash.integration.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.services.people.v1.model.Person;
import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.oslash.integration.resolver.GoogleApiResolver.apiResolver;


// TODO: add support to fetch auth code by email, and initiate job for logged in users
@Controller
public class UserController {
    @GetMapping(value = {"/"})
    public void landingPage(HttpServletResponse response) throws Exception {
        response.sendRedirect("/signup");
    }

    @GetMapping(value = {"/signup"})
    public void signUp(HttpServletResponse response) throws Exception {
        GoogleAuthorizationCodeRequestUrl url = apiResolver().authorizationCodeFlow().newAuthorizationUrl();
        String redirectURL = url.setRedirectUri(apiResolver().callBackUrl()).setAccessType(apiResolver().accessType()).build();
        response.sendRedirect(redirectURL);
    }

    @GetMapping(value = {"/oauth"})
    public @ResponseBody Person oAuthCallback(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String authCode = request.getParameter("code");
        Preconditions.checkArgument(StringUtils.isNotEmpty(authCode), "Invalid auth code");
        GoogleTokenResponse tokenResponse = apiResolver().authorizationCodeFlow().newTokenRequest(authCode).setRedirectUri(apiResolver().callBackUrl()).execute();
        Person result = apiResolver().saveUserDetails(tokenResponse);
        return result;
    }
}
