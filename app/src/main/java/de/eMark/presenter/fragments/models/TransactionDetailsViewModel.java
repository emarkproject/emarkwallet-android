package de.eMark.presenter.fragments.models;

import android.annotation.SuppressLint;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.text.TextUtils;

import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import de.eMark.eMark;
import de.eMark.R;
import de.eMark.presenter.entities.TxItem;
import de.eMark.tools.database.Database;
import de.eMark.tools.database.DigiTransaction;
import de.eMark.tools.manager.BRSharedPrefs;
import de.eMark.tools.manager.TxManager;
import de.eMark.tools.threads.BRExecutor;
import de.eMark.tools.util.BRCurrency;
import de.eMark.tools.util.BRExchange;

public class TransactionDetailsViewModel extends BaseObservable {

    private TxItem item;

    public TransactionDetailsViewModel(TxItem item) {
        this.item = item;
    }

    @Bindable
    public String getCryptoAmount() {
        if (!BRSharedPrefs.getPreferredBTC(eMark.getContext()) && currentFiatAmountEqualsOriginalFiatAmount()) {
            return getRawFiatAmount(item);
        } else {
            boolean received = item.getSent() == 0;
            long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived());
            return BRCurrency.getFormattedCurrencyString(eMark.getContext(), "DEM",
                    BRExchange.getAmountFromSatoshis(eMark.getContext(), "DEM",
                            new BigDecimal(satoshisAmount)));
        }
    }

    public static String getRawFiatAmount(TxItem txItem) {
        boolean received = txItem.getSent() == 0;
        long satoshisAmount = received ? txItem.getReceived() : (txItem.getSent() - txItem.getReceived());
        return BRCurrency.getFormattedCurrencyString(eMark.getContext(),
                BRSharedPrefs.getIso(eMark.getContext()),
                BRExchange.getAmountFromSatoshis(eMark.getContext(),
                        BRSharedPrefs.getIso(eMark.getContext()),
                        new BigDecimal(satoshisAmount)));
    }

    @SuppressLint("StringFormatInvalid")
    @Bindable
    public String getFiatAmount() {
        return String.format(eMark.getContext().getString(R.string.current_amount), getRawFiatAmount(item));
    }

    @SuppressLint("StringFormatInvalid")
    public String getOriginalFiatAmount() {
        DigiTransaction transaction = Database.instance.findTransaction(item.getTxHash());
        if (transaction == null) {
            return String.format(eMark.getContext().getString(R.string.original_amount), "");
        } else {
            return String.format(eMark.getContext().getString(R.string.original_amount),
                    transaction.getTxAmount());
        }
    }

    public boolean currentFiatAmountEqualsOriginalFiatAmount() {
        DigiTransaction transaction = Database.instance.findTransaction(item.getTxHash());
        if (transaction == null) {
            return true;
        }
        boolean received = item.getSent() == 0;
        long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived());
        return transaction.getTxAmount().equals(BRCurrency.getFormattedCurrencyString(eMark.getContext(),
                BRSharedPrefs.getIso(eMark.getContext()),
                BRExchange.getAmountFromSatoshis(eMark.getContext(),
                        BRSharedPrefs.getIso(eMark.getContext()),
                        new BigDecimal(satoshisAmount))));
    }

    @Bindable
    public String getToFrom() {
        return item.getReceived() - item.getSent() < 0 ? eMark.getContext().getString(
                R.string.sent) : eMark.getContext().getString(R.string.received);
    }

    @Bindable
    public boolean getSent() {
        return item.getReceived() - item.getSent() < 0;
    }

    @Bindable
    public String getAddress() {
        return getSent() ? item.getTo()[0] : item.getFrom()[0];
    }

    @Bindable
    public int getSentReceivedIcon() {
        if (getSent()) {
            return R.drawable.transaction_details_sent;
        } else {
            return R.drawable.transaction_details_received;
        }
    }

    @Bindable
    public String getDate() {
        return getFormattedDate(item.getTimeStamp());
    }

    @Bindable
    public String getTime() {
        return getFormattedTime(item.getTimeStamp());
    }

    @Bindable
    public boolean getCompleted() {
        return BRSharedPrefs.getLastBlockHeight(eMark.getContext()) - item.getBlockHeight() + 1
                >= 6;
    }

    @Bindable
    public String getFee() {
        if (item.getSent() == 0) {
            return "";
        }
        String approximateFee = BRCurrency.getFormattedCurrencyString(eMark.getContext(), "DEM",
                BRExchange.getAmountFromSatoshis(eMark.getContext(), "DEM",
                        new BigDecimal(item.getFee())));
        if (TextUtils.isEmpty(approximateFee)) {
            approximateFee = "";
        }
        return eMark.getContext().getString(R.string.Send_fee).replace("%1$s", approximateFee);
    }

    @Bindable
    public String getMemo() {
        return item.metaData != null ? item.metaData.comment : "";
    }

    public void setMemo(String memo) {
        TxMetaData metaData = item.metaData;
        if (metaData == null) {
            item.metaData = new TxMetaData();
        }
        item.metaData.comment = memo;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            KVStoreManager.getInstance().putTxMetaData(eMark.getContext(), item.metaData, item.getTxHash());
            TxManager.getInstance().updateTxList();
        });
    }

    private String getFormattedDate(long timeStamp) {
        Date currentLocalTime = new Date(
                timeStamp == 0 ? System.currentTimeMillis() : timeStamp * 1000);
        Locale current = eMark.getContext().getResources().getConfiguration().locale;
        return DateFormat.getDateInstance(DateFormat.SHORT, current).format(currentLocalTime);
    }

    private String getFormattedTime(long timeStamp) {
        Date currentLocalTime = new Date(
                timeStamp == 0 ? System.currentTimeMillis() : timeStamp * 1000);
        Locale current = eMark.getContext().getResources().getConfiguration().locale;
        return DateFormat.getTimeInstance(DateFormat.MEDIUM, current).format(currentLocalTime);
    }
}
