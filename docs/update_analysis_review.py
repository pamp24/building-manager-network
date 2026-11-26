"""Rebuild dated review supplements, retaining the original 29-page analyses."""
from pathlib import Path
from io import BytesIO
from html import escape
import json
from reportlab.pdfgen import canvas
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.colors import HexColor
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader, PdfWriter

ROOT = Path(__file__).resolve().parents[1]
FRONT = ROOT.parent / 'building-manager-front-end'
TMP = ROOT / 'tmp' / 'pdfs'
TMP.mkdir(parents=True, exist_ok=True)
pdfmetrics.registerFont(TTFont('Review', 'C:/Windows/Fonts/arial.ttf'))
pdfmetrics.registerFont(TTFont('ReviewBold', 'C:/Windows/Fonts/arialbd.ttf'))
pdfmetrics.registerFontFamily('Review', normal='Review', bold='ReviewBold', italic='Review', boldItalic='ReviewBold')
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='TitleGR', fontName='ReviewBold', fontSize=25, leading=31, textColor=HexColor('#123248'), spaceAfter=20))
styles.add(ParagraphStyle(name='HGR', fontName='ReviewBold', fontSize=15, leading=20, textColor=HexColor('#123248'), spaceAfter=12))
styles.add(ParagraphStyle(name='BodyGR', fontName='Review', fontSize=10, leading=15, spaceAfter=10))
styles.add(ParagraphStyle(name='RefGR', fontName='Review', fontSize=8, leading=11, textColor=HexColor('#506070'), spaceAfter=12, wordWrap='CJK'))

def ref(root, file, needle):
    p = root / file
    lines = p.read_text(encoding='utf-8-sig').splitlines()
    line = next(i for i, s in enumerate(lines, 1) if needle in s)
    return f'{file}:{line}'

def finding(code, priority, title, behavior, fix, refs):
    return dict(code=code, priority=priority, title=title, behavior=behavior, fix=fix, refs=refs)

B = 'src/main/java/com/buildingmanager/'
F = 'src/app/'
backend = [
 finding('BE-01','P0','Αλλαγή ρόλου χωρίς έλεγχο Admin',
 'Ένας συνδεδεμένος χρήστης μπορεί να καλέσει PUT /api/v1/users/{userId}/role?roleName=Admin. Το controller αποθηκεύει απευθείας τον ρόλο χωρίς έλεγχο του καλούντος. Υπάρχει και δεύτερη διαδρομή POST /users/{userId}/roles/assign. Με νέο login μπορεί να αποκτηθεί ο νέος ρόλος. Το audit καταγράφει την πράξη, δεν την εξουσιοδοτεί.',
 'Περιορισμός και των δύο διαδρομών σε Admin στο service/controller, έλεγχος επιτρεπόμενων μεταβάσεων και tests όπου Resident/Owner λαμβάνει 403 χωρίς αλλαγή βάσης.',
 [ref(ROOT,B+'user/UserController.java','public ResponseEntity<?> updateUserRole'),ref(ROOT,B+'user/UserController.java','public ResponseEntity<?> assignRoleToUser'),ref(ROOT,B+'security/SecurityConfig.java','requestMatchers("/api/v1/users/*/role")')]),
 finding('BE-02','P1','Πληρωμές και allocations χωρίς έλεγχο πολυκατοικίας',
 'Τα PaymentController/PaymentService και CommonExpenseAllocationController/Service δέχονται IDs χωρίς έλεγχο ιδιοκτησίας ή δικαιώματος στο κτίριο. Έγκυρο JWT άλλης πολυκατοικίας αρκεί για ανάγνωση πληρωμών ή PATCH /expenses/allocations/{id}/pay και /unpay, όταν είναι γνωστό το ID.',
 'Κεντρικός έλεγχος BuildingPermissionService και σύνδεσης χρήστη-διαμερίσματος σε κάθε read/write. Tests δύο κτιρίων: νόμιμη πρόσβαση 200, ξένο ID 403 και καμία μεταβολή.',
 [ref(ROOT,B+'payment/PaymentController.java','public class PaymentController'),ref(ROOT,B+'payment/PaymentService.java','public PaymentDTO createPayment'),ref(ROOT,B+'commonExpenseAllocation/CommonExpenseAllocationController.java','public ResponseEntity<CommonExpenseAllocation> markAsPaid')]),
 finding('BE-03','P1','Ιδιωτικά έγγραφα εκτίθενται ως δημόσια uploads',
 'Η λίστα εγγράφων ελέγχει canViewBuilding, αλλά τα φυσικά αρχεία αποθηκεύονται κάτω από uploads/buildings/{id}. Το resource handler εκθέτει όλο το root και το SecurityConfig επιτρέπει ανώνυμη πρόσβαση σε /uploads/** και /api/v1/uploads/**. Όποιος έχει το URL παρακάμπτει τον έλεγχο της λίστας. Δεν απαιτείται να μπορεί να μαντέψει το UUID.',
 'Διαχωρισμός δημόσιων φωτογραφιών από ιδιωτικά έγγραφα. Download controller με authorization ή βραχύβια signed URLs. Test ανώνυμης και διακτιριακής λήψης γνωστού URL.',
 [ref(ROOT,B+'security/SecurityConfig.java','"/uploads/**"'),ref(ROOT,B+'config/UploadResourceConfiguration.java','addResourceHandler(prefix'),ref(ROOT,B+'fileStorage/LocalFileStorageService.java','String publicUrl =')]),
 finding('BE-04','P1','Τα webhooks πληρωμών απαιτούν JWT χρήστη',
 'Με το καθολικό /api/v1 prefix, τα endpoints είναι /api/v1/payments/webhook/stripe και /viva. Δεν περιλαμβάνονται στα permitAll. Callback παρόχου χωρίς Bearer token απορρίπτεται πριν φτάσει στην επαλήθευση υπογραφής. Έτσι η επιτυχής εξωτερική χρέωση δεν ενημερώνει το λογιστικό υπόλοιπο.',
 'Ρητό άνοιγμα μόνο των callback routes με επαλήθευση παρόχου. Για Stripe να διατηρηθεί ο έλεγχος υπογραφής. MockMvc tests χωρίς JWT: έγκυρη υπογραφή φτάνει στο handler, άκυρη απορρίπτεται.',
 [ref(ROOT,B+'payment/PaymentWebhookController.java','@RequestMapping'),ref(ROOT,B+'config/ApiVersionConfig.java','"/api/v1"'),ref(ROOT,B+'security/SecurityConfig.java','.anyRequest().authenticated()')]),
 finding('BE-05','P1','Επανάληψη Stripe webhook διπλομετρά μερική πληρωμή',
 'Το handleWebhook καλεί κάθε φορά createPayment χωρίς έλεγχο event/intent ID. Αν οφειλή 100 EUR έχει intent 20 EUR και το ίδιο event φτάσει δύο φορές, η υπάρχουσα πληρωμή αυξάνεται σε 40 EUR και οι κατανομές ενημερώνονται ξανά. Στην πλήρη εξόφληση το retry μπορεί αντίθετα να αποτύχει ως ήδη εξοφλημένο.',
 'Μοναδικό provider event/transaction key, ατομική καταχώριση event και λογιστικής εγγραφής. Το ίδιο callback να επιστρέφει επιτυχία χωρίς δεύτερη επίδραση. Test διαδοχικών και ταυτόχρονων retries.',
 [ref(ROOT,B+'payment/StripePaymentService.java','PaymentDTO paymentDTO = paymentService.createPayment'),ref(ROOT,B+'payment/PaymentService.java','payment.setAmount(current.add(reqAmount)')]),
 finding('BE-06','P1','Μη θετικά ποσά και ασυνεπής offline εξόφληση',
 'Το PaymentRequest επιβάλλει μόνο NotNull στο amount. Αρνητική τιμή περνά τους ελέγχους υπερπληρωμής, μειώνει το Payment.amount, αλλά το allocation loop σταματά επειδή remainingToAllocate <= 0. Επίσης το PATCH /allocations/{id}/pay αγνοεί το JSON amount και αλλάζει μόνο isPaid/paidDate, χωρίς paidAmount, status, payment ledger ή statement.',
 'Θετικό ποσό σε ακέραια cents ή BigDecimal, κοινή transactional ροή καταχώρισης πληρωμής και συγχρονισμός όλων των υπολοίπων. Tests αρνητικού/μηδενικού ποσού, μερικής και πλήρους offline εξόφλησης.',
 [ref(ROOT,B+'payment/PaymentRequest.java','private Double amount'),ref(ROOT,B+'payment/PaymentService.java','BigDecimal reqAmount ='),ref(ROOT,B+'commonExpenseAllocation/CommonExpenseAllocationService.java','allocation.setIsPaid(true)')]),
 finding('BE-07','P1','Απενεργοποίηση λογαριασμού δεν ανακαλεί ενεργό JWT',
 'Το JwtFilter φορτώνει τον χρήστη αλλά δεν εξετάζει isEnabled/isAccountNonLocked. Το isTokenValid ελέγχει μόνο username και λήξη. Οι authorities προέρχονται από το token, άρα απενεργοποίηση, κλείδωμα ή υποβιβασμός ρόλου δεν εφαρμόζεται πλήρως στα ήδη εκδοθέντα JWT μέχρι να λήξουν.',
 'Έλεγχος τρέχουσας κατάστασης λογαριασμού, token version/revocation ή τρέχουσες authorities. Test: έκδοση JWT, disable/demote, επόμενο protected request απορρίπτεται ή έχει μειωμένα δικαιώματα.',
 [ref(ROOT,B+'security/JwtFilter.java','if (jwtService.isTokenValid'),ref(ROOT,B+'security/JwtService.java','public boolean isTokenValid')]),
 finding('BE-08','P1','Το test suite δεν μεταγλωττίζεται',
 'Το mvnw.cmd test -q αποτυγχάνει στο CommonExpenseStatementServiceTest.java:65. Το test δημιουργεί service με 6 dependencies, ενώ ο constructor πλέον απαιτεί 9, μαζί με NotificationPreferenceService, EmailService και StatementPdfService. Δεν εκτελέστηκαν tests σε αυτό το run.',
 'Ενημέρωση mocks/setup και επανεκτέλεση ολόκληρου του suite. Το επιτυχές packaging με maven.test.skip=true αποδεικνύει build, όχι ορθότητα συμπεριφοράς.',
 [ref(ROOT,'src/test/java/com/buildingmanager/commonExpenseStatement/CommonExpenseStatementServiceTest.java','new CommonExpenseStatementService('),ref(ROOT,B+'commonExpenseStatement/CommonExpenseStatementService.java','private final NotificationPreferenceService')]),
]
frontend = [
 finding('FE-01','P1','Το production build στοχεύει mock API',
 'Το environment.prod.ts ορίζει apiUrl=https://mock-data-api-nextjs.vercel.app. Το angular.json αντικαθιστά το development environment στο production build, επομένως login και business requests δεν πηγαίνουν στο Spring backend. Υπάρχουν και placeholder Stripe/Viva keys. Το επιτυχές build δεν αποτελεί έτοιμο deployment.',
 'Ρύθμιση πραγματικού API origin και public payment keys ανά περιβάλλον, κατά προτίμηση runtime configuration. Smoke test production build με το πραγματικό API και ρητό έλεγχο του τελικού request URL.',
 [ref(FRONT,'src/environments/environment.prod.ts','apiUrl:'),ref(FRONT,'angular.json','"fileReplacements"')]),
 finding('FE-02','P1','Λανθασμένο gateway στη Viva πληρωμή',
 'Η επιλογή Viva στο PaymentModalComponent στέλνει gateway: viva. Το backend κάνει toUpperCase και δέχεται μόνο VIVA_WALLET. Καταλήγει στο Unsupported gateway: viva, πριν δημιουργηθεί checkout order.',
 'Κοινό enum/API contract και αποστολή VIVA_WALLET. Contract test που χρησιμοποιεί ακριβώς το payload του UI και ελέγχει επιτυχή δημιουργία checkout.',
 [ref(FRONT,F+'demo/application/dashboard/default/payment-modal/payment-modal.component.ts',"gateway: 'viva'"),ref(ROOT,B+'payment/PaymentController.java','case "VIVA_WALLET"')]),
 finding('FE-03','P1','Η offline πληρωμή εμφανίζει επιτυχία χωρίς σωστή καταχώριση',
 'Το dashboard στέλνει { amount } στο PATCH allocation pay. Ο server δεν διαβάζει body και σημειώνει μόνο το allocation ως paid. Για μερική πληρωμή 10 EUR σε οφειλή 100 EUR, το UI μπορεί να δείξει done χωρίς paidAmount ή σωστή εγγραφή πληρωμής. Το πρόβλημα είναι κοινό API contract με το BE-06.',
 'Ενιαίο endpoint πληρωμής με ποσό, ταυτότητα διαμερίσματος και έγκριση διαχειριστή όπου απαιτείται. Το UI να εμφανίζει το επιβεβαιωμένο υπόλοιπο του server.',
 [ref(FRONT,F+'theme/shared/service/userDashboard.service.ts','payAllocation(id:'),ref(FRONT,F+'demo/application/dashboard/default/payment-modal/payment-modal.component.ts','this.dashboardService.payAllocation'),ref(ROOT,B+'commonExpenseAllocation/CommonExpenseAllocationController.java','public ResponseEntity<CommonExpenseAllocation> markAsPaid')]),
 finding('FE-04','P1','Σελίδα επιβεβαίωσης εμφανίζει ξένη ή παλιά πληρωμή',
 'Το PaymentStatusComponent χρησιμοποιεί μόνο statementId και διαλέγει payments[0] από τη λίστα όλων των πληρωμών του statement. Δεν αντιστοιχίζει transaction ID, χρήστη ή apartment. Μια παλιά ή άλλου ενοίκου εγγραφή μπορεί να παρουσιαστεί ως Payment Confirmed για το τρέχον checkout. Όταν η λίστα είναι κενή, εμφανίζει ότι θα ανανεωθεί αυτόματα, αλλά δεν υπάρχει polling. Η ημερομηνία είναι η σημερινή, όχι paymentDate.',
 'Ανάκτηση κατάστασης συγκεκριμένου payment intent/order από ασφαλές endpoint, bounded polling μέχρι την επιβεβαίωση webhook και πραγματική ημερομηνία πληρωμής. Tests για καθυστερημένο webhook και παλιά πληρωμή άλλου διαμερίσματος.',
 [ref(FRONT,F+'demo/application/payment/payment-status/payment-status.component.ts','this.payment = payments[0]')]),
 finding('FE-05','P1','JWT αποστέλλεται σε κάθε HttpClient προορισμό',
 'Το JwtInterceptor είναι ενεργό στο main.ts και προσθέτει Authorization σε κάθε request χωρίς έλεγχο origin. Παρακάμπτει τον περιορισμό API URL του BasicAuthInterceptor. Όποια κλήση HttpClient σταλεί σε τρίτο origin μπορεί να λάβει και το JWT, εφόσον επιτρέπεται το σχετικό CORS request. Δεν διαπιστώθηκε πραγματική διαρροή σε αυτό το review.',
 'Ένας interceptor με σύγκριση URL.origin και επιτρεπόμενων API paths. Tests για backend, assets και τρίτο domain, επιβεβαιώνοντας ότι μόνο το backend λαμβάνει token.',
 [ref(FRONT,F+'theme/shared/components/_helpers/error.interceptor.ts','if (token)'),ref(FRONT,'src/main.ts','useClass: JwtInterceptor')]),
 finding('FE-06','P2','Παλιό token άλλου λογαριασμού παραμένει μετά από login',
 'Το login αποθηκεύει token στο επιλεγμένο storage αλλά δεν καθαρίζει το token στο άλλο. Το persistCurrentUser καθαρίζει μόνο currentUser. Αν υπάρχει παλιό localStorage token και γίνει session login ως άλλος χρήστης, οι interceptors προτιμούν το παλιό token ενώ το UI δείχνει τον νέο χρήστη. Το profile-image updater επίσης αγνοεί χρήστες που βρίσκονται μόνο στο sessionStorage.',
 'Ατομική αλλαγή session, καθαρισμός token/currentUser και στα δύο storages πριν από αποθήκευση. Ενιαίο active storage για profile updates. Test αλλαγής λογαριασμού με rememberMe true -> false.',
 [ref(FRONT,F+'theme/shared/service/authentication.service.ts',"storage.setItem('token'"),ref(FRONT,F+'theme/shared/service/authentication.service.ts','updateCurrentUserProfileImage')]),
 finding('FE-07','P1','Test runners δεν διαχωρίζουν unit και browser tests',
 'Το Playwright testMatch=**/*.spec.ts συλλέγει και Jasmine/Angular unit specs. Το test --list αναπαράγει JIT compiler errors και describe is not defined, με 0 συλλεγμένα tests. Στο angular.json δεν υπάρχει test target για το npm test. Το lint επίσης απέτυχε με 12 errors και 1 warning.',
 'Διαχωρισμός e2e/unit καταλόγων ή explicit testMatch/testIgnore και υποστηριζόμενο unit test target. Στο CI: lint, unit και Playwright ως ξεχωριστά υποχρεωτικά checks.',
 [ref(FRONT,'playwright.config.ts','testMatch:'),ref(FRONT,F+'theme/shared/models/apartment-order.spec.ts',"describe('compareApartments'"),ref(FRONT,'angular.json','"architect"')]),
]

proposals = [
 ('1. Συμφωνία πληρωμών και υπενθυμίσεις οφειλών', 'Dashboard ανεξόφλητων ανά διαμέρισμα, εισαγωγή κινήσεων τράπεζας, αντιστοίχιση με οφειλές και ελεγχόμενες υπενθυμίσεις με ιστορικό αποστολής. Backend: ledger, import preview και reconciliation API. Frontend: ουρά μη αντιστοιχισμένων κινήσεων και approval. Κριτήριο MVP: καμία διπλή καταχώριση στο ίδιο import, χειροκίνητη αναίρεση και ορθό υπόλοιπο. Προϋπόθεση: διόρθωση authorization και ακεραιότητας πληρωμών.'),
 ('2. Προληπτική συντήρηση εξοπλισμού', 'Μητρώο ανελκυστήρα, λέβητα και πυρασφάλειας με επόμενη συντήρηση, ανάθεση σε επαγγελματία, προσφορά, φωτογραφίες και παραστατικά. Αξιοποιεί helpdesk, calendar, professionals και documents. Κριτήριο MVP: επαναλαμβανόμενη εργασία δημιουργεί ένα ticket και η ολοκλήρωση ενημερώνει ιστορικό κόστους και επόμενη ημερομηνία.'),
 ('3. Ψηφιακή γενική συνέλευση', 'Ημερήσια διάταξη, προσκλήσεις, συμμετοχές, εξουσιοδοτήσεις, ψηφοφορία με χιλιοστά και εξαγωγή πρακτικών PDF. Αξιοποιεί polls, building members και permissions. Κριτήριο MVP: snapshot δικαιωμάτων/χιλιοστών κατά το άνοιγμα, μία ψήφος ανά δικαίωμα και αμετάβλητο audit αποτελέσματος. Οι κανόνες απαρτίας και εκπροσώπησης θα οριστούν με τους χρήστες πριν την υλοποίηση.')
]

def build(kind, root, output, findings, commit):
    is_back = kind == 'Backend'
    pages = []
    pages.append(('Review | 30 Οκτωβρίου 2026', [
        ('title',f'Building Manager\n{kind} Analysis Update'),
        ('body',f'Βάση review: commit {commit} και το υπάρχον working tree. Αναφορά στα ελληνικά. Προτεραιότητες: P0 κρίσιμο, P1 υψηλό, P2 κανονικό. Η αξιολόγηση αφορά τον τοπικό κώδικα, όχι πιστοποίηση production λειτουργίας.'),
        ('body',f'Καταγράφηκαν {len(findings)} ευρήματα σε αυτό το repo. Τα BE-06/FE-03 και BE-04/FE-04 περιγράφουν διαφορετικές πλευρές κοινών ροών και δεν πρέπει να μετρηθούν ως ανεξάρτητες αποτυχίες συστήματος.'),
        ('body','Συμπέρασμα: υπάρχει εκτεταμένος λειτουργικός κώδικας για διαχείριση πολυκατοικιών, όμως δεν τεκμηριώνεται ακόμη ασφαλής ετοιμότητα παραγωγής. Προηγούνται authorization, ιδιωτικότητα εγγράφων, συνέπεια πληρωμών και αποκατάσταση των test runners.'),
        ('body','Η νέα αναφορά προηγείται και υπερισχύει παλιών συμπερασμάτων για testing, ασφάλεια και readiness. Οι αρχικές 29 σελίδες του Ιουλίου 2026 διατηρούνται στο τέλος ως ιστορικό παράρτημα και δεν επανεπικυρώνονται.'),
    ]))
    checks = ([
        ('PASS','mvnw.cmd "-Dmaven.test.skip=true" package -q: exit 0. Δημιουργήθηκε executable JAR. Δεν εκτελούνται ούτε μεταγλωττίζονται tests με αυτή την επιλογή.'),
        ('FAIL','mvnw.cmd test -q: exit 1 στο testCompile. Constructor mismatch στο CommonExpenseStatementServiceTest:65. Καμία επιτυχία test δεν αποδίδεται σε αυτό το run.'),
    ] if is_back else [
        ('PASS','node node_modules/@angular/cli/bin/ng.js build --configuration production: exit 0. Initial bundle 3.15 MB, estimated transfer 597.74 kB. Sass deprecations και CommonJS warning παραμένουν.'),
        ('FAIL','node node_modules/@angular/cli/bin/ng.js lint: exit 1, 12 errors και 1 warning.'),
        ('FAIL','Playwright test --list: exit 1, JIT compiler errors και describe is not defined. Δεν τεκμηριώθηκε επιτυχής browser test.'),
        ('LIMIT','Η πλήρης browser εκτέλεση διακόπηκε αφού δεν παρήγαγε αποτελέσματα. Το ανεξάρτητο --list εντόπισε το πρόβλημα συλλογής. ng test --watch=false επίσης απορρίφθηκε ως Unknown argument: watch· δεν υπάρχει test target στο angular.json.'),
    ])
    pages.append(('Τι επαληθεύτηκε', [('body',f'{status}: {message}') for status,message in checks] + [
        ('heading','Όρια και συνθήκες review'),
        ('body','Δεν έγινε σύνδεση σε production, χρέωση κάρτας ή μεταβολή επιχειρησιακών δεδομένων. Δεν εκκινήθηκε backend με live PostgreSQL/SMTP. Δεν αποδίδονται runtime επιτυχίες σε login, αποστολή email, uploads ή payments. Τα ευρήματα πρόσβασης και πληρωμών προκύπτουν από έλεγχο των συγκεκριμένων controller/service/filter διαδρομών και απαιτούν regression tests μετά τη διόρθωση.'),
        ('body','Υπήρχαν τοπικές αλλαγές πριν το review: στο backend log και νέο upload· στο frontend statement-dashboard, buildings και building-photos.spec.ts. Διατηρήθηκαν. Το review δεν άλλαξε application code ή tests. Οι αρχικές αποτυχημένες κλήσεις npm/Maven λόγω PowerShell argument forwarding επαναλήφθηκαν με άμεσο CLI/quoted argument και δεν μετρήθηκαν ως bugs του project.'),
    ]))
    pages.append(('Χάρτης εφαρμογής και λειτουργικότητας', [
        ('heading','Αρχιτεκτονική που υπάρχει στον κώδικα'),
        ('body', 'Spring Boot / Java, JPA repositories, PostgreSQL configuration, JWT security και καθολικό /api/v1 prefix. Υπάρχουν controllers/services για users/roles, buildings/apartments/members, bulk imports, κοινόχρηστα/allocations/PDF, πληρωμές, dashboards, chat, helpdesk, calendar/polls, professionals, notifications και pricing/contact.' if is_back else 'Angular εφαρμογή με standalone components, lazy routes, HttpClient services και Bootstrap UI. Υπάρχουν public landing/pricing/contact, authentication, dashboards ανά ρόλο, πολυκατοικίες/διαμερίσματα και imports, κοινόχρηστα, πληρωμές, professionals, helpdesk, chat, calendar/polls και profile.'),
        ('heading','Πώς ερμηνεύεται το «δουλεύει»'),
        ('body','Build/package: επαληθεύτηκε. Business λειτουργία: υλοποιημένη σε κώδικα, αλλά δεν επανελέγχθηκε end-to-end με πραγματικό backend και βάση. Υπάρχοντα test files αποτελούν υποδομή, όχι ένδειξη ότι πέρασαν. Οι ροές πληρωμών έχουν συγκεκριμένα προβλήματα contract και consistency που καταγράφονται παρακάτω.'),
        ('heading','Θετικά σημεία που διατηρούμε'),
        ('body','Το BuildingPermissionService δίνει κοινή βάση ελέγχων πρόσβασης, αλλά χρειάζεται συστηματική εφαρμογή. Το document service ελέγχει view/manage στη λίστα/μεταφόρτωση και διαθέτει rollback cleanup και διαγραφή φυσικού αρχείου μετά το commit. Η υπογραφή Stripe ελέγχεται στον handler. Υπάρχουν bulk workflows και tests για βασικές επιχειρησιακές περιοχές.' if is_back else 'Το production build ολοκληρώνεται εντός του ορίου initial bundle. Τα services έχουν σαφή αντιστοίχιση με API modules και υπάρχει σημαντικό σύνολο browser specs για authentication, professionals, helpdesk και bulk flows. Οι τοπικές αλλαγές στις φωτογραφίες συνοδεύονται από νέο spec, που δεν θεωρείται εκτελεσμένο στο παρόν review.'),
        ('heading','Επόμενο λειτουργικό smoke test'),
        ('body','Με απομονωμένη βάση και δοκιμαστικούς λογαριασμούς: registration/activation/login, δημιουργία κτιρίου και imports, έκδοση κοινοχρήστων, μερική/πλήρης πληρωμή, callback retry, download εγγράφου από μέλος και ξένο χρήστη. Χρειάζεται κοινό report frontend-backend με request IDs και αναμενόμενα υπόλοιπα.'),
    ]))
    # Two findings per page make all source references and acceptance criteria readable.
    for offset in range(0,len(findings),2):
        blocks=[]
        for f in findings[offset:offset+2]:
            blocks.extend([('heading',f"{f['code']} [{f['priority']}] {f['title']}"),('body',f['behavior']),('body','Διόρθωση / επαλήθευση: '+f['fix']),('ref','Πηγές: '+' | '.join(f['refs']))])
        pages.append(('Ευρήματα και τρόπος επαλήθευσης',blocks))
    pages.append(('Προτεραιότητες και 3 επόμενες δυνατότητες', [
        ('body','Σειρά εργασιών: (1) περιορισμός αλλαγής ρόλων και διακτιριακής πρόσβασης, (2) ιδιωτικά downloads και κατάσταση ενεργού JWT, (3) ενιαίο payment ledger, ασφαλή/idempotent callbacks και κοινό API contract, (4) production configuration και πράσινα test/lint checks. Μετά ακολουθούν οι παρακάτω προσθήκες προϊόντος.'),
    ] + sum(([('heading',title),('body',body)] for title,body in proposals), [])))
    pages.append(('Ιστορικό παράρτημα | Ιούλιος 2026', [
        ('title','Αρχική ανάλυση'),
        ('body','Ακολουθούν αυτούσιες οι 29 σελίδες της προηγούμενης ανάλυσης για αναφορά στην αρχιτεκτονική και στο ιστορικό προϊόντος. Οι ημερομηνίες, εκτιμήσεις testing και ισχυρισμοί readiness σε αυτές τις σελίδες αφορούν την παλιά έκδοση.'),
        ('body','Για την κατάσταση στις 30 Οκτωβρίου 2026, χρησιμοποιήστε τις προηγούμενες σελίδες αυτού του update. Η αρίθμηση του παλιού παραρτήματος διατηρείται όπως ήταν.'),
    ]))
    buf=BytesIO()
    def footer(c,doc):
        c.setFont('Review',8); c.setFillColor(HexColor('#506070'))
        c.drawString(43,25,f'{kind} | Review 2026-10-30 | Internal')
        c.drawRightString(552,25,f'{doc.page}')
        c.setStrokeColor(HexColor('#d6e0e7')); c.line(43,40,552,40)
    flow=[]; md=[f'# {kind} analysis review - 2026-10-30',f'Commit: {commit} + existing working tree','']
    for i,(title,blocks) in enumerate(pages):
        if i: flow.append(PageBreak())
        flow.append(Paragraph(escape(title),styles['HGR'])); flow.append(Spacer(1,12))
        md.extend(['## '+title,''])
        for typ,value in blocks:
            style={'title':'TitleGR','heading':'HGR','body':'BodyGR','ref':'RefGR'}[typ]
            flow.append(Paragraph(escape(value).replace('\n','<br/>'),styles[style]))
            md.extend([('### ' if typ=='heading' else '')+value,''])
    doc=SimpleDocTemplate(buf,pagesize=(595.28,841.89),rightMargin=43,leftMargin=43,topMargin=45,bottomMargin=55,title=f'{kind} Analysis Review - 2026-10-30',author='Repository Review')
    doc.build(flow,onFirstPage=footer,onLaterPages=footer)
    original=PdfReader(output)
    writer=PdfWriter()
    supplement=PdfReader(BytesIO(buf.getvalue()))
    writer.append(supplement)
    writer.append(original,pages=(len(original.pages)-29,len(original.pages)))
    writer.add_metadata({'/Title':f'{kind} Project Analysis - Updated 2026-10-30','/Author':'Repository Review','/Subject':'Dated review followed by historical July 2026 analysis'})
    with output.open('wb') as stream: writer.write(stream)
    docs=root/'docs'; docs.mkdir(exist_ok=True)
    (docs/'review-2026-10-30.md').write_text('\n'.join(md),encoding='utf-8')
    (TMP/f'{kind.lower()}-supplement.pdf').write_bytes(buf.getvalue())
    print(json.dumps({'output':str(output),'supplement_pages':len(supplement.pages),'total_pages':len(writer.pages)},ensure_ascii=False))

if __name__ == '__main__':
    build('Backend',ROOT,ROOT/'Building-Manager-Network-Project-Analysis.pdf',backend,'638a358')
    build('Frontend',FRONT,FRONT/'project-analysis.pdf',frontend,'8f29553')
