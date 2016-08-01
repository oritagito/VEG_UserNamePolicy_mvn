package com.oim.gis.veg.handler.post;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Operations.tcUserOperationsIntf;
import Thor.API.tcResultSet;
import com.oim.gis.veg.utils.Utils;
import oracle.iam.platform.Platform;
import oracle.iam.platform.context.ContextAware;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateAttributes_postprocess implements PostProcessHandler {

    private static final Logger log = Logger.getLogger("PLUGINS");
    private final String className = getClass().getName();

    private final String lookupWorking = "Working";                // Работает
    private final String lookupVacation = "Vacation";              // В Отпуске
    private final String lookupFired = "Fired";                    // Уволен
    private final String lookupMaternityLeave = "Maternity-Leave"; // Декретный Отпуск

    private final String usrActive = "Active";
    private final String usrDisabled = "Disabled";

    public EventResult execute(long l, long l1, Orchestration orchestration) {
        log.entering(className, "---*--- execute ---*--- ");
        log.log(Level.INFO, "---* Entering  EventResult of UpdateAttributes_postprocess");
        log.log(Level.INFO, "---* orchestration.getOperation() -> [" + orchestration.getOperation() + "]");

        String userKey = orchestration.getTarget().getEntityId();
        log.log(Level.FINEST, "---* userKey -> [" + userKey + "]");

        HashMap<String, Serializable> orchestrationParameters = orchestration.getParameters();
        log.log(Level.FINEST, "---* orchestrationParameters -> [" + orchestrationParameters + "]");

        Set<String> parametersSet = orchestrationParameters.keySet();
        log.log(Level.FINEST, "---* parametersSet -> [" + parametersSet + "]");

        String empStatus = null;

        if (orchestration.getOperation().equalsIgnoreCase("MODIFY")) {
            log.log(Level.INFO, "---* Operation is MODIFY");

            boolean isUpdateAttributes = false;

            for (String key : parametersSet) {
                Serializable serializable = orchestrationParameters.get(key);
                if (key.equalsIgnoreCase("emp_status")) {
                    isUpdateAttributes = true;
                    if (serializable == null) empStatus = "";
                    else {
                        empStatus = serializable.toString();
                        log.log(Level.FINEST, "---* empStatus -> [" + empStatus + "]");
                    }
                }
            }

            if (isUpdateAttributes) {
                String response = empStatusIsUpdate(userKey, empStatus);
                log.log(Level.INFO, "---* response updateEmpStatus-> [" + response + "]");
            }
        } else if (orchestration.getOperation().equalsIgnoreCase("CREATE")) {
            log.log(Level.INFO, "---* Operation is CREATE");

            empStatus = getParameterValue(orchestrationParameters, "emp_status") == null ? "" : getParameterValue(orchestrationParameters, "emp_status");
            log.log(Level.FINEST, "---* empStatus -> [" + empStatus + "]");

            String response = empStatusIsUpdate(userKey, empStatus);
            log.log(Level.INFO, "---* response updateEmpStatus-> [" + response + "]");
        }

        log.log(Level.INFO, "---* Exiting  EventResult of UpdateAttributes_postprocess");
        log.exiting(className, "---*--- execute ---*---");

        return new EventResult();
    }

    public BulkEventResult execute(long l, long l1, BulkOrchestration bulkOrchestration) {
        log.entering(className, "---*--- execute ---*---");
        log.log(Level.INFO, "---* Entering BulkEventResult of UpdateAttributes_postprocess");
        log.log(Level.INFO, "---* bulkOrchestration.getOperation() -> [" + bulkOrchestration.getOperation() + "]");

        String userKey = bulkOrchestration.getTarget().getEntityId();
        log.log(Level.FINEST, "---* userKey -> [" + userKey + "]");

        HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
        log.log(Level.FINEST, "---* bulkParameters -> [" + bulkParameters + "]");

        String empStatus = null;

        if (bulkOrchestration.getOperation().equalsIgnoreCase("MODIFY")) {
            log.log(Level.INFO, "---* Operation is MODIFY");

            for (HashMap<String, Serializable> bulkParameter : bulkParameters) {
                Set<String> bulkKeySet = bulkParameter.keySet();

                boolean isUpdateAttributes = false;

                for (String key : bulkKeySet) {
                    Serializable serializable = bulkParameter.get(key);
                    if (key.equalsIgnoreCase("emp_status")) {
                        isUpdateAttributes = true;
                        if (serializable == null) empStatus = "";
                        else {
                            empStatus = serializable.toString();
                            log.log(Level.FINEST, "---* empStatus -> [" + empStatus + "]");
                        }
                    }
                }
                if (isUpdateAttributes) {
                    String response = empStatusIsUpdate(userKey, empStatus);
                    log.log(Level.INFO, "---* response updateEmpStatus-> [" + response + "]");
                }
            }
        } else if (bulkOrchestration.getOperation().equalsIgnoreCase("CREATE")) {
            log.log(Level.INFO, "---* Operation is CREATE");

            for (HashMap<String, Serializable> bulkParameter : bulkParameters) {

                empStatus = getParameterValue(bulkParameter, "emp_status") == null ? "" : getParameterValue(bulkParameter, "emp_status");
                log.log(Level.FINEST, "---* empStatus -> [" + empStatus + "]");

                String response = empStatusIsUpdate(userKey, empStatus);
                log.log(Level.INFO, "---* response updateEmpStatus-> [" + response + "]");
            }
        }

        log.log(Level.INFO, "---* Exiting BulkEventResult of UpdateAttributes_postprocess");
        log.exiting(className, "---*--- execute ---*---");
        return new BulkEventResult();
    }

    private String empStatusIsUpdate(String userKey, String empStatus) {

        String response = null;

        Map<String, String> phAttributeList = new HashMap<String, String>();
        phAttributeList.put("Users.Key", userKey);
        tcResultSet resultSet;

        try {
            tcUserOperationsIntf tcUserOperationsIntf = (Platform.getService(tcUserOperationsIntf.class));
            resultSet = tcUserOperationsIntf.findUsers(phAttributeList);

            int count = 0;
            count = resultSet.getRowCount();
            log.log(Level.FINEST, "---* count -> [" + count + "]");

            for (int i = 0; i < count; i++) {
                resultSet.goToRow(i);

                String usrLogin = resultSet.getStringValue("Users.User ID");
                String usrStatus = resultSet.getStringValue("Users.Status");
                String usrDisplayName = resultSet.getStringValue("Users.Display Name");
                String firstName = resultSet.getStringValue("Users.First Name");
                String lastName = resultSet.getStringValue("Users.Last Name");
                String middleName = resultSet.getStringValue("Users.Middle Name");

                log.log(Level.FINEST, "---* usrLogin -> [" + usrLogin + "]");
                log.log(Level.FINEST, "---* usrStatus -> [" + usrStatus + "]");
                log.log(Level.FINEST, "---* usrDisplayName -> [" + usrDisplayName + "]");
                log.log(Level.FINEST, "---* firstName -> [" + firstName + "]");
                log.log(Level.FINEST, "---* lastName -> [" + lastName + "]");
                log.log(Level.FINEST, "---* middleName -> [" + middleName + "]");

                if (empStatus != null && empStatus != "") {

                    Map<String, String> updateUsr = new HashMap<String, String>();

                    if (empStatus.equalsIgnoreCase(lookupWorking)) {
                        if (!usrStatus.equalsIgnoreCase(usrActive)) {
                            tcUserOperationsIntf.enableUser(Long.valueOf(userKey).longValue());
                            log.log(Level.FINEST, "---* Enabled user -> [" + userKey + "] Successfully");
                        }
                        if (usrDisplayName.toLowerCase().contains("уволен")) {
                            usrDisplayName = getDisplayName(firstName, lastName, middleName, false);
                            updateUsr.put("Users.Display Name", usrDisplayName);
                        }
                        if (usrLogin.toLowerCase().contains("fired")) {
                            usrLogin = Utils.generateName(firstName, lastName, middleName);
                            updateUsr.put("User Login", usrLogin);
                        }
                    }

                    if (empStatus.equalsIgnoreCase(lookupVacation)) {
                        if (!usrStatus.equalsIgnoreCase(usrDisabled)) {
                            tcUserOperationsIntf.disableUser(Long.valueOf(userKey).longValue());
                            log.log(Level.FINEST, "---* Disabled user -> [" + userKey + "] Successfully");
                        }
                        if (usrDisplayName.toLowerCase().contains("уволен")) {
                            usrDisplayName = getDisplayName(firstName, lastName, middleName, false);
                            updateUsr.put("Users.Display Name", usrDisplayName);
                        }
                        if (usrLogin.toLowerCase().contains("fired")) {
                            usrLogin = Utils.generateName(firstName, lastName, middleName);
                            updateUsr.put("User Login", usrLogin);
                        }
                    }

                    if (empStatus.equalsIgnoreCase(lookupFired)) {
                        if (!usrStatus.equalsIgnoreCase(usrDisabled)) {
                            tcUserOperationsIntf.disableUser(Long.valueOf(userKey).longValue());
                            log.log(Level.FINEST, "---* Disabled user -> [" + userKey + "] Successfully");
                        }
                        if (!usrDisplayName.toLowerCase().contains("уволен")) {
                            usrDisplayName = getDisplayName(firstName, lastName, middleName, true);
                            updateUsr.put("Users.Display Name", usrDisplayName);
                        }
                        if (!usrLogin.toLowerCase().contains("fired")) {
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                            usrLogin = "FIRED_" + usrLogin + "_" + simpleDateFormat.format(calendar.getTime());
                            updateUsr.put("User Login", usrLogin);
                        }
                    }

                    if (empStatus.equalsIgnoreCase(lookupMaternityLeave)) {
                        if (!usrStatus.equalsIgnoreCase(usrDisabled)) {
                            tcUserOperationsIntf.disableUser(Long.valueOf(userKey).longValue());
                            log.log(Level.FINEST, "---* Disabled user -> [" + userKey + "] Successfully");
                        }
                        if (usrDisplayName.toLowerCase().contains("уволен")) {
                            usrDisplayName = getDisplayName(firstName, lastName, middleName, false);
                            updateUsr.put("Users.Display Name", usrDisplayName);
                        }
                        if (usrLogin.toLowerCase().contains("fired")) {
                            usrLogin = Utils.generateName(firstName, lastName, middleName);
                            updateUsr.put("User Login", usrLogin);
                        }
                    }
                    tcUserOperationsIntf.updateUser(resultSet, updateUsr);
                }
            }
            tcUserOperationsIntf.close();
        } catch (tcAPIException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    private String getDisplayName(String firstName, String lastName, String middleName, boolean isFired) {
        String usrDisplayName = null;

        if (!isFired) {
            if (lastName != null && lastName != "") usrDisplayName = lastName;
            if (firstName != null && firstName != "") usrDisplayName = usrDisplayName + " " + firstName;
            if (middleName != null && middleName != "") usrDisplayName = usrDisplayName + " " + middleName;
        } else {
            if (lastName != null && lastName != "") usrDisplayName = lastName;
            if (firstName != null && firstName != "") usrDisplayName = usrDisplayName + " " + firstName;
            if (middleName != null && middleName != "") usrDisplayName = usrDisplayName + " " + middleName;
            usrDisplayName = "Уволен " + usrDisplayName;
        }
        return usrDisplayName;
    }

    private String getParameterValue(HashMap<String, Serializable> parameters, String key) {
        String value = (parameters.get(key) instanceof ContextAware) ? (String) ((ContextAware) parameters.get(key)).getObjectValue() : (String) parameters.get(key);
        return value;
    }

    public boolean cancel(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {
        return false;
    }

    public void compensate(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {

    }

    public void initialize(HashMap<String, String> hashMap) {

    }
}
