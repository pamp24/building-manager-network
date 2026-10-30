package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentOrder;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import com.buildingmanager.commonExpenseItem.ExpenseCategory;
import com.buildingmanager.company.Company;
import com.buildingmanager.fileStorage.FileStorageException;
import com.buildingmanager.fileStorage.FileStorageProperties;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatementPdfService {

    private static final Locale GREEK = Locale.forLanguageTag("el-GR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d/M/yyyy", GREEK);
    private static final List<ExpenseCategory> CATEGORY_ORDER = Arrays.asList(
            ExpenseCategory.COMMON,
            ExpenseCategory.HEATING,
            ExpenseCategory.ELEVATOR,
            ExpenseCategory.EQUAL,
            ExpenseCategory.BOILER,
            ExpenseCategory.SPECIAL,
            ExpenseCategory.OWNERS,
            ExpenseCategory.OTHER
    );

    private final ApartmentRepository apartmentRepository;
    private final CommonExpenseAllocationRepository allocationRepository;
    private final BuildingRepository buildingRepository;
    private final FileStorageProperties fileStorageProperties;
    private final TemplateEngine templateEngine;

    public Path ensurePdfExists(CommonExpenseStatement statement) {
        Path pdfPath = resolvePdfPath(statement);
        if (Files.exists(pdfPath)) {
            return pdfPath;
        }

        writePdf(statement, pdfPath);
        return pdfPath;
    }

    public Path ensureApartmentPdfExists(CommonExpenseStatement statement, Integer apartmentId) {
        Path pdfPath = resolveApartmentPdfPath(statement, apartmentId);
        if (Files.exists(pdfPath)) {
            return pdfPath;
        }

        generateApartmentPdf(statement, apartmentId);
        return pdfPath;
    }

    public void regeneratePdf(CommonExpenseStatement statement) {
        writePdf(statement, resolvePdfPath(statement));
    }

    public void regenerateAllPdfs(CommonExpenseStatement statement) {
        regeneratePdf(statement);
        Integer buildingId = statement.getBuilding().getId();
        for (Apartment apartment : apartmentRepository.findAllByBuilding_Id(buildingId)) {
            generateApartmentPdf(statement, apartment.getId());
        }
    }

    public void regenerateApartmentPdf(CommonExpenseStatement statement, Integer apartmentId) {
        generateApartmentPdf(statement, apartmentId);
    }

    public void deletePdfIfExists(CommonExpenseStatement statement) {
        try {
            Files.deleteIfExists(resolvePdfPath(statement));
        } catch (IOException ignored) {
            // best effort cleanup only
        }
    }

    public void deleteAllPdfs(CommonExpenseStatement statement) {
        deletePdfIfExists(statement);
        Path apartmentsDir = resolveStatementDirectory(statement).resolve("apartments");
        if (!Files.exists(apartmentsDir)) {
            return;
        }

        try {
            Files.list(apartmentsDir).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
            Files.deleteIfExists(apartmentsDir);
        } catch (IOException ignored) {
        }
    }

    private void writePdf(CommonExpenseStatement statement, Path pdfPath) {
        try {
            Files.createDirectories(pdfPath.getParent());
            Files.write(
                    pdfPath,
                    renderPdf(statement),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw new FileStorageException("Αποτυχία αποθήκευσης του PDF κοινοχρήστων.", exception);
        }
    }

    private byte[] renderPdf(CommonExpenseStatement statement) {
        Integer buildingId = statement.getBuilding().getId();
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalStateException("Δεν βρέθηκε το κτίριο του παραστατικού."));
        List<Apartment> apartments = apartmentRepository.findAllByBuilding_Id(buildingId)
                .stream()
                .sorted(ApartmentOrder.COMPARATOR)
                .toList();
        List<CommonExpenseAllocation> allocations = allocationRepository.findAllByStatement_Id(statement.getId());

        Context context = new Context(GREEK);
        context.setVariable("logoDataUri", loadLogoDataUri());
        context.setVariable("appName", "Building Manager Network");
        context.setVariable("appSubtitle", "Εκδοση Κοινοχρήστων");
        context.setVariable("issuerName", issuerName(building));
        context.setVariable("issuerAddress", issuerAddress(building));
        context.setVariable("issuerCity", issuerCity(building));
        context.setVariable("issuerPhone", issuerPhone(building));
        context.setVariable("statementCode", statement.getCode());
        context.setVariable("statementDate", statement.getStartDate() != null ? statement.getStartDate().format(DATE_FORMATTER) : "-");
        context.setVariable("statementMonth", formatMonth(statement.getMonth()));
        context.setVariable("managerName", building.getManager() != null ? building.getManager().getFullName() : "-");
        context.setVariable("buildingAddress", buildingAddress(building));
        context.setVariable("buildingCity", building.getCity());
        context.setVariable("description", statement.getDescription());
        context.setVariable("categorySections", buildCategorySections(statement));
        context.setVariable("apartmentRows", buildApartmentRows(apartments, allocations));
        context.setVariable("commonPercentTotal", formatNumber(apartments.stream().map(Apartment::getCommonPercent).reduce(0d, Double::sum)));
        context.setVariable("elevatorPercentTotal", formatNumber(apartments.stream().map(Apartment::getElevatorPercent).reduce(0d, Double::sum)));
        context.setVariable("heatingPercentTotal", formatNumber(apartments.stream().map(Apartment::getHeatingPercent).reduce(0d, Double::sum)));
        context.setVariable("categoryTotals", buildCategoryTotals(apartments, allocations));
        context.setVariable("grandTotal", formatAmount(statement.getTotal()));

        String html = templateEngine.process("statement-pdf", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.toStream(outputStream);
            builder.withHtmlContent(html, null);
            registerFonts(builder);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Αποτυχία δημιουργίας PDF κοινοχρήστων.", exception);
        }
    }

    private void generateApartmentPdf(CommonExpenseStatement statement, Integer apartmentId) {
        Path pdfPath = resolveApartmentPdfPath(statement, apartmentId);
        try {
            Files.createDirectories(pdfPath.getParent());
            Files.write(
                    pdfPath,
                    renderApartmentPdf(statement, apartmentId),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw new FileStorageException("Αποτυχία αποθήκευσης του PDF ειδοποιητηρίου διαμερίσματος.", exception);
        }
    }

    private byte[] renderApartmentPdf(CommonExpenseStatement statement, Integer apartmentId) {
        Integer buildingId = statement.getBuilding().getId();
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalStateException("Δεν βρέθηκε το κτίριο του παραστατικού."));
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new IllegalStateException("Δεν βρέθηκε το διαμέρισμα."));
        List<CommonExpenseAllocation> allocations = allocationRepository.findByStatementIdAndApartmentId(statement.getId(), apartmentId);

        Context context = new Context(GREEK);
        context.setVariable("logoDataUri", loadLogoDataUri());
        context.setVariable("issuerName", issuerName(building));
        context.setVariable("issuerAddress", issuerAddress(building));
        context.setVariable("issuerCity", issuerCity(building));
        context.setVariable("issuerPhone", issuerPhone(building));
        context.setVariable("statementMonth", formatMonth(statement.getMonth()));
        context.setVariable("statementDate", statement.getStartDate() != null ? statement.getStartDate().format(DATE_FORMATTER) : "-");
        context.setVariable("statementCode", statement.getCode());
        context.setVariable("apartmentNoticeCode", resolveApartmentNoticeCode(statement, apartment));
        context.setVariable("buildingAddress", buildingAddress(building));
        context.setVariable("buildingCity", building.getCity());
        context.setVariable("tenantApartmentName", apartmentDisplayName(apartment) + " - " + apartmentDisplayLabel(apartment));
        context.setVariable("buildingGrandTotal", formatAmountOrZero(statement.getTotal()));
        context.setVariable("apartmentTotal", formatAmountOrZero(allocations.stream().map(CommonExpenseAllocation::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        context.setVariable("managerApartmentName", (building.getManager() != null ? building.getManager().getFullName() : "-") + " - " + apartmentDisplayLabel(apartment));
        context.setVariable("description", statement.getDescription());
        context.setVariable("paymentCode", buildPaymentCode(statement, apartment));
        context.setVariable("collectionDateText", statement.getStartDate() != null ? statement.getStartDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", GREEK)) : "-");
        context.setVariable("noticeSections", buildApartmentNoticeSections(statement, apartment, allocations));
        context.setVariable("monthExpenseTotal", formatAmountOrZero(allocations.stream().map(CommonExpenseAllocation::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));

        String html = templateEngine.process("statement-apartment-pdf", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.toStream(outputStream);
            builder.withHtmlContent(html, null);
            registerFonts(builder);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Αποτυχία δημιουργίας PDF ειδοποιητηρίου διαμερίσματος.", exception);
        }
    }

    private void registerFonts(PdfRendererBuilder builder) {
        List<String> candidates = List.of(
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/arialbd.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
        );

        for (String candidate : candidates) {
            File fontFile = new File(candidate);
            if (fontFile.exists()) {
                builder.useFont(fontFile, "AppSans");
            }
        }
    }

    private Path resolvePdfPath(CommonExpenseStatement statement) {
        return resolveStatementDirectory(statement)
                .resolve("statement_" + statement.getId() + "_" + sanitizeFileName(statement.getCode()) + ".pdf")
                .normalize();
    }

    private Path resolveApartmentPdfPath(CommonExpenseStatement statement, Integer apartmentId) {
        return resolveStatementDirectory(statement)
                .resolve("apartments")
                .resolve("apartment_" + apartmentId + "_" + sanitizeFileName(statement.getCode()) + ".pdf")
                .normalize();
    }

    private Path resolveStatementDirectory(CommonExpenseStatement statement) {
        String rootDirectory = fileStorageProperties.getRootDirectory();
        return Paths.get(rootDirectory)
                .toAbsolutePath()
                .normalize()
                .resolve("buildings")
                .resolve(String.valueOf(statement.getBuilding().getId()))
                .resolve("statements")
                .resolve(String.valueOf(statement.getId()))
                .normalize();
    }

    private String sanitizeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "document";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String loadLogoDataUri() {
        try {
            byte[] bytes = new ClassPathResource("pdf/oikio-logo.png").getInputStream().readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("Αποτυχία φόρτωσης του logo για το PDF.", exception);
        }
    }

    private String issuerName(Building building) {
        Company company = building.getCompany();
        if (company != null && company.getCompanyName() != null && !company.getCompanyName().isBlank()) {
            return company.getCompanyName();
        }
        if (building.getManager() != null && building.getManager().getFullName() != null) {
            return building.getManager().getFullName();
        }
        return building.getName();
    }

    private String issuerAddress(Building building) {
        Company company = building.getCompany();
        if (company != null) {
            return joinNonBlank(company.getAddress(), company.getAddressNumber());
        }
        return joinNonBlank(building.getStreet1(), building.getStNumber1());
    }

    private String issuerCity(Building building) {
        Company company = building.getCompany();
        if (company != null) {
            return joinNonBlank(company.getCity(), company.getPostalCode());
        }
        return joinNonBlank(building.getCity(), building.getPostalCode());
    }

    private String issuerPhone(Building building) {
        Company company = building.getCompany();
        if (company != null && company.getPhone() != null) {
            return company.getPhone();
        }
        return building.getManager() != null ? building.getManager().getPhoneNumber() : "";
    }

    private String buildingAddress(Building building) {
        return joinNonBlank(joinNonBlank(building.getStreet1(), building.getStNumber1()), joinNonBlank(building.getStreet2(), building.getStNumber2()));
    }

    private String joinNonBlank(String first, String second) {
        String a = first == null ? "" : first.trim();
        String b = second == null ? "" : second.trim();
        if (a.isBlank()) return b;
        if (b.isBlank()) return a;
        return a + " " + b;
    }

    private String formatMonth(String month) {
        if (month == null || month.isBlank()) {
            return "-";
        }
        String[] parts = month.split("-");
        if (parts.length != 2) {
            return month;
        }
        int monthNumber = Integer.parseInt(parts[1]);
        String[] greekMonths = {
                "Ιανουάριος", "Φεβρουάριος", "Μάρτιος", "Απρίλιος", "Μάιος", "Ιούνιος",
                "Ιούλιος", "Αύγουστος", "Σεπτέμβριος", "Οκτώβριος", "Νοέμβριος", "Δεκέμβριος"
        };
        return greekMonths[monthNumber - 1] + " " + parts[0];
    }

    private List<PdfCategorySection> buildCategorySections(CommonExpenseStatement statement) {
        List<PdfCategorySection> sections = new ArrayList<>();
        for (ExpenseCategory category : CATEGORY_ORDER) {
            List<CommonExpenseItem> items = statement.getItems().stream()
                    .filter(item -> item.getCategory() == category)
                    .toList();
            List<PdfExpenseRow> rows = items.stream()
                    .map(item -> new PdfExpenseRow(item.getDescriptionItem(), formatAmount(item.getPrice())))
                    .toList();
            BigDecimal total = items.stream()
                    .map(CommonExpenseItem::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            sections.add(new PdfCategorySection(labelForCategory(category), rows, formatAmount(total), Math.max(0, 4 - rows.size())));
        }
        return sections;
    }

    private Map<ExpenseCategory, String> buildCategoryTotals(List<Apartment> apartments, List<CommonExpenseAllocation> allocations) {
        EnumMap<ExpenseCategory, BigDecimal> totals = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : CATEGORY_ORDER) {
            totals.put(category, BigDecimal.ZERO);
        }

        for (CommonExpenseAllocation allocation : allocations) {
            ExpenseCategory category = allocation.getItem() != null ? allocation.getItem().getCategory() : ExpenseCategory.OTHER;
            totals.put(category, totals.getOrDefault(category, BigDecimal.ZERO).add(zero(allocation.getAmount())));
        }

        EnumMap<ExpenseCategory, String> formatted = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : CATEGORY_ORDER) {
            formatted.put(category, formatAmount(totals.get(category)));
        }
        return formatted;
    }

    private List<PdfApartmentRow> buildApartmentRows(List<Apartment> apartments, List<CommonExpenseAllocation> allocations) {
        Map<Integer, EnumMap<ExpenseCategory, BigDecimal>> byApartment = new java.util.HashMap<>();

        for (CommonExpenseAllocation allocation : allocations) {
            Integer apartmentId = allocation.getApartment() != null ? allocation.getApartment().getId() : null;
            if (apartmentId == null) {
                continue;
            }

            EnumMap<ExpenseCategory, BigDecimal> categoryAmounts = byApartment.computeIfAbsent(apartmentId, ignored -> {
                EnumMap<ExpenseCategory, BigDecimal> initial = new EnumMap<>(ExpenseCategory.class);
                for (ExpenseCategory category : CATEGORY_ORDER) {
                    initial.put(category, BigDecimal.ZERO);
                }
                return initial;
            });

            ExpenseCategory category = allocation.getItem() != null ? allocation.getItem().getCategory() : ExpenseCategory.OTHER;
            categoryAmounts.put(category, categoryAmounts.get(category).add(zero(allocation.getAmount())));
        }

        List<PdfApartmentRow> rows = new ArrayList<>();
        for (int index = 0; index < apartments.size(); index++) {
            Apartment apartment = apartments.get(index);
            EnumMap<ExpenseCategory, BigDecimal> amounts = byApartment.getOrDefault(apartment.getId(), emptyCategoryMap());
            BigDecimal total = amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            rows.add(new PdfApartmentRow(
                    String.format("%03d", index + 1),
                    apartmentDisplayName(apartment),
                    apartmentDisplayLabel(apartment),
                    formatNumber(apartment.getCommonPercent()),
                    formatNumber(apartment.getElevatorPercent()),
                    formatNumber(apartment.getHeatingPercent()),
                    formatAmount(amounts.get(ExpenseCategory.COMMON)),
                    formatAmount(amounts.get(ExpenseCategory.ELEVATOR)),
                    formatAmount(amounts.get(ExpenseCategory.HEATING)),
                    formatAmount(amounts.get(ExpenseCategory.EQUAL)),
                    formatAmount(amounts.get(ExpenseCategory.BOILER)),
                    formatAmount(amounts.get(ExpenseCategory.SPECIAL)),
                    formatAmount(amounts.get(ExpenseCategory.OWNERS)),
                    formatAmount(amounts.get(ExpenseCategory.OTHER)),
                    formatAmount(total)
            ));
        }

        return rows;
    }

    private EnumMap<ExpenseCategory, BigDecimal> emptyCategoryMap() {
        EnumMap<ExpenseCategory, BigDecimal> map = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : CATEGORY_ORDER) {
            map.put(category, BigDecimal.ZERO);
        }
        return map;
    }

    private String apartmentDisplayName(Apartment apartment) {
        if (apartment.getResident() != null && apartment.getResident().getFullName() != null && !apartment.getResident().getFullName().isBlank()) {
            return apartment.getResident().getFullName();
        }
        return joinNonBlank(apartment.getOwnerLastName(), apartment.getOwnerFirstName());
    }

    private String apartmentDisplayLabel(Apartment apartment) {
        return joinNonBlank(apartment.getFloor(), apartment.getNumber());
    }

    private String resolveApartmentNoticeCode(CommonExpenseStatement statement, Apartment apartment) {
        Integer apartmentIndex = apartmentRepository.findAllByBuilding_Id(statement.getBuilding().getId()).stream()
                .sorted(Comparator.comparing(Apartment::getFloor, Comparator.nullsLast(String::compareTo))
                        .thenComparing(Apartment::getNumber, Comparator.nullsLast(String::compareTo)))
                .map(Apartment::getId)
                .toList()
                .indexOf(apartment.getId());
        if (apartmentIndex < 0) {
            return String.format("%03d", apartment.getId());
        }
        return String.format("%03d", apartmentIndex + 1);
    }

    private String buildPaymentCode(CommonExpenseStatement statement, Apartment apartment) {
        return String.format("%d%06d%05d", statement.getStartDate() != null ? statement.getStartDate().getYear() : 0, statement.getId(), apartment.getId());
    }

    private List<ApartmentNoticeSection> buildApartmentNoticeSections(
            CommonExpenseStatement statement,
            Apartment apartment,
            List<CommonExpenseAllocation> allocations
    ) {
        EnumMap<ExpenseCategory, List<CommonExpenseAllocation>> grouped = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : CATEGORY_ORDER) {
            grouped.put(category, new ArrayList<>());
        }
        for (CommonExpenseAllocation allocation : allocations) {
            ExpenseCategory category = allocation.getItem() != null ? allocation.getItem().getCategory() : ExpenseCategory.OTHER;
            grouped.get(category).add(allocation);
        }

        List<ApartmentNoticeSection> sections = new ArrayList<>();
        for (ExpenseCategory category : CATEGORY_ORDER) {
            List<CommonExpenseAllocation> categoryAllocations = grouped.get(category);
            List<PdfExpenseRow> rows = categoryAllocations.stream()
                    .map(allocation -> new PdfExpenseRow(allocation.getItem().getDescriptionItem(), formatAmount(allocation.getItem().getPrice())))
                    .toList();
            BigDecimal buildingTotal = categoryAllocations.stream()
                    .map(allocation -> allocation.getItem().getPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal apartmentTotal = categoryAllocations.stream()
                    .map(CommonExpenseAllocation::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            sections.add(new ApartmentNoticeSection(
                    labelForCategory(category),
                    rows,
                    formatAmountOrZero(buildingTotal),
                    formatCategoryApartmentPercent(category, apartment),
                    formatNumberOrZero(1000d),
                    formatAmountOrZero(apartmentTotal),
                    Math.max(0, 4 - rows.size())
            ));
        }
        return sections;
    }

    private String formatCategoryApartmentPercent(ExpenseCategory category, Apartment apartment) {
        return switch (category) {
            case COMMON, OWNERS -> formatNumber(apartment.getCommonPercent());
            case ELEVATOR -> formatNumber(apartment.getElevatorPercent());
            case HEATING -> formatNumber(apartment.getHeatingPercent());
            default -> "";
        };
    }

    private String labelForCategory(ExpenseCategory category) {
        return switch (category) {
            case COMMON -> "ΚΟΙΝΟΧΡΗΣΤΑ";
            case HEATING -> "ΘΕΡΜΑΝΣΗ";
            case ELEVATOR -> "ΑΣΑΝΣΕΡ";
            case EQUAL -> "ΔΑΠΑΝΕΣ ΣΕ ΙΣΑ ΜΕΡΗ";
            case BOILER -> "BOILER";
            case SPECIAL -> "ΕΙΔΙΚΕΣ ΔΑΠΑΝΕΣ";
            case OWNERS -> "ΙΔΙΟΚΤΗΤΩΝ";
            case OTHER -> "ΛΟΙΠΕΣ ΔΑΠΑΝΕΣ";
        };
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatAmount(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        if (safe.compareTo(BigDecimal.ZERO) == 0) {
            return "";
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(GREEK);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(safe);
    }

    private String formatAmountOrZero(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(GREEK);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(safe);
    }

    private String formatNumber(Double value) {
        if (value == null || value == 0d) {
            return "";
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(GREEK);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(value);
    }

    private String formatNumberOrZero(Double value) {
        double safe = value == null ? 0d : value;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(GREEK);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(safe);
    }

    public record PdfExpenseRow(String description, String amount) {}

    public record PdfCategorySection(String label, List<PdfExpenseRow> items, String total, int emptyRows) {}

    public record ApartmentNoticeSection(
            String label,
            List<PdfExpenseRow> items,
            String buildingTotal,
            String apartmentPercent,
            String buildingPercent,
            String apartmentCharge,
            int emptyRows
    ) {}

    public record PdfApartmentRow(
            String index,
            String name,
            String apartment,
            String commonPercent,
            String elevatorPercent,
            String heatingPercent,
            String commonAmount,
            String elevatorAmount,
            String heatingAmount,
            String equalAmount,
            String boilerAmount,
            String specialAmount,
            String ownersAmount,
            String otherAmount,
            String total
    ) {}
}
