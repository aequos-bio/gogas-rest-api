package eu.aequos.gogas.mock;

import eu.aequos.gogas.mvc.WithTenant;
import eu.aequos.gogas.persistence.entity.AccountingEntry;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.Year;
import eu.aequos.gogas.persistence.repository.AccountingReasonRepo;
import eu.aequos.gogas.persistence.repository.AccountingRepo;
import eu.aequos.gogas.persistence.repository.YearRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
@Component
@WithTenant("integration-test")
public class MockAccountingData implements MockDataLifeCycle {

    private final AccountingReasonRepo accountingReasonRepo;
    private final AccountingRepo accountingRepo;
    private final YearRepo yearRepo;

    public AccountingEntryReason createAccountingReason(String reasonCode, String description, String sign) {
        AccountingEntryReason reason = new AccountingEntryReason();
        reason.setReasonCode(reasonCode);
        reason.setDescription(description);
        reason.setSign(sign);

        return accountingReasonRepo.save(reason);
    }

    public AccountingEntry createAccountingEntry(User user, String reason, double amount, LocalDate date) {
        return createAccountingEntry(user, reason, "An entry", amount, date);
    }

    public AccountingEntry createAccountingEntry(User user, String reason, String description, double amount, LocalDate date) {
        AccountingEntry entry = new AccountingEntry();
        entry.setUser(user);
        entry.setReason(accountingReasonRepo.findByReasonCode(reason));
        entry.setDescription(description);
        entry.setConfirmed(true);
        entry.setDate(date);
        entry.setAmount(BigDecimal.valueOf(amount));

        if (user.getRoleEnum().isFriend()) {
            entry.setFriendReferralId(user.getFriendReferral().getId());
        }

        return accountingRepo.save(entry);
    }

    public AccountingEntry createAccountingEntryWithOrderId(String orderId, User user) {
        return createAccountingEntryWithOrderId(orderId, user, LocalDate.now());
    }

    public AccountingEntry createAccountingEntryWithOrderId(String orderId, User user, LocalDate date) {
        AccountingEntry entry = new AccountingEntry();
        entry.setUser(user);
        entry.setReason(accountingReasonRepo.findByReasonCode("ORDINE"));
        entry.setDescription("Addebito ordine");
        entry.setOrderId(orderId);
        entry.setConfirmed(true);
        entry.setDate(date);
        entry.setAmount(BigDecimal.valueOf(100.0));

        return accountingRepo.save(entry);
    }

    public void setYearClosed(int year) {
        Year yearEntity = new Year();
        yearEntity.setYear(year);
        yearEntity.setClosed(true);
        yearRepo.save(yearEntity);
    }

    public void deleteAllEntries() {
        accountingRepo.deleteAll();
        yearRepo.deleteAll();
    }

    @Override
    public void init() {}

    @Override
    public void destroy() {
        deleteAllEntries();

        StreamSupport.stream(accountingReasonRepo.findAll().spliterator(), false)
                .filter(reason -> !reason.getReasonCode().equals("ORDINE"))
                .forEach(accountingReasonRepo::delete);
    }
}
