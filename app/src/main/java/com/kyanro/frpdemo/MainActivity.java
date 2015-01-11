package com.kyanro.frpdemo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.kyanro.frpdemo.models.api.github.GithubUser;
import com.kyanro.frpdemo.service.api.github.GithubService;

import java.util.List;

import retrofit.RestAdapter;
import retrofit.RestAdapter.Builder;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 手動で作る場合ならこっちだが、今回はretrofitを利用するのでつかわない
        //Observable<String> requestStream = Observable.just("https://api.github.com/users");

        RestAdapter restAdapter = new Builder().setEndpoint("https://api.github.com").build();
        GithubService service = restAdapter.create(GithubService.class);

        Observable<List<GithubUser>> responseStream = service.listUsers();

        // debug
        responseStream
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    Toast.makeText(MainActivity.this, list.get(0).login, Toast.LENGTH_LONG).show();
                    Log.d("myrx", "login name:" + list.get(0).login);
                }, throwable -> Log.d("myrx", "error:" + throwable.getMessage()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
