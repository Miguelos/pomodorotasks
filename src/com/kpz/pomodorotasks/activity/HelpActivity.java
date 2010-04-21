package com.kpz.pomodorotasks.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class HelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.help);
    	
    	initBackButton();
    	
    	TextView visitText = (TextView)findViewById(R.id.visitText);
    	Linkify.addLinks(visitText, Linkify.WEB_URLS);
    }

	private void initBackButton() {
		Button revertButton = (Button) findViewById(R.id.back);
    	revertButton.setOnClickListener(new View.OnClickListener() {

    	    public void onClick(View view) {
    	        setResult(RESULT_OK);
    	        finish();
    	    }
    	});
	}
}
