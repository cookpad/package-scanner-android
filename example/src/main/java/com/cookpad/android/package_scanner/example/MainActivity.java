package com.cookpad.android.package_scanner.example;

import com.cookpad.android.package_scanner.PackageScanner;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, PackageScanner.searchClasses(this, View.class)));
    }
}
