package com.kyanro.frpdemo.service.api.github;

import com.kyanro.frpdemo.models.api.github.GithubUser;

import java.util.List;

import retrofit.http.GET;
import rx.Observable;

/**
 * Created by ppp on 2015/01/12.
 */
public interface GithubService  {
    @GET("/users")
    Observable<List<GithubUser>> listUsers();
}
