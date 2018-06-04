package test.socero.paytest;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputWidget;

public class MainActivity extends AppCompatActivity {

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 101;
    public final static int WHITE = 0xFFFFFFFF;
    public final static int BLACK = 0xFF000000;
    public final static int WIDTH = 400;
    public final static int HEIGHT = 400;
    Button buyTicket;
    Context context = this;
    Card card;
    public static final String PUBLISHABLE_KEY = "pk_test_1Tqu2RRf2owV3RCWuHV0whMY";

    PaymentsClient paymentsClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = (ImageView) findViewById(R.id.qrCode);
        try {
            Bitmap bitmap = encodeAsBitmap("Hello World\n Mahrus Kazi");
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        paymentsClient =
                Wallet.getPaymentsClient(this,
                        new Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                                .build());

        //isReadyToPay();

        final LayoutInflater inflater = this.getLayoutInflater();
        buyTicket = findViewById(R.id.button);
        buyTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int ticketPriceNum = 70;
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(context);
                }
                builder.setTitle("Buy Tickets");
                builder.setMessage("Purchase tickets for " + "Life" + " at $" + ticketPriceNum + "?");
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                        View dialogView = inflater.inflate(R.layout.card_widget, null);
                        dialogBuilder.setView(dialogView);
                        final AlertDialog alertDialog = dialogBuilder.create();

                        final CardInputWidget mCardInputWidget = (CardInputWidget) dialogView.findViewById(R.id.card_input_widget);
                        Button defaultCC = (Button) dialogView.findViewById(R.id.default_cc);
                        Button next = (Button) dialogView.findViewById(R.id.next);
                        final TextView error = (TextView) dialogView.findViewById(R.id.errorMessage);

                        defaultCC.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mCardInputWidget.setCardNumber("4242424242424242");
                                mCardInputWidget.setCvcCode("123");
                                mCardInputWidget.setExpiryDate(12, 2020);
                            }
                        });

                        next.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                error.setVisibility(View.GONE);
                                if (mCardInputWidget.getCard() == null) {
                                    error.setText("Invalid Card Information");
                                    error.setVisibility(View.VISIBLE);
                                } else {
                                    card = new Card(

                                            mCardInputWidget.getCard().getNumber(), //card number
                                            mCardInputWidget.getCard().getExpMonth(),
                                            mCardInputWidget.getCard().getExpYear(),
                                            mCardInputWidget.getCard().getCVC()
                                    );
                                    validate(card);
                                    alertDialog.cancel();
                                }
                            }
                        });
                        alertDialog.show();

                        // insert stripe related properties here
                    }
                });
                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
                builder.show();
                /*PaymentDataRequest request = createPaymentDataRequest();
                if (request != null) {
                    AutoResolveHelper.resolveTask(
                            paymentsClient.loadPaymentData(request),
                            MainActivity.this,
                            LOAD_PAYMENT_DATA_REQUEST_CODE);
                    // LOAD_PAYMENT_DATA_REQUEST_CODE is a constant integer of your choice,
                    // similar to what you would use in startActivityForResult
                }*/
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        PaymentData paymentData = PaymentData.getFromIntent(data);
                        // You can get some data on the user's card, such as the brand and last 4 digits
                        CardInfo info = paymentData.getCardInfo();
                        // You can also pull the user address from the PaymentData object.
                        UserAddress address = paymentData.getShippingAddress();
                        // This is the raw JSON string version of your Stripe token.
                        String rawToken = paymentData.getPaymentMethodToken().getToken();

                        // Now that you have a Stripe token object, charge that by using the id
                        Token stripeToken = Token.fromString(rawToken);
                        if (stripeToken != null) {
                            // This chargeToken function is a call to your own server, which should then connect
                            // to Stripe's API to finish the charge.
                            //chargeToken(stripeToken.getId());
                            new StripeProcessing().execute(stripeToken);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        // Log the status for debugging
                        // Generally there is no need to show an error to
                        // the user as the Google Payment API will do that
                        break;
                    default:
                        // Do nothing.
                }
                break; // Breaks the case LOAD_PAYMENT_DATA_REQUEST_CODE
            // Handle any other startActivityForResult calls you may have made.
            default:
                // Do nothing.
        }
    }

    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, WIDTH, HEIGHT, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }

        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public void validate(Card card){
        boolean validation = card.validateCard();
        if (validation) {
            new Stripe(this).createToken(
                    card,
                    PUBLISHABLE_KEY,
                    new TokenCallback() {
                        @Override
                        public void onError(Exception error) {
                            Log.d("Stripe", error.toString());
                        }

                        @Override
                        public void onSuccess(Token token) {
                            new StripeProcessing().execute(token);
                        }
                    });
        } else if (!card.validateNumber()) {
            Log.d("Stripe", "The card number that you entered is invalid");
        } else if (!card.validateExpiryDate()) {
            Log.d("Stripe", "The expiration date that you entered is invalid");
        } else if (!card.validateCVC()) {
            Log.d("Stripe", "The CVC code that you entered is invalid");
        } else {
            Log.d("Stripe", "The card details that you entered are invalid");
        }
    }

    /*private void isReadyToPay() {
        IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();
        Task<Boolean> task = paymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(
                new OnCompleteListener<Boolean>() {
                    public void onComplete(Task<Boolean> task) {
                        try {
                            boolean result =
                                    task.getResult(ApiException.class);
                            if(result == true) {
                                //show Google as payment option
                            } else {
                                //hide Google as payment option
                            }
                        } catch (ApiException exception) { }
                    }
                });
    }

    private PaymentMethodTokenizationParameters createTokenizationParameters() {
        return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "stripe")
                .addParameter("stripe:publishableKey", "pk_test_1Tqu2RRf2owV3RCWuHV0whMY")
                .addParameter("stripe:version", "6.1.2")
                .build();
    }

    private PaymentDataRequest createPaymentDataRequest() {
        PaymentDataRequest.Builder request =
                PaymentDataRequest.newBuilder()
                        .setTransactionInfo(
                                TransactionInfo.newBuilder()
                                        .setTotalPriceStatus(WalletConstants.ENVIRONMENT_TEST)
                                        .setTotalPrice("10.00")
                                        .setCurrencyCode("CAD")
                                        .build())
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .setCardRequirements(
                                CardRequirements.newBuilder()
                                        .addAllowedCardNetworks(Arrays.asList(
                                                WalletConstants.CARD_NETWORK_AMEX,
                                                WalletConstants.CARD_NETWORK_DISCOVER,
                                                WalletConstants.CARD_NETWORK_VISA,
                                                WalletConstants.CARD_NETWORK_MASTERCARD))
                                        .build());

        request.setPaymentMethodTokenizationParameters(createTokenizationParameters());
        return request.build();
    }*/
}
