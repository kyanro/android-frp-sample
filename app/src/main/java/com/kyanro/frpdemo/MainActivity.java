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
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.RestAdapter;
import retrofit.RestAdapter.Builder;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.view.OnClickEvent;
import rx.android.view.ViewObservable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;


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


        // ボタンクリック を stream へ. stream開始時に clickイベント実行
        Observable<OnClickEvent> refreshClickStream = ViewObservable.clicks(mRefreshView, true);

        // text view のクリックで自身を更新するために streamへ
        Observable<OnClickEvent> close1ClickStream = ViewObservable.clicks(mUser1Text, true);
        Observable<OnClickEvent> close2ClickStream = ViewObservable.clicks(mUser2Text, true);
        Observable<OnClickEvent> close3ClickStream = ViewObservable.clicks(mUser3Text, true);

        // https://api.github.com/users の observable
        Observable<List<GithubUser>> responseStream = refreshClickStream
                .map(onClickEvent -> new Random().nextInt(500))
                .flatMap(service::listUsers);

        // 推薦1ユーザ用stream
        Observable<GithubUser> suggestion1Stream = Observable.combineLatest(
                close1ClickStream, responseStream, (onClickEvent, githubUsers) -> githubUsers.get(new Random().nextInt(githubUsers.size())))
                .mergeWith(refreshClickStream.map(onClickEvent -> null));
        // 推薦2ユーザ用stream
        Observable<GithubUser> suggestion2Stream = Observable.combineLatest(
                close2ClickStream, responseStream, (onClickEvent, githubUsers) ->
                        githubUsers.get(new Random().nextInt(githubUsers.size())))
                .mergeWith(refreshClickStream.map(onClickEvent -> null));
        // 推薦3ユーザ用stream
        Observable<GithubUser> suggestion3Stream = Observable.combineLatest(
                close3ClickStream, responseStream, (onClickEvent, githubUsers) ->
                        githubUsers.get(new Random().nextInt(githubUsers.size())))
                .mergeWith(refreshClickStream.map(onClickEvent -> null));

        // subscribe
        //Subscription subscription1 = updateUser(suggestion1Stream, mUser1Text);
        //Subscription subscription2 = updateUser(suggestion2Stream, mUser2Text);
        //Subscription subscription3 = updateUser(suggestion3Stream, mUser3Text);

        Log.d("myrx", "subject1 subscribe check");

        

        PublishSubject<GithubUser> subject1 = PublishSubject.create();
        subject1
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(githubUser -> {
                    if (githubUser == null) {
                        mUser1Text.setText("Refreshing...");
                    } else {
                        mUser1Text.setText(githubUser.login);
                    }
                }, e -> Log.d("myrx", "error handring:" + e.getMessage()));
        suggestion1Stream
                .flatMap(githubUser -> Observable.<GithubUser>error(new Throwable("test")))
                        //.flatMap(githubUser -> Observable.create((Subscriber<? super GithubUser> subscriber) -> subscriber.onError(new Throwable("force error"))))
                        //.onErrorResumeNext(throwable -> {
                        //    Log.d("myrx", "s1 error resume:" + throwable.getMessage());
                        //    return Observable.just(null);
                        //})
                .subscribe(subject1);

    }

    private Subscription updateUser(Observable<GithubUser> suggestionStream, TextView userText) {
        return suggestionStream
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(githubUser -> {
                    if (githubUser == null) {
                        userText.setText("Refreshing...");
                    } else {
                        userText.setText(githubUser.login);
                    }
                }, e -> Log.d("myrx", "error handring:" + e.getMessage()));
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
