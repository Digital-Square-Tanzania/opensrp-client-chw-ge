package org.smartregister.chw.ge.util;


import static org.smartregister.chw.ge.util.Constants.EVENT_TYPE.DELETE_EVENT;
import static org.smartregister.chw.ge.util.Constants.JSON_FORM_EXTRA.DELETE_EVENT_ID;
import static org.smartregister.chw.ge.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID;
import static org.smartregister.chw.ge.util.JsonFormUtils.HOME_VISIT_GROUP;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONObject;
import org.smartregister.chw.ge.dao.GeDao;
import org.smartregister.chw.ge.domain.Visit;
import org.smartregister.chw.ge.GeLibrary;
import org.smartregister.chw.ge.domain.VisitDetail;
import org.smartregister.chw.ge.repository.VisitDetailsRepository;
import org.smartregister.chw.ge.repository.VisitRepository;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.AllSharedPreferences;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import timber.log.Timber;

public class VisitUtils {


    public static void processVisits(VisitRepository visitRepository, VisitDetailsRepository visitDetailsRepository, Context context) throws Exception {

        List<Visit> visits = visitRepository.getAllUnSynced();
        List<Visit> visitList = new ArrayList<>();

        for (Visit v : visits) {

            if (v.getVisitType().equalsIgnoreCase(Constants.EVENT_TYPE.GE_FOLLOW_UP_VISIT)) {
                try {
                    visitList.add(v);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }

        if (visitList.size() > 0) {
            processVisits(visitList, visitRepository, visitDetailsRepository);
            //TODO: Extract string resource and give a more descriptive text
            Toast.makeText(context, "VISIT SAVED AND PROCESSED", Toast.LENGTH_SHORT).show();
        }
    }

    public static List<Visit> getVisits(String memberID, String... eventTypes) {

        List<Visit> visits = (eventTypes != null && eventTypes.length > 0) ? getVisitsOnly(memberID, eventTypes[0]) : getVisitsOnly(memberID, Constants.EVENT_TYPE.GE_FOLLOW_UP_VISIT);

        return visits;
    }


    public static List<Visit> getVisitsOnly(String memberID, String visitName) {
        return new ArrayList<>(GeLibrary.getInstance().visitRepository().getVisits(memberID, visitName));
    }

    public static List<VisitDetail> getVisitDetailsOnly(String visitID) {
        return GeLibrary.getInstance().visitDetailsRepository().getVisits(visitID);
    }


    public static Map<String, List<VisitDetail>> getVisitGroups(List<VisitDetail> detailList) {
        Map<String, List<VisitDetail>> visitMap = new HashMap<>();

        for (VisitDetail visitDetail : detailList) {

            List<VisitDetail> visitDetailList = visitMap.get(visitDetail.getVisitKey());
            if (visitDetailList == null)
                visitDetailList = new ArrayList<>();

            visitDetailList.add(visitDetail);

            visitMap.put(visitDetail.getVisitKey(), visitDetailList);
        }
        return visitMap;
    }

    /**
     * To be invoked for manual processing
     *
     * @param baseEntityID
     * @throws Exception
     */
    public static void processVisits(String baseEntityID) throws Exception {
        processVisits(GeLibrary.getInstance().visitRepository(), GeLibrary.getInstance().visitDetailsRepository(), baseEntityID);
    }

    public static void processVisits(VisitRepository visitRepository, VisitDetailsRepository visitDetailsRepository, String baseEntityID) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -0);

        List<Visit> visits = StringUtils.isNotBlank(baseEntityID) ?
                visitRepository.getAllUnSynced(calendar.getTime().getTime(), baseEntityID) :
                visitRepository.getAllUnSynced(calendar.getTime().getTime());
        processVisits(visits, visitRepository, visitDetailsRepository);
    }

    public static void processVisits(List<Visit> visits, VisitRepository visitRepository, VisitDetailsRepository visitDetailsRepository) throws Exception {
        String visitGroupId = UUID.randomUUID().toString();
        for (Visit v : visits) {
            if (!v.getProcessed()) {

                // persist to db
                Event baseEvent = new Gson().fromJson(v.getPreProcessedJson(), Event.class);
                if (StringUtils.isBlank(baseEvent.getFormSubmissionId()))
                    baseEvent.setFormSubmissionId(UUID.randomUUID().toString());

                baseEvent.addDetails(HOME_VISIT_GROUP, visitGroupId);

                AllSharedPreferences allSharedPreferences = GeLibrary.getInstance().context().allSharedPreferences();
                NCUtils.addEvent(allSharedPreferences, baseEvent);

                // process details
                //   processVisitDetails(visitGroupId, v, visitDetailsRepository, v.getVisitId(), v.getBaseEntityId(), baseEvent.getFormSubmissionId());

                visitRepository.completeProcessing(v.getVisitId());
            }
        }

        // process after all events are saved
        NCUtils.startClientProcessing();

        // process vaccines and services
        Context context = GeLibrary.getInstance().context().applicationContext();

    }


    public static Date getDateFromString(String dateStr) {
        try {
            return NCUtils.getSaveDateFormat().parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }


    /**
     * Check whether a visit occurred in the last 24 hours
     *
     * @param lastVisit The Visit instance for which you wish to check
     * @return true or false based on whether the visit was between 24 hours
     */
    public static boolean isVisitWithin24Hours(Visit lastVisit) {
        if (lastVisit != null) {
            return (Days.daysBetween(new DateTime(lastVisit.getCreatedAt()), new DateTime()).getDays() < 1) &&
                    (Days.daysBetween(new DateTime(lastVisit.getDate()), new DateTime()).getDays() <= 1);
        }
        return false;
    }

    public static void deleteSavedEvent(AllSharedPreferences allSharedPreferences, String baseEntityId, String eventId, String formSubmissionId, String type) {
        Event event = (Event) new Event()
                .withBaseEntityId(baseEntityId)
                .withEventDate(new Date())
                .withEventType(DELETE_EVENT)
                .withLocationId(JsonFormUtils.locationId(allSharedPreferences))
                .withProviderId(allSharedPreferences.fetchRegisteredANM())
                .withEntityType(type)
                .withFormSubmissionId(UUID.randomUUID().toString())
                .withDateCreated(new Date());

        event.addDetails(DELETE_EVENT_ID, eventId);
        event.addDetails(DELETE_FORM_SUBMISSION_ID, formSubmissionId);

        try {
            NCUtils.processEvent(event.getBaseEntityId(), new JSONObject(JsonFormUtils.gson.toJson(event)));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public static void deleteProcessedVisit(String visitID, String baseEntityId) {
        // check if the event
        AllSharedPreferences allSharedPreferences = GeLibrary.getInstance().context().allSharedPreferences();
        Visit visit = GeLibrary.getInstance().visitRepository().getVisitByVisitId(visitID);
        if (visit == null || !visit.getProcessed()) return;

        Event processedEvent = GeDao.getEventByFormSubmissionId(visit.getFormSubmissionId());
        if (processedEvent == null) return;
        deleteSavedEvent(allSharedPreferences, baseEntityId, processedEvent.getEventId(), processedEvent.getFormSubmissionId(), "event");
    }
}
