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
    private String[] datfiles = {"art", "ascii-art", "computers", "definitions", "education", "food", "kids", "humorists", "linux",
    		"linuxcookie", "literature", "love", "magic", "medicine", "men-women", "news", "paradoxum", "people", "pets",
    		"platitudes", "riddles", "science", "sports", "startrek", "wisdom", "work", "zippy"};
    private HashMap<String, ArrayList<String>> fortunesdict = null;
    
    private void InitFortunes(){
    	this.fortunesdict = new HashMap<String, ArrayList<String>>();
    	for (String datfile : datfiles) {
    		fortunesdict.put(datfile, new ArrayList<String>());
    		try {
	    		InputStream is = new ClassPathResource("datfiles/" + datfile).getInputStream();
	    		Scanner inscanner = new Scanner(is).useDelimiter("\\n%\\n");
	    		while (inscanner.hasNext()) {
	    			String token = inscanner.next();
	    			fortunesdict.get(datfile).add(token);
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
        return getRandomFortune(null);
    }

    @RequestMapping(value = "webhook", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity webhookCallback(@RequestHeader(X_OUTBOUND_TOKEN) String outboundToken, @RequestBody WebhookEvent webhookEvent){
        if(VERIFICATION.equalsIgnoreCase(webhookEvent.getType()) && authService.isValidVerificationRequest(webhookEvent, outboundToken)) {
            return buildVerificationResponse(webhookEvent);
        }

        if(!workspaceProperties.getAppId().equals(webhookEvent.getUserId())) {
            // respond to webhook
        	String in = webhookEvent.getContent();
        	if (!in.startsWith("@fortunebot")) {
        		return ResponseEntity.ok().build();
        	}
            String fortune = getRandomFortune(null);
        	if (in.contains(" categories")) {
        		fortune = getCategories();
        	}
        	if (in.contains(" -h") || in.contains(" help")) {
        		fortune = getHelpString();
        	}
        	for (int i = 0; i < datfiles.length; i++) {
        		if (in.contains(datfiles[i])) {
        			fortune = getRandomFortune(datfiles[i]);
        		}
        	}
        	workspaceService.createMessage(webhookEvent.getSpaceId(), buildMessage("FortuneBot", fortune));	
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

    private String getRandomFortune(String category){
    	if (this.fortunesdict == null) {
    		this.InitFortunes();
    	}
    	ArrayList<String> target = null;
    	if (category != null) {
    		target = fortunesdict.get(category);
    	} 
    	else {
    		List<String> keys = new ArrayList<String>(fortunesdict.keySet());
    		String rkey = datfiles[random.nextInt(keys.size())];
    		target = fortunesdict.get(rkey);
    	}
    	return target.get(random.nextInt(target.size()));
    }
    
    private String getCategories(){
    	String ret = "";
    	for (int i = 0; i < datfiles.length; i++) {
    		ret += datfiles[i] + " ";
    	}
    	return ret;
    }
    
    private String getHelpString(){
    	return "Usage:\n @fortunebot [-h] [help] [categories] <category>";
    }
}
