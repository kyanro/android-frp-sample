package com.kyanro.frpdemo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kyanro.frpdemo.models.api.github.GithubUser;
import com.kyanro.frpdemo.service.api.github.GithubService;

import java.util.List;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.RestAdapter;
import retrofit.RestAdapter.Builder;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.view.OnClickEvent;
import rx.android.view.ViewObservable;
import rx.functions.Action1;
import rx.functions.Func1;


public class MainActivity extends ActionBarActivity {

    @InjectView(R.id.refresh_button)
    View mRefreshView;
    @InjectView(R.id.user1_text)
    TextView mUser1Text;
    @InjectView(R.id.user2_text)
    TextView mUser2Text;
    @InjectView(R.id.user3_text)
    TextView mUser3Text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // api kickの準備
        RestAdapter restAdapter = new Builder().setEndpoint("https://api.github.com").build();
        GithubService service = restAdapter.create(GithubService.class);


        // ボタンクリック を stream へ
        Observable<OnClickEvent> refreshStream = ViewObservable.clicks(mRefreshView);

        // https://api.github.com/users の observable


        Observable<List<GithubUser>> responseStream = refreshStream
                .map(new Func1<OnClickEvent, Integer>() {
                    @Override
                    public Integer call(OnClickEvent onClickEvent) {
                        return new Random().nextInt(500);

                    }
                })
                .flatMap(new Func1<Integer, Observable<List<GithubUser>>>() {
                    @Override
                    public Observable<List<GithubUser>> call(Integer since) {
                        return service.listUsers(since);
                    }
                });

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
