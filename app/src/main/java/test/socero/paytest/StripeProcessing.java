package test.socero.paytest;

import android.os.AsyncTask;
import android.util.Log;

import com.stripe.Stripe;
import com.stripe.android.model.Token;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;
import com.stripe.model.Transfer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mahrus Kazi on 2018-05-31.
 */

public class StripeProcessing extends AsyncTask<Token, String, String>
{
    Token token;
    @Override
    protected String doInBackground(Token... token) {

        int price = 100*100;
        int theirCut = (int) (price - (price*0.035 + 30)) - 99;

        Stripe.apiKey = "sk_test_eTI4M4VjixNf2VzXD2jXKDmL";

        Map<String, Object> params = new HashMap<>();
        params.put("amount", price);
        params.put("currency", "cad");
        params.put("source", "tok_visa");
        params.put("receipt_email", "mahrus.kazi@gmail.com");
        params.put("transfer_group", "abcd");
        Charge charge = null;
        try {
            charge = Charge.create(params);
            Log.d("MainActivity", "Here: " + charge.getPaid());
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (InvalidRequestException e) {
            e.printStackTrace();
        } catch (APIConnectionException e) {
            e.printStackTrace();
        } catch (CardException e) {
            e.printStackTrace();
        } catch (APIException e) {
            e.printStackTrace();
        }

        // Create a Transfer to a connected account (later):
        Map<String, Object> transferParams = new HashMap<String, Object>();
        transferParams.put("amount", theirCut);
        transferParams.put("currency", "cad");
        transferParams.put("source_transaction", charge.getId());
        transferParams.put("destination", "acct_1CXyw2HwlN5TX0Mc");
        transferParams.put("transfer_group", "abcd");
        try {
            Transfer transfer = Transfer.create(transferParams);
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (InvalidRequestException e) {
            e.printStackTrace();
        } catch (APIConnectionException e) {
            e.printStackTrace();
        } catch (CardException e) {
            e.printStackTrace();
        } catch (APIException e) {
            e.printStackTrace();
        }

        return null;
    }
}
