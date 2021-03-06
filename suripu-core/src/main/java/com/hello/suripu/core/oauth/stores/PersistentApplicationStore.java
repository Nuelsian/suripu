package com.hello.suripu.core.oauth.stores;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.util.SqlArray;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.OAuthScope;

import java.util.ArrayList;
import java.util.List;


public class PersistentApplicationStore implements ApplicationStore<Application, ApplicationRegistration>{

    private final ApplicationsDAO applicationsDAO;

    public PersistentApplicationStore(final ApplicationsDAO applicationsDAO) {
         this.applicationsDAO = applicationsDAO;
    }

    @Override
    public Optional<Application> getApplicationById(final Long applicationId) {
        final Optional<Application> applicationOptional = applicationsDAO.getById(applicationId);
        return applicationOptional;
    }

    @Override
    public Optional<Application> getApplicationByClientId(final String clientId) {
        return applicationsDAO.getByClientId(clientId);
    }

    @Override
    public Application register(final ApplicationRegistration registration) {
        final Long id = applicationsDAO.insertRegistration(registration);
        final Application application = Application.fromApplicationRegistration(registration, id);
        return application;
    }

    @Override
    public void activateForAccountId(final Application application, final Long accountId) {
        applicationsDAO.insertInstallation(application.id, accountId);
    }

    @Override
    public List<Application> getApplicationsByDevId(final Long accountId) {
        return applicationsDAO.getAllByDevId(accountId);
    }

    @Override
    public List<Application> getAll() {
        return applicationsDAO.getAll();
    }

    @Override
    public void updateScopes(Long applicationId, List<OAuthScope> scopes) {
        final List<Integer> scopesIds = new ArrayList<>();
        for(OAuthScope scope : scopes) {
            scopesIds.add(scope.getValue());
        }

        final SqlArray<Integer> sqlArray = new SqlArray<>(Integer.class, scopesIds);
        applicationsDAO.updateScopes(sqlArray, applicationId);
    }
}
