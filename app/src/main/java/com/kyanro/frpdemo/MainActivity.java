package com.kyanro.frpdemo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.kyanro.frpdemo.models.api.github.GithubUser;
import com.kyanro.frpdemo.service.api.github.GithubService;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.RestAdapter;
import retrofit.RestAdapter.Builder;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.view.OnClickEvent;
import rx.android.view.ViewObservable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;


public class MainActivity extends ActionBarActivity {

    @InjectView(R.id.refresh_button)
    View mRefreshView;
    @InjectView(R.id.user1_text)
    TextView mUser1Text;
    @InjectView(R.id.user2_text)
    TextView mUser2Text;
    @InjectView(R.id.user3_text)
    TextView mUser3Text;
    private Observable<GithubUser> mSuggestion2Stream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // api kickの準備
        RestAdapter restAdapter = new Builder().setEndpoint("https://api.github.com").build();
        GithubService service = restAdapter.create(GithubService.class);


        // ボタンクリック を stream へ. stream開始時に clickイベント実行
        Observable<OnClickEvent> refreshClickStream = ViewObservable.clicks(mRefreshView, true);

        // text view のクリックで自身を更新するために streamへ
        Observable<OnClickEvent> close1ClickStream = ViewObservable.clicks(mUser1Text, true);
        Observable<List<OnClickEvent>> close1BufferdClickStream = close1ClickStream.buffer(2, TimeUnit.SECONDS);
        Observable<Integer> close1Stream = close1BufferdClickStream.map(List::size).filter(count -> count > 1);
        //Observable<OnClickEvent> close2ClickStream = ViewObservable.clicks(mUser2Text, true);
        Observable<View> close2Stream = Observable.create(subscriber -> {
            mUser2Text.setOnClickListener(v -> {
                if (subscriber.isUnsubscribed()) {
                    return;
                }
                subscriber.onError(new Throwable("close2 error"));
            });
            mUser2Text.setOnLongClickListener(v -> {
                updateUser2();
                if (subscriber.isUnsubscribed()) {
                    return true;
                }
                subscriber.onNext(v);
                subscriber.onCompleted();
                return true;
            });
        });

        Observable<OnClickEvent> close3ClickStream = ViewObservable.clicks(mUser3Text, true);

        // https://api.github.com/users の observable
        Observable<List<GithubUser>> responseStream = refreshClickStream
                .map(onClickEvent -> new Random().nextInt(500))
                .flatMap(service::listUsers);

        // 推薦1ユーザ用stream
        Observable<GithubUser> suggestion1Stream = Observable.combineLatest(
                close1Stream, responseStream, (onClickEvent, githubUsers) ->
                        githubUsers.get(new Random().nextInt(githubUsers.size())))
                .mergeWith(refreshClickStream.map(onClickEvent -> null));
        // 推薦2ユーザ用stream
        mSuggestion2Stream = Observable.combineLatest(
                close2Stream, responseStream, (onClickEvent, githubUsers) ->
                        githubUsers.get(new Random().nextInt(githubUsers.size())))
                .mergeWith(refreshClickStream.map(onClickEvent -> null))
                .observeOn(AndroidSchedulers.mainThread());
        // 推薦3ユーザ用stream
        Observable<GithubUser> suggestion3Stream = Observable.combineLatest(
                close3ClickStream, responseStream, (onClickEvent, githubUsers) ->
                        githubUsers.get(new Random().nextInt(githubUsers.size())))
                .mergeWith(refreshClickStream.map(onClickEvent -> null));


        // subscribe
        mPreparedSuggestion1UserStream = suggestion1Stream
                .flatMap(githubUser -> Observable.create((Subscriber<? super GithubUser> subscriber) -> {
                    subscriber.onError(new Throwable("force error"));
                }))
                .onErrorResumeNext(throwable -> {
                    Log.d("myrx", "s1 error resume:" + throwable.getMessage());
                    return Observable.just(null);
                })
                .observeOn(AndroidSchedulers.mainThread());
        mPreparedSuggestion1UserStream.subscribe(
                githubUser -> {
                    if (githubUser == null) {
                        mUser1Text.setText("Refreshing...");
                    } else {
                        mUser1Text.setText(githubUser.login);
                    }
                }
                , throwable -> Log.d("myrx", "s1 error:" + throwable.getMessage())
                , () -> Log.d("myrx", "s1 complete"));


        updateUser2();

        suggestion3Stream
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(githubUser -> {
                    if (githubUser == null) {
                        mUser3Text.setText("Refreshing...");
                    } else {
                        mUser3Text.setText(githubUser.login);
                    }
                });


    }

    private void updateUser2() {
        mSuggestion2Stream
                .subscribe(
                githubUser -> {
                    if (githubUser == null) {
                        mUser2Text.setText("Refreshing...");
                    } else {
                        mUser2Text.setText(githubUser.login);
                    }
                }
                , throwable -> Log.d("myrx", "s2 error:" + throwable.getMessage())
                , () -> Log.d("myrx", "s2 complete"));
    }

    Observable<GithubUser> mPreparedSuggestion1UserStream;

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
