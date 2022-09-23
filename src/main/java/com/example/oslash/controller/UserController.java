package com.example.oslash.controller;

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

import static com.example.oslash.manager.GoogleApiManager.apiManager;


@Controller
public class UserController {

    @GetMapping(value = {"/"})
    public void landingPage(HttpServletResponse response) throws Exception {
        response.sendRedirect("/signup");
    }

    @GetMapping(value = {"/signup"})
    public void signUp(HttpServletResponse response) throws Exception {
        GoogleAuthorizationCodeRequestUrl url = apiManager().authorizationCodeFlow().newAuthorizationUrl();
        String redirectURL = url.setRedirectUri(apiManager().callBackUrl()).setAccessType(apiManager().accessType()).build();
        response.sendRedirect(redirectURL);
    }

    @GetMapping(value = {"/oauth"})
    public @ResponseBody Person oAuthCallback(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String code = request.getParameter("code");
        Preconditions.checkArgument(StringUtils.isNotEmpty(code), "Invalid auth code");
        GoogleTokenResponse tokenResponse = apiManager().authorizationCodeFlow().newTokenRequest(code).setRedirectUri(apiManager().callBackUrl()).execute();
        Person result = apiManager().saveUserDetails(tokenResponse);
        return result;
    }
}
