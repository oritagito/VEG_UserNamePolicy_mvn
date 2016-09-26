package com.oim.gis.veg.handler.pre;

import com.oim.gis.veg.utils.Utils;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.entitymgr.*;
import oracle.iam.platform.entitymgr.UnsupportedOperationException;
import oracle.iam.platform.kernel.spi.PreProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The type Update attributes preprocess.
 */
public class UpdateAttributes_preprocess implements PreProcessHandler {

    private static final Logger LOG = Logger.getLogger("PLUGINS");
    private final String className = getClass().getName();

    /**
     * @param l
     * @param l1
     * @param orchestration
     * @return
     */
    @Override
    public EventResult execute(long l, long l1, Orchestration orchestration) {
        LOG.entering(this.className, "---*--- entering EventResult execute ---*--- ", new Object[]{orchestration.getTarget().getType(), orchestration.getOperation()});

        String userKey = orchestration.getTarget().getEntityId();
        LOG.log(Level.FINEST, "---* userKey -> [" + userKey + "]");

        executeEvent(userKey, orchestration.getTarget().getType(), orchestration.getOperation());

        LOG.exiting(this.className, "---*--- exiting EventResult execute ---*---");
        return new EventResult();
    }

    /**
     * @param l
     * @param l1
     * @param bulkOrchestration
     * @return
     */
    @Override
    public BulkEventResult execute(long l, long l1, BulkOrchestration bulkOrchestration) {
        LOG.entering(this.className, "---*--- entering BulkEventResult execute ---*---", new Object[]{bulkOrchestration.getTarget().getType(), bulkOrchestration.getOperation()});

        HashMap[] bulkParameters = bulkOrchestration.getBulkParameters();

        String[] userKey = bulkOrchestration.getTarget().getAllEntityId();

        for (int i = 0; i < bulkParameters.length; i++) {
            LOG.log(Level.FINEST, "---* userKey -> [" + userKey[i] + "]");
            executeEvent(userKey[i], bulkOrchestration.getTarget().getType(), bulkOrchestration.getOperation());
        }

        LOG.exiting(this.className, "---*--- exiting BulkEventResult execute ---*---");
        return new BulkEventResult();
    }

    /**
     * выполняет обновление login при удалении
     *
     * @param userKey   - уникальный идентификатор пользователя
     * @param type      - тип операции. необходим для коммита
     * @param operation - вид операции
     */
    private void executeEvent(String userKey, String type, String operation) {
        LOG.entering(this.className, "---* entering executeEvent", new Object[]{userKey, type, operation});

        if (operation.equalsIgnoreCase("DELETE")) {
            updateLogin(userKey, type);
        }

        LOG.exiting(this.className, "---* exiting executeEvent");
    }

    /**
     * обновляет login
     * выполняет коммит
     *
     * @param userKey - уникальный идентификатор пользователя
     * @param type    - тип операции. необходим для коммита
     */
    private void updateLogin(String userKey, String type) {
        LOG.entering(this.className, "---* entering updateLogin", new Object[]{userKey, type});
        try {
            User user = Utils.getUserManager().getDetails(userKey, null, false);

            LOG.log(Level.FINEST, "---* Current login -> [" + user.getLogin() + "]");

            String login = null;
            // если текущий login содержит "fired". заменить на "del" (del - login - date)
            if (user.getLogin().toLowerCase().startsWith("fired")) {
                if (!user.getLogin().isEmpty()) {
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                    login = "DEL_" + user.getLogin().substring(user.getLogin().indexOf('_') + 1, user.getLogin().lastIndexOf('_')) + "_" + simpleDateFormat.format(calendar.getTime());
                }
                // иначе подставить к текущему логину "del" (del - login - date)
            } else {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                login = "DEL_" + user.getLogin() + "_" + simpleDateFormat.format(calendar.getTime());
            }

            LOG.log(Level.FINEST, "---* login after update -> [" + login + "]");

            if (login != null) {
                HashMap<String, Object> updateUser = new HashMap<String, Object>();
                updateUser.put(UserManagerConstants.AttributeName.USER_LOGIN.getName(), login);

                EntityManager entityManager = Utils.getEntityManager();
                entityManager.modifyEntity(type, userKey, updateUser);
            }
        } catch (NoSuchUserException e) {
            LOG.log(Level.SEVERE, "---* NoSuchUserException", e);
            e.printStackTrace();
        } catch (UserLookupException e) {
            LOG.log(Level.SEVERE, "---* UserLookupException", e);
            e.printStackTrace();
        } catch (InvalidDataTypeException e) {
            LOG.log(Level.SEVERE, "---* InvalidDataTypeException", e);
            e.printStackTrace();
        } catch (StaleEntityException e) {
            LOG.log(Level.SEVERE, "---* StaleEntityException", e);
            e.printStackTrace();
        } catch (NoSuchEntityException e) {
            LOG.log(Level.SEVERE, "---* NoSuchEntityException", e);
            e.printStackTrace();
        } catch (ProviderException e) {
            LOG.log(Level.SEVERE, "---* ProviderException", e);
            e.printStackTrace();
        } catch (UnknownAttributeException e) {
            LOG.log(Level.SEVERE, "---* UnknownAttributeException", e);
            e.printStackTrace();
        } catch (InvalidDataFormatException e) {
            LOG.log(Level.SEVERE, "---* InvalidDataFormatException", e);
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            LOG.log(Level.SEVERE, "---* UnsupportedOperationException", e);
            e.printStackTrace();
        }
        LOG.exiting(this.className, "---* exiting updateLogin");
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
