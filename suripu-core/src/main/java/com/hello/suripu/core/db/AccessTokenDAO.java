package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.binders.BindAccessToken;
import com.hello.suripu.core.db.mappers.AccessTokenMapper;
import com.hello.suripu.core.oauth.AccessToken;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

import java.util.UUID;


@RegisterMapper(AccessTokenMapper.class)
public interface AccessTokenDAO {

    @SingleValueResult(AccessToken.class)
    @SqlQuery("SELECT * FROM oauth_tokens WHERE access_token = cast(:access_token as uuid)")
    Optional<AccessToken> getByAccessToken(@Bind("access_token") UUID accessToken);

    @SingleValueResult(AccessToken.class)
    @SqlQuery("SELECT * FROM oauth_tokens WHERE refresh_token = :refresh_token")
    Optional<AccessToken> getByRefreshToken(@Bind("refresh_token") UUID accessToken);

    @SqlUpdate("INSERT INTO oauth_tokens (access_token, refresh_token, expires_in, refresh_expires_in, app_id, account_id, scopes) VALUES (:access_token, :refresh_token, :expires_in, :refresh_expires_in, :app_id, :account_id, :scopes);")
    void storeAccessToken(@BindAccessToken AccessToken accessToken);

    @SqlUpdate("UPDATE oauth_tokens SET expires_in=0 WHERE access_token = cast(:access_token as uuid);")
    void disable(@Bind("access_token") UUID accessToken);

    @SqlUpdate("UPDATE oauth_tokens SET expires_in=0,refresh_expires_in=0 WHERE refresh_token = cast(:refresh_token as uuid);")
    void disableByRefreshToken(@Bind("refresh_token") UUID refreshToken);
}
