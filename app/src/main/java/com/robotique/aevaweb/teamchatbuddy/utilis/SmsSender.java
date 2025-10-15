package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

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

    private final String smtpHost = "mail.aevaweb.com"; // OVH SMTP
    private final int smtpPort = 587;
    private final String smtpUser;
    private final String smtpPassword;
    private final Context context;
    private String phoneNumberDevice;

    public interface MailCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public SmsSender(Context context, String user, String password) {
        this.context = context;
        this.smtpUser = user;
        this.smtpPassword = password;
    }

    /**
     * Méthode principale :
     *  - Si SIM détectée → envoi SMS via SmsManager
     *  - Sinon → envoi mail vers OVH email2sms
     */
    public void sendSmsOrEmailFallback(String phoneNumber, String smsText, final MailCallback callback) {
        Log.i("AlertManager", "hasSimCard " + hasSimCard());
        if (hasSimCard()) {
            phoneNumberDevice = getDevicePhoneNumber();
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, smsText, null, null);
                Log.i("AlertManager", "SMS :"+smsText+"\nenvoyé via carte SIM numéro: " + phoneNumberDevice+"  envoyé via carte SIM vers " + phoneNumber);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e("AlertManager", "Erreur lors de l’envoi du SMS: " + e.getMessage(), e);
                if (callback != null) callback.onError(e);
            }
        } else {
            Log.w("AlertManager", "Aucune carte SIM détectée — envoi via OVH email2sms");
            sendEmailToSms("sms-fb3019-2:aeva:Aeva2025:" + phoneNumberDevice + ":" + phoneNumber, smsText, callback);
        }
    }

    /**
     * Vérifie si le device possède une carte SIM active.
     */
    private boolean hasSimCard() {
        try {
            TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int simState = telMgr.getSimState();
            return simState == TelephonyManager.SIM_STATE_READY;
        } catch (Exception e) {
            Log.e("AlertManager", "Erreur lors de la vérification SIM: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envoi via OVH email2sms
     */
    private void sendEmailToSms(final String subject, final String body, final MailCallback callback) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", smtpHost);
                props.put("mail.smtp.port", String.valueOf(smtpPort));
                props.put("mail.smtp.ssl.trust", smtpHost);

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(smtpUser, smtpPassword);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(smtpUser));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse("email2sms@ovh.net", false));
                message.setSubject(subject);
                message.setText(body, "UTF-8");

                Transport.send(message);
                Log.i("AlertManager", "Email envoyé vers OVH email2sms");
                if (callback != null) callback.onSuccess();

            } catch (Exception e) {
                Log.e("AlertManager", "Erreur envoi OVH: " + e.getMessage(), e);
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    @SuppressLint("MissingPermission")
    private String getDevicePhoneNumber() {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) return null;

            String phoneNumber = telephonyManager.getLine1Number();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                Log.i(TAG, "Numéro de téléphone détecté : " + phoneNumber);
            } else {
                Log.w(TAG, "Numéro de téléphone non disponible (retourne null ou vide)");
            }
            return phoneNumber;
        } catch (Exception e) {
            Log.e(TAG, "Erreur récupération numéro téléphone: " + e.getMessage(), e);
            return null;
        }
    }

}