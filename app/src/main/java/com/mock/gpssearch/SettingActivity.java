package com.mock.gpssearch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import static com.mock.gpssearch.MockLocationImpl.accuracy_value;

public class SettingActivity extends AppCompatActivity {

    private Button btn_save;
    private EditText et_accuracy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        initToolbar();

        init_view();
    }

    private void initToolbar(){
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");
    }

    private void init_view(){
        et_accuracy = findViewById(R.id.et_accuracy);
        btn_save = findViewById(R.id.save_button);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        float saved_accuracy = pref.getFloat("Accuracy", 0);

        int et_accu = (int)Math.round(saved_accuracy);
        et_accuracy.setText(String.valueOf(et_accu));

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String st_accu = et_accuracy.getText().toString();
                float ft_accu = Float.parseFloat(st_accu);

                editor.putFloat("Accuracy", ft_accu);
                accuracy_value = ft_accu;
                editor.apply();

                Toast.makeText(getApplicationContext(), "Saved successfully!" , Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}