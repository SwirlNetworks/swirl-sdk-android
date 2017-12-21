package com.swirl.swirlx.sample_integrations;

import android.content.Context;
import android.util.Log;

import com.swirl.API;
import com.swirl.API.Completion;
import com.swirl.Content;
import com.swirl.ContentManager;
import com.swirl.Error;
import com.swirl.MainLoop;
import com.swirl.SafeRunnable;
import com.swirl.SwirlListener;
import com.swirl.Util;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Tom on 8/31/17.
 */

public class KouponMediaListener extends SwirlListener {
    private String      offerViewerURL;
    private Context     context;
    private String      apiKey;
    private String      apiSecret;
    private String      user;

    private static int      KM_SUCCESS              = 200;
    private static String   KM_BASE_URL             = "https://consumer.sandbox1.kouponmedia.com/v2";
    //private static String   KM_DEFAULT_VIEWER_URL   = "https://offer-sandbox1.kou.pn/OfferViewer/Redirect.aspx?property_code=swirl_mobilecapture&offers=%s&primary=%s";
    private static String   KM_DEFAULT_VIEWER_URL   = "https://offer-sandbox1.kou.pn/OfferViewer/Redirect.aspx?property_code=swirl_mobilecapture&offers=%s";

    public KouponMediaListener(Context context, String apiKey, String apiSecret, String user) {
        this.context = context;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.user = user;
        this.offerViewerURL = KM_DEFAULT_VIEWER_URL;
    }

    public void setOfferViewerURL(String url) {
        offerViewerURL = url;
    }

    public String getOfferViewerURL() {
        return offerViewerURL;

    }
    public String getOfferViewerURL(String offer) {
        return String.format(offerViewerURL, offer, user);
    }

    private boolean isOfferValid(JSONObject offer) {
        return true;
    }

    private boolean doesOfferHaveIdentifier(JSONObject offer, String identifier) {
        return (offer.optString("offerId", "").equals(identifier));
    }

    // =============================================================================================
    // km request and helpers
    // =============================================================================================

    private void kmRequest(String requestURL, JSONObject body, Completion completion) {
        long timestamp = System.currentTimeMillis()/1000L;
        requestURL = requestURL.concat(String.format("%stimestamp=%d&identifier=%s", requestURL.endsWith("?") ? "" : "&", timestamp, apiKey));
        requestURL = requestURL.concat(String.format("&authSignature=%s", Util.hexdigest(Util.hmac_sha1(requestURL, apiSecret))));
        API.getInstance().makeRequest(requestURL, body, completion);
    }

    private void kmAddOffer(final String offer, final Completion completion) {
        kmRequest(String.format("%s/%s/offers/%s?", KM_BASE_URL, user, offer), new JSONObject(), new Completion() {
            public void completion(int error, JSONObject response) {
                if (response.optInt("code", Error.ERROR_UNKNOWN) == KM_SUCCESS) {
                    kmRequest(String.format("%s/%s/offers?", KM_BASE_URL, user), null, new Completion() {
                        public void completion(int error, JSONObject response) {
                            if (response.optInt("code", Error.ERROR_UNKNOWN) == KM_SUCCESS) {
                                JSONArray offers = response.optJSONArray("Offers");
                                for (int index = 0; index < offers.length(); index++) {
                                    JSONObject info = offers.optJSONObject(index);
                                    if (doesOfferHaveIdentifier(info, offer) && isOfferValid(info)) {
                                        completion.complete(0, info);
                                        return;
                                    }
                                }
                                completion.complete(Error.ERROR_NOT_FOUND);
                            } else
                                completion.complete(Error.ERROR_UNKNOWN);
                        }
                    });
                } else
                    completion.complete(Error.ERROR_UNKNOWN);
            }
        });
    }

    /*
     * Swirl event callbacks
     */

    // TESTING ONLY
//    protected void onStarted() {
//        MainLoop.getHandler().postDelayed(new SafeRunnable() {
//            @Override public void safeRunThrows() throws Throwable {
//                String json = "{\"type\":\"custom\",\"impression_id\":\"1\",\"attributes\":{\"custom_type\":\"kouponmedia\",\"offer_id\":\"15731\"}}";
//                ContentManager.getInstance().onReceiveContent(new Content(context, new JSONObject(json), null, false));
//            }
//        }, 10 * 1000L);
//    }

    private Content contentFromContentAndOffer(Content content, String offer, JSONObject offerInfo) {
        try {
            JSONObject info = new JSONObject()
                    .put("type", "swirl")
                    .put("impression_id", content.getIdentifier())
                    .put("url", getOfferViewerURL(offer))
                    .put("notification", new JSONObject()
                        .put("title",    offerInfo.optString("offerTitle", ""))
                        .put("subtitle", offerInfo.optString("offerSubtitle", ""))
                        .put("text",     offerInfo.optString("offerDisclaimer", "")));

            return new Content(context, info, content.getVisit(), content.isFromNotification());
        } catch (Throwable t) {
            Log.e("KM", Log.getStackTraceString(t));
        }
        return null;
    }

    protected void onReceiveContentCustom(ContentManager manager, final Content content, final com.swirl.Completion completion) {
        final String offer;

        if (!content.getCustomType().equals("koupon_media") ||  (offer = content.getAttributes().optString("offer_id", null)) == null)
            return;

        kmAddOffer(offer, new API.Completion() {
            public void completion(int error, JSONObject response) {
                if (error == 0 && response != null) {
                    completion.complete(0, contentFromContentAndOffer(content, offer, response));
                } else
                    completion.complete(error);
            }
        });
    }
}
