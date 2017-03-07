package com.workspace.controller;

import com.workspace.WorkspaceProperties;
import com.workspace.model.WebhookEvent;
import com.workspace.service.AuthService;
import com.workspace.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ClassPathResource;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;

import static com.workspace.WorkspaceConstants.VERIFICATION;
import static com.workspace.WorkspaceConstants.X_OUTBOUND_TOKEN;
import static com.workspace.utils.MessageUtils.buildMessage;

@RestController
public class WorkspaceController {
	protected static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceController.class);
	private ArrayList<String> fortunes = null;
    private Random random = new Random();
    private String[] datfiles = {"art", "computers", "definitions", "education", "food", "kids", "humorists", "linux",
    		"linuxcookie", "literature", "love", "magic", "medicine", "men-women", "news", "paradoxum", "people", "pets",
    		"platitudes", "riddles", "science", "sports", "startrek", "wisdom", "work", "zippy"};
    
    private void InitFortunes(){
    	this.fortunes = new ArrayList<String>();
    	for (String datfile : datfiles) {
    		try {
	    		InputStream is = new ClassPathResource("datfiles/" + datfile).getInputStream();
	    		Scanner inscanner = new Scanner(is).useDelimiter("%");
	    		while (inscanner.hasNext()) {
	    			String token = inscanner.next();
	    			fortunes.add(token);
	    		}
	    		inscanner.close();
	    	} catch (Exception e) {
	    		LOGGER.error("Exception: " + e.toString());
	    		continue;
	    	}
    	}	
    }


    @Autowired
    private WorkspaceProperties workspaceProperties;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private AuthService authService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String home(){
        return getRandomFortune();
    }

    @RequestMapping(value = "webhook", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity webhookCallback(@RequestHeader(X_OUTBOUND_TOKEN) String outboundToken, @RequestBody WebhookEvent webhookEvent){
        if(VERIFICATION.equalsIgnoreCase(webhookEvent.getType()) && authService.isValidVerificationRequest(webhookEvent, outboundToken)) {
            return buildVerificationResponse(webhookEvent);
        }

        if(!workspaceProperties.getAppId().equals(webhookEvent.getUserId())) {
            // respond to webhook
        	String in = webhookEvent.getContent();
        	if (in.startsWith("@fortunebot")) {
        		String fortune = getRandomFortune();
        		workspaceService.createMessage(webhookEvent.getSpaceId(), buildMessage("FortuneBot", fortune));
        	}
        }
        return ResponseEntity.ok().build();
    }

    private ResponseEntity buildVerificationResponse(WebhookEvent webhookEvent) {
        String responseBody = String.format("{\"response\": \"%s\"}", webhookEvent.getChallenge());

        String verificationHeader = authService.createVerificationHeader(responseBody);
        return ResponseEntity.status(HttpStatus.OK)
                .header(X_OUTBOUND_TOKEN, verificationHeader)
                .body(responseBody);
    }

    private String getRandomFortune(){
    	if (this.fortunes == null) {
    		this.InitFortunes();
    	}
    	int randomIndex = this.random.nextInt(this.fortunes.size());
    	return fortunes.get(randomIndex);
    }
}
