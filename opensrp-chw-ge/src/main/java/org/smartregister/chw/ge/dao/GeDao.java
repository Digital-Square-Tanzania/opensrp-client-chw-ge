package org.smartregister.chw.ge.dao;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.ge.GeLibrary;
import org.smartregister.chw.ge.domain.MemberObject;
import org.smartregister.chw.ge.util.Constants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.dao.AbstractDao;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class GeDao extends AbstractDao {
    public static void closeGeMemberFromRegister(String baseEntityID) {
        String sql = "update ec_malaria_confirmation set is_closed = 1 where base_entity_id = '" + baseEntityID + "'";
        updateDB(sql);
    }

    public static boolean isRegisteredForGe(String baseEntityID) {
        String sql = "SELECT count(p.base_entity_id) count FROM " + Constants.TABLES.GE_REGISTER + " p " + "WHERE p.base_entity_id = '" + baseEntityID + "' AND p.is_closed = 0 AND p.consent_given = 'yes'  ";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "count");

        List<Integer> res = readData(sql, dataMap);
        if (res == null || res.size() != 1) return false;

        return res.get(0) > 0;
    }

    public static MemberObject getMember(String baseEntityID) {
        String sql = "select m.base_entity_id , m.unique_id , m.relational_id , m.dob , m.first_name , m.middle_name , m.last_name , m.gender , m.phone_number , m.other_phone_number , f.first_name family_name ,f.primary_caregiver , f.family_head , f.village_town ,fh.first_name family_head_first_name , fh.middle_name family_head_middle_name , fh.last_name family_head_last_name, fh.phone_number family_head_phone_number ,  pcg.first_name pcg_first_name , pcg.last_name pcg_last_name , pcg.middle_name pcg_middle_name , pcg.phone_number  pcg_phone_number , mr.* from ec_family_member m inner join ec_family f on m.relational_id = f.base_entity_id inner join " + Constants.TABLES.GE_REGISTER + " mr on mr.base_entity_id = m.base_entity_id left join ec_family_member fh on fh.base_entity_id = f.family_head left join ec_family_member pcg on pcg.base_entity_id = f.primary_caregiver where m.base_entity_id ='" + baseEntityID + "' ";
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        DataMap<MemberObject> dataMap = cursor -> {
            MemberObject memberObject = new MemberObject();

            memberObject.setFirstName(getCursorValue(cursor, "first_name", ""));
            memberObject.setMiddleName(getCursorValue(cursor, "middle_name", ""));
            memberObject.setLastName(getCursorValue(cursor, "last_name", ""));
            memberObject.setAddress(getCursorValue(cursor, "village_town"));
            memberObject.setGender(getCursorValue(cursor, "gender"));
            memberObject.setUniqueId(getCursorValue(cursor, "unique_id", ""));
            memberObject.setDob(getCursorValue(cursor, "dob"));
            memberObject.setFamilyBaseEntityId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setRelationalId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setPrimaryCareGiver(getCursorValue(cursor, "primary_caregiver"));
            memberObject.setFamilyName(getCursorValue(cursor, "family_name", ""));
            memberObject.setPhoneNumber(getCursorValue(cursor, "phone_number", ""));
            memberObject.setBaseEntityId(getCursorValue(cursor, "base_entity_id", ""));
            memberObject.setFamilyHead(getCursorValue(cursor, "family_head", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "pcg_phone_number", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "family_head_phone_number", ""));
            memberObject.setHivStatus(getCursorValue(cursor, "hiv_status", ""));
            memberObject.setCtcNumber(getCursorValue(cursor, "ctc_number", ""));

            String familyHeadName = getCursorValue(cursor, "family_head_first_name", "") + " " + getCursorValue(cursor, "family_head_middle_name", "");

            familyHeadName = (familyHeadName.trim() + " " + getCursorValue(cursor, "family_head_last_name", "")).trim();
            memberObject.setFamilyHeadName(familyHeadName);

            String familyPcgName = getCursorValue(cursor, "pcg_first_name", "") + " " + getCursorValue(cursor, "pcg_middle_name", "");

            familyPcgName = (familyPcgName.trim() + " " + getCursorValue(cursor, "pcg_last_name", "")).trim();
            memberObject.setPrimaryCareGiverName(familyPcgName);

            return memberObject;
        };

        List<MemberObject> res = readData(sql, dataMap);
        if (res == null || res.size() != 1) return null;

        return res.get(0);
    }

    public static void updateGeMobilization(GeMobilization geMobilization) {
        String sql = String.format("INSERT INTO " + Constants.TABLES.GE_MOBILIZATION_SESSIONS +
                        " (" + "id, " +
                        "event_start_date, " +
                        "event_end_date, " +
                        "event_type, " +
                        "event_supporter, " +
                        "number_of_male_members_loan, " +
                        "number_of_female_members_loan, " +
                        "total_number_of_male_citizens_reached_by_the_campaign, " +
                        "total_number_of_female_citizens_reached_by_the_campaign, " +
                        "last_interacted_with" + ") " +
                        "VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s') ON CONFLICT (id) DO UPDATE SET " +
                        "event_start_date = '%s', " + "event_end_date = '%s', " + "event_type = '%s', " +
                        "event_supporter = '%s', " +
                        "number_of_male_members_loan = '%s', " +
                        "number_of_female_members_loan = '%s', " +
                        "total_number_of_male_citizens_reached_by_the_campaign = '%s', " +
                        "total_number_of_female_citizens_reached_by_the_campaign = '%s', " +
                        "last_interacted_with = '%s' ",
                geMobilization.getBaseEntityID(),
                geMobilization.getEventStartDate(),
                geMobilization.getEventEndDate(),
                geMobilization.getMobilizationEventType(),
                geMobilization.getEventSupporter(),
                geMobilization.getNumberOfMaleMembersLoan(),
                geMobilization.getNumberOfFemaleMembersLoan(),
                geMobilization.getTotalNumberOfMaleCitizensReachedByTheCampaign(),
                geMobilization.getTotalNumberOfFemaleCitizensReachedByTheCampaign(),
                geMobilization.getLastInteractedWith(),
                geMobilization.getEventStartDate(),
                geMobilization.getEventEndDate(),
                geMobilization.getMobilizationEventType(),
                geMobilization.getEventSupporter(),
                geMobilization.getNumberOfMaleMembersLoan(),
                geMobilization.getNumberOfFemaleMembersLoan(),
                geMobilization.getTotalNumberOfMaleCitizensReachedByTheCampaign(),
                geMobilization.getTotalNumberOfFemaleCitizensReachedByTheCampaign(),
                geMobilization.getLastInteractedWith()
        );
        updateDB(sql);
    }

    public static Event getEventByFormSubmissionId(String formSubmissionId) {
        String sql = "select json from event where formSubmissionId = '" + formSubmissionId + "'";
        DataMap<Event> dataMap = (c) -> {
            Event event;
            try {
                event = (Event) GeLibrary.getInstance().getEcSyncHelper().convert(new JSONObject(getCursorValue(c, "json")), Event.class);
            } catch (JSONException e) {
                Timber.e(e);
                return null;
            }

            return event;
        };
        return (Event) AbstractDao.readSingleValue(sql, dataMap);
    }

    public static class GeMobilization {
        private String baseEntityID;

        private String eventStartDate;

        private String eventEndDate;

        private String mobilizationEventType;

        private String eventSupporter;

        private String numberOfMaleMembersLoan;

        private String numberOfFemaleMembersLoan;

        private String totalNumberOfMaleCitizensReachedByTheCampaign;

        private String totalNumberOfFemaleCitizensReachedByTheCampaign;

        private Long lastInteractedWith;

        public String getBaseEntityID() {
            return baseEntityID;
        }

        public void setBaseEntityID(String baseEntityID) {
            this.baseEntityID = baseEntityID;
        }

        public String getEventStartDate() {
            return eventStartDate;
        }

        public void setEventStartDate(String eventStartDate) {
            this.eventStartDate = eventStartDate;
        }

        public String getEventEndDate() {
            return eventEndDate;
        }

        public void setEventEndDate(String eventEndDate) {
            this.eventEndDate = eventEndDate;
        }

        public String getMobilizationEventType() {
            return mobilizationEventType;
        }

        public void setMobilizationEventType(String mobilizationEventType) {
            this.mobilizationEventType = mobilizationEventType;
        }

        public String getEventSupporter() {
            return eventSupporter;
        }

        public void setEventSupporter(String eventSupporter) {
            this.eventSupporter = eventSupporter;
        }

        public Long getLastInteractedWith() {
            return lastInteractedWith;
        }

        public void setLastInteractedWith(Long lastInteractedWith) {
            this.lastInteractedWith = lastInteractedWith;
        }

        public String getNumberOfMaleMembersLoan() {
            return numberOfMaleMembersLoan;
        }

        public void setNumberOfMaleMembersLoan(String numberOfMaleMembersLoan) {
            this.numberOfMaleMembersLoan = numberOfMaleMembersLoan;
        }

        public String getNumberOfFemaleMembersLoan() {
            return numberOfFemaleMembersLoan;
        }

        public void setNumberOfFemaleMembersLoan(String numberOfFemaleMembersLoan) {
            this.numberOfFemaleMembersLoan = numberOfFemaleMembersLoan;
        }

        public String getTotalNumberOfMaleCitizensReachedByTheCampaign() {
            return totalNumberOfMaleCitizensReachedByTheCampaign;
        }

        public void setTotalNumberOfMaleCitizensReachedByTheCampaign(String totalNumberOfMaleCitizensReachedByTheCampaign) {
            this.totalNumberOfMaleCitizensReachedByTheCampaign = totalNumberOfMaleCitizensReachedByTheCampaign;
        }

        public String getTotalNumberOfFemaleCitizensReachedByTheCampaign() {
            return totalNumberOfFemaleCitizensReachedByTheCampaign;
        }

        public void setTotalNumberOfFemaleCitizensReachedByTheCampaign(String totalNumberOfFemaleCitizensReachedByTheCampaign) {
            this.totalNumberOfFemaleCitizensReachedByTheCampaign = totalNumberOfFemaleCitizensReachedByTheCampaign;
        }
    }
}
