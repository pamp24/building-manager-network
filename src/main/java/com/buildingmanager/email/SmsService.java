package com.buildingmanager.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub υπηρεσίας SMS.
 *
 * <p>Για να λειτουργήσει πραγματικά το SMS χρειάζεται ένας SMS provider (π.χ. Twilio,
 * Vonage, Textlocal) με:
 * <ul>
 *   <li>λογαριασμό / συνδρομή με χρέωση ανά μήνυμα,</li>
 *   <li>Account SID + Auth Token (ή API key),</li>
 *   <li>έναν αριθμό αποστολέα,</li>
 *   <li>και τα στοιχεία στο application-dev.yml / application.yml (όχι στο git όταν είναι μυστικά).</li>
 * </ul>
 *
 * <p>Στο τοπικό περιβάλλον ΔΕΝ στέλνει πραγματικά SMS — καταγράφει το μήνυμα στο log ώστε
 * να μπορεί να δοκιμαστεί η ροή των ρυθμίσεων. Αντικαταστήστε το σώμα της {@link #sendSms}
 * με την κλήση του provider όταν υπάρχει πρόσβαση.
 */
@Service
@Slf4j
public class SmsService {

    /**
     * Στέλνει (ή καταγράφει) ένα SMS.
     *
     * @param to       αριθμός παραλήπτη (με διεθνή κωδικό χώρας, π.χ. +30...)
     * @param username ονομασία χρήστη για το μήνυμα
     * @param message  το κείμενο του μηνύματος
     */
    public void sendSms(String to, String username, String message) {
        // TODO: Εδώ καλείται ο πραγματικός SMS provider (π.χ. Twilio):
        //   Message.creator(new PhoneNumber(to), new PhoneNumber(FROM_NUMBER), message).create();
        // Για το τοπικό περιβάλλον απλώς καταγράφουμε την πρόθεση αποστολής.
        log.info("[SMS-STUB] Προς {} ({}): {}", to, username, message);
    }

    public boolean isConfigured() {
        // Επιστρέφει false μέχρι να ρυθμιστεί πραγματικός provider.
        return false;
    }
}
