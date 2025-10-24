package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class SmsSender {

    private static final String TAG = "SmsSender";

    private String smtpHost; // OVH SMTP
    private String smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private final Context context;

    private TeamChatBuddyApplication app;

    public interface MailCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public SmsSender(Context context) {
        this.context = context;
    }

    /**
     * Méthode principale :
     *  - Si SIM détectée → envoi SMS via SmsManager
     *  - Sinon → envoi mail vers OVH email2sms
     */
    public void sendSmsOrEmailFallback(String phoneNumber, String smsText, final MailCallback callback) {
        Log.i("AlertManager", "hasSimCard " + hasSimCardValid());
        if (hasSimCardValid() && isValidPhoneNumber(phoneNumber)) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, smsText, null, null);
                Log.i(TAG, "SMS :"+smsText+"\n envoyé via carte SIM vers " + phoneNumber);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l’envoi du SMS: " + e.getMessage(), e);
                if (callback != null) callback.onError(e);
            }
        } else {
            String ovh_identifier = app.getParamFromFile("ovh_identifier", "TeamChatBuddy.properties");
            String ovh_login = app.getParamFromFile("ovh_login", "TeamChatBuddy.properties");
            String ovh_customer_password = app.getParamFromFile("ovh_customer_password", "TeamChatBuddy.properties");
            String phoneSender = app.getParamFromFile("OVH_NUM_SENDER", "TeamChatBuddy.properties");
            Log.w("AlertManager", "Aucune carte SIM détectée — envoi via OVH email2sms to: "+ovh_identifier+":"+ovh_login+":"+ovh_customer_password+":" + phoneSender + ":" + phoneNumber);
            sendEmailToSms(ovh_identifier+":"+ovh_login+":"+ovh_customer_password+":" + phoneSender + ":" + phoneNumber, smsText, callback);
        }
    }

    /**
     * Vérifie si le device possède une carte SIM active.
     */
    private boolean hasSimCardValid() {
        try {
            TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int simState = telMgr.getSimState();

            // Step 1: Check if SIM is present and ready
            boolean hasSim = (simState == TelephonyManager.SIM_STATE_READY
                    || simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED
                    || simState == TelephonyManager.SIM_STATE_UNKNOWN);

            if (!hasSim) {
                Log.w(TAG, "Aucune carte SIM détectée ou non prête.");
                return false;
            }

            // Step 2: Check airplane mode status
            boolean isAirplaneModeOn = Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0;

            if (isAirplaneModeOn) {
                Log.w(TAG, "Mode avion activé — SIM détectée mais non fonctionnelle.");
                return false;
            }

            // SIM present and airplane mode off → valid
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification de la carte SIM: " + e.getMessage(), e);
            return false;
        }
    }


    /**
     * Envoi via OVH email2sms
     */
    private void sendEmailToSms(final String subject, final String body, final MailCallback callback) {
        new Thread(() -> {
            try {

                smtpUser = app.getParamFromFile("username", "TeamChatBuddy.properties");
                smtpPassword = app.getParamFromFile("username_Password", "TeamChatBuddy.properties");

                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", app.getParamFromFile("mail.smtp.host","TeamChatBuddy.properties"));
                props.put("mail.smtp.port", app.getParamFromFile("mail.smtp.port","TeamChatBuddy.properties"));
                props.put("mail.smtp.ssl.trust", app.getParamFromFile("mail.smtp.host","TeamChatBuddy.properties"));

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(smtpUser, smtpPassword);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(smtpUser));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(app.getParamFromFile("ovh_mail","TeamChatBuddy.properties"), false));
                message.setSubject(subject);
                message.setText(body, "UTF-8");

                Transport.send(message);
                Log.i(TAG, "Email envoyé vers OVH email2sms");
                if (callback != null) callback.onSuccess();

            } catch (Exception e) {
                Log.e(TAG, "Erreur envoi OVH: " + e.getMessage(), e);
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("^\\+?[0-9]{8,15}$");
    }
}