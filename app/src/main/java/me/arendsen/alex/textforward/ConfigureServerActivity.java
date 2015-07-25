package me.arendsen.alex.textforward;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class ConfigureServerActivity extends ActionBarActivity {

    EditText etServerIP, etServerPort;
    Button bSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_server);

        etServerIP = (EditText) findViewById(R.id.serverAddressInput);
        etServerPort = (EditText) findViewById(R.id.serverPortInput);

        bSave = (Button) findViewById(R.id.serverSaveButton);
        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit(v);
            }
        });
    }

    public void onSubmit(View v) {
        Intent data = new Intent();
        String serverIP = etServerIP.getText().toString();
        int serverPort = Integer.parseInt(etServerPort.getText().toString());

        data.putExtra("serverIP",serverIP);
        data.putExtra("serverPort", serverPort);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_configure_server, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
