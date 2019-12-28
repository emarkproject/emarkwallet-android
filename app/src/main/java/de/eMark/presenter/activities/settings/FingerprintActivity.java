package de.eMark.presenter.activities.settings;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import java.math.BigDecimal;

import de.eMark.eMark;
import de.eMark.R;
import de.eMark.databinding.ActivityFingerprintBinding;
import de.eMark.presenter.activities.util.BRActivity;
import de.eMark.presenter.interfaces.BRAuthCompletion;
import de.eMark.tools.animation.BRDialog;
import de.eMark.tools.manager.BRSharedPrefs;
import de.eMark.tools.security.AuthManager;
import de.eMark.tools.security.BRKeyStore;
import de.eMark.tools.util.BRCurrency;
import de.eMark.tools.util.BRExchange;
import de.eMark.tools.util.Utils;


public class FingerprintActivity extends BRActivity {
    private static final String TAG = FingerprintActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityFingerprintBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_fingerprint);
        setupToolbar();
        setToolbarTitle(R.string.TouchIdSettings_title_android);

        binding.toggleButton.setChecked(BRSharedPrefs.getUseFingerprint(this));
        binding.limitExchange.setText(getLimitText());
        binding.toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Activity app = FingerprintActivity.this;
            if (isChecked && !Utils.isFingerprintAvailable(app)) {
                Log.e(TAG, "onCheckedChanged: fingerprint not setup");
                BRDialog.showCustomDialog(app,
                        getString(R.string.TouchIdSettings_disabledWarning_title_android),
                        getString(R.string.TouchIdSettings_disabledWarning_body_android),
                        getString(R.string.Button_ok), null,
                        brDialogView -> brDialogView.dismissWithAnimation(), null, null, 0);
                buttonView.setChecked(false);
            } else {
                BRSharedPrefs.putUseFingerprint(app, isChecked);
            }
        });
        SpannableString ss = new SpannableString(
                getString(R.string.TouchIdSettings_customizeText_android));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                AuthManager.getInstance().authPrompt(FingerprintActivity.this, null,
                        getString(R.string.VerifyPin_continueBody), new AuthType(
                                AuthType.Type.SPENDING_LIMIT));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        //start index of the last space (beginning of the last word)
        int indexOfSpace = binding.limitInfo.getText().toString().lastIndexOf(" ");
        // make the whole text clickable if failed to select the last word
        ss.setSpan(clickableSpan, indexOfSpace == -1 ? 0 : indexOfSpace,
                binding.limitInfo.getText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.limitInfo.setText(ss);
        binding.limitInfo.setMovementMethod(LinkMovementMethod.getInstance());
        binding.limitInfo.setHighlightColor(Color.TRANSPARENT);
    }

    private String getLimitText() {
        String iso = BRSharedPrefs.getIso(this);
        //amount in satoshis
        BigDecimal digibyte = new BigDecimal(BRKeyStore.getSpendLimit(this));
        if (digibyte.equals(BigDecimal.ZERO)) {
            return String.format(getString(R.string.TouchIdSettings_spendingLimit),
                    BRCurrency.getFormattedCurrencyString(this, "DEM", digibyte),
                    eMark.getContext().getString(R.string.no_limit));
        }
        BigDecimal curAmount = BRExchange.getAmountFromSatoshis(this, iso, digibyte.multiply(new BigDecimal(1000000)));
        //formatted string for the label
        return String.format(getString(R.string.TouchIdSettings_spendingLimit),
                BRCurrency.getFormattedCurrencyString(this, "DEM", digibyte),
                BRCurrency.getFormattedCurrencyString(this, iso, curAmount));
    }

    @Override
    public void onComplete(AuthType authType) {
        switch(authType.type) {
            case SPENDING_LIMIT:
                Intent intent = new Intent(FingerprintActivity.this,
                        SpendLimitActivity.class);
                overridePendingTransition(R.anim.enter_from_right,
                        R.anim.exit_to_left);
                startActivity(intent);
                finish();
                break;
        }
    }

    @Override
    public void onCancel(AuthType type) {

    }
}