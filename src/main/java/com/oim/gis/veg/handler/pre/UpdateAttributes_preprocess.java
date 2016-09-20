package com.oim.gis.veg.handler.pre;

import com.oim.gis.veg.utils.Utils;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.exception.UserModifyException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.kernel.spi.PreProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateAttributes_preprocess implements PreProcessHandler {

    private static final Logger LOG = Logger.getLogger("PLUGINS");
    private final String className = getClass().getName();

    @Override
    public EventResult execute(long l, long l1, Orchestration orchestration) {
        LOG.entering(className, "---*--- execute ---*--- ");
        LOG.log(Level.INFO, "---* Entering  EventResult of UpdateAttributes_preprocess");
        LOG.log(Level.INFO, "---* orchestration.getOperation() -> [" + orchestration.getOperation() + "]");

        String userKey = orchestration.getTarget().getEntityId();
        LOG.log(Level.FINEST, "---* userKey -> [" + userKey + "]");
        LOG.log(Level.FINEST, "---* orchestration.getParameters() -> [" + orchestration.getParameters() + "]");

        executeEvent(userKey, orchestration.getOperation());

        LOG.log(Level.INFO, "---* Exiting  EventResult of UpdateAttributes_postprocess");
        LOG.exiting(className, "---*--- execute ---*---");

        return new EventResult();
    }

    @Override
    public BulkEventResult execute(long l, long l1, BulkOrchestration bulkOrchestration) {
        LOG.entering(className, "---*--- execute ---*---");
        LOG.log(Level.INFO, "---* Entering BulkEventResult of UpdateAttributes_postprocess");
        LOG.log(Level.INFO, "---* bulkOrchestration.getOperation() -> [" + bulkOrchestration.getOperation() + "]");

        HashMap[] bulkParameters = bulkOrchestration.getBulkParameters();

        String[] userKey = bulkOrchestration.getTarget().getAllEntityId();

        for (int i = 0; i < bulkParameters.length; i++) {
            LOG.log(Level.FINEST, "---* userKey -> [" + userKey[i] + "]");
            LOG.log(Level.FINEST, "---* bulkParameters -> [" + bulkParameters[i] + "]");
            executeEvent(userKey[i], bulkOrchestration.getOperation());
        }

        LOG.log(Level.INFO, "---* Exiting BulkEventResult of UpdateAttributes_postprocess");
        LOG.exiting(className, "---*--- execute ---*---");
        return new BulkEventResult();
    }

    private void executeEvent(String userKey, String operation) {
        LOG.entering(this.className, "---* executeEvent", new Object[]{operation, userKey});

        String response;

        if (operation.equalsIgnoreCase("DELETE")) {
            LOG.log(Level.INFO, "---* Operation is DELETE");

            try {
                response = updateLogin(userKey, true);
                LOG.log(Level.INFO, "---* response updateLogin-> [" + response + "]");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LOG.exiting(this.className, "---* executeEvent");
    }

    private String updateLogin(String userKey, boolean isDeleted) throws Exception {

        String response = "Error updateLogin";

        try {
            User user = Utils.getUserManager().getDetails(userKey, null, false);

            String currentLogin = user.getLogin();
            LOG.log(Level.FINEST, "---* currentLogin -> [" + currentLogin + "]");

            User updateUser = new User(userKey);

            if (isDeleted) {

                String login = null;

                if (currentLogin.toLowerCase().contains("fired")) {
                    if (currentLogin != null && currentLogin != "") {
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                        login = "DEL_" + currentLogin.substring(currentLogin.indexOf('_') + 1, currentLogin.lastIndexOf('_')) + "_" + simpleDateFormat.format(calendar.getTime());
                    }
                } else {
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                    login = "DEL_" + currentLogin + "_" + simpleDateFormat.format(calendar.getTime());
                }

                LOG.log(Level.FINEST, "---* login -> [" + login + "]");

                updateUser.setLogin(login);
            }

            Utils.getUserManager().modify(updateUser);

            response = "Success updateLogin";


        } catch (NoSuchUserException e) {
            LOG.log(Level.WARNING, "---* NoSuchUserException", e);
            throw new Exception(e);
        } catch (UserLookupException e) {
            LOG.log(Level.WARNING, "---* UserLookupException", e);
            throw new Exception(e);
        } catch (ValidationFailedException e) {
            LOG.log(Level.WARNING, "---* ValidationFailedException", e);
            throw new Exception(e);
        } catch (UserModifyException e) {
            LOG.log(Level.WARNING, "---* UserModifyException", e);
            throw new Exception(e);
        }

        return response;
    }

    @Override
    public boolean cancel(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {
        return false;
    }

    @Override
    public void compensate(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {

    }

    @Override
    public void initialize(HashMap<String, String> hashMap) {

    }

}
