package dk.aau.netsec.hostage.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import dk.aau.netsec.hostage.R;

/**
 * Shows Privacy information
 * Added as a part for fix in Android 11
 *
 * @author Shreyas Srinivasa
 * Created on 01-03-2021
 */
public class PrivacyFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        final Activity activity = getActivity();
        if (activity != null) {
            activity.setTitle(getResources().getString(R.string.privacy_policy));
        }

        View rootView = inflater.inflate(R.layout.fragment_privacy, container, false);

        String policyText = "<p>Last updated: July 20, 2021</p>\n" +
                "<!DOCTYPE html>\n" +
                "    <html>\n" +
                "    <head>\n" +
                "      <meta charset='utf-8'>\n" +
                "      <meta name='viewport' content='width=device-width'>\n" +
                "    </head>\n" +
                "    <body>\n" +
                "    <strong>Privacy Policy</strong> <p>\n" +
                <<<<<<<HEAD
        "                  Aalborg University, Denmark built the HosTaGe app as\n" +
                "                  an Open Source app. This SERVICE is provided by\n" +
                "                  Aalborg University, Denmark at no cost and is intended for use as\n" +
                "                  is.\n" +
                "                </p> <p>\n" +
                "                  This page is used to inform visitors regarding our\n" +
                "                  policies with the collection, use, and disclosure of Personal\n" +
                "                  Information if anyone decided to use our Service.\n" +
                "                </p> <p>\n" +
                "                  If you choose to use our Service, then you agree to\n" +
                "                  the collection and use of information in relation to this\n" +
                "                  policy. The Personal Information that we collect is\n" +
                "                  used for providing and improving the Service. We will not use or share your information with\n" +
                "                  anyone except as described in this Privacy Policy.\n" +
                "                 </p> <p>\n" +
                "                  The users can consent for the following:\n" +
                "                  <ul> \n " +
                "                   <li> The right to be forgotten – to get your data deleted. The right to have incorrect data corrected. </li> \n " +
                "                   <li> The right to know what data a controller have on you. </li> \n " +
                "                   <li> The right to have your data shared with another organization, if requested be the subject. </li> \n " +
                "                   <li> The right to not be profiled, the right to object.</li> \n " +
                "                  </ul> \n " +
                "                </p> <p>\n" +
                "                  The terms used in this Privacy Policy have the same meanings\n" +
                "                  as in our Terms and Conditions, which is accessible at\n" +
                "                  HosTaGe unless otherwise defined in this Privacy Policy.\n" +
                "                  The Privacy Policy of Aalborg University can be accessed at https://www.en.aau.dk/privacy-policy-cookies#370625\n" +
                "                </p> <p><strong>Information Collection and Use</strong></p> <p>\n" +
                "                  For a better experience, while using our Service, we\n" +
                "                  may require you to provide us with certain personally\n" +
                "                  identifiable information. The information that\n" +
                "                  we request will be retained by us and used as described in this privacy policy.\n" +
                "                </p> <div><p>\n" +
                "                    On app startup, we ask for user consent for accessing the location permission. \n" +
                "                    This information is required to access the network related information, necessary for the app main functionality.\n" +
                "                  </p> <p>\n" +
                "                </p> <div><p>\n" +
                "                    The app does use third party services that may collect\n" +
                "                    information used to identify you.\n" +
                "                  </p> <p>\n" +
                "                    Link to privacy policy of third party service providers used\n" +
                "                    by the app\n" +
                "                  </p> <ul><li><a href=\"https://www.google.com/policies/privacy/\" target=\"_blank\" rel=\"noopener noreferrer\">Google Play Services</a></li><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----><!----></ul></div> <p><strong>Log Data</strong></p> <p>\n" +
                "                  We want to inform you that whenever you\n" +
                "                  use our Service, in a case of an error in the app\n" +
                "                  we collect data and information (through third party\n" +
                "                  products) on your phone called Log Data. This Log Data may\n" +
                "                  include information such as your device Internet Protocol\n" +
                "                  (“IP”) address, device name, operating system version, the\n" +
                "                  configuration of the app when utilizing our Service,\n" +
                "                  the time and date of your use of the Service, and other\n" +
                "                  statistics. The logs will be purged periodically every 3 months.\n" +
                "                </p> <p><strong>Cookies</strong></p> <p>\n" +
                "                  Cookies are files with a small amount of data that are\n" +
                "                  commonly used as anonymous unique identifiers. These are sent\n" +
                "                  to your browser from the websites that you visit and are\n" +
                "                  stored on your device's internal memory.\n" +
                "                </p> <p>\n" +
                "                  This Service does not use these “cookies” explicitly. However,\n" +
                "                  the app may use third party code and libraries that use\n" +
                "                  “cookies” to collect information and improve their services.\n" +
                "                  You have the option to either accept or refuse these cookies\n" +
                "                  and know when a cookie is being sent to your device. If you\n" +
                "                  choose to refuse our cookies, you may not be able to use some\n" +
                "                  portions of this Service.\n" +
                "                </p> <p><strong>Service Providers</strong></p> <p>\n" +
                "                  We may employ third-party companies and\n" +
                "                  individuals due to the following reasons:\n" +
                "                </p> <ul><li>To facilitate our Service;(Google Services for hosting the app information)</li> <li>To provide the Service on our behalf;</li> <li>To perform Service-related services; or</li> <li>To assist us in analyzing how our Service is used.</li></ul> <p>\n" +
                "                  We want to inform users of this Service\n" +
                "                  that these third parties have access to your Personal\n" +
                "                  Information. The reason is to perform the tasks assigned to\n" +
                "                  them on our behalf. However, they are obligated not to\n" +
                "                  disclose or use the information for any other purpose.\n" +
                "                </p> <p><strong>Security</strong></p> <p>\n" +
                "                  We value your trust in providing us your\n" +
                "                  Personal Information, thus we are striving to use commercially\n" +
                "                  acceptable means of protecting it. But remember that no method\n" +
                "                  of transmission over the internet, or method of electronic\n" +
                "                  storage is 100% secure and reliable, and we cannot\n" +
                "                  guarantee its absolute security.\n" +
                "                </p> <p><strong>Links to Other Sites</strong></p> <p>\n" +
                "                  This Service may contain links to other sites. If you click on\n" +
                "                  a third-party link, you will be directed to that site. Note\n" +
                "                  that these external sites are not operated by us.\n" +
                "                  Therefore, we strongly advise you to review the\n" +
                "                  Privacy Policy of these websites. We have\n" +
                "                  no control over and assume no responsibility for the content,\n" +
                "                  privacy policies, or practices of any third-party sites or\n" +
                "                  services.\n" +
                "                </p> <p><strong>Children’s Privacy</strong></p> <p>\n" +
                "                  These Services do not address anyone under the age of 13.\n" +
                "                  We do not knowingly collect personally\n" +
                "                  identifiable information from children under 13 years of age. In the case\n" +
                "                  we discover that a child under 13 has provided\n" +
                "                  us with personal information, we immediately\n" +
                "                  delete this from our servers. If you are a parent or guardian\n" +
                "                  and you are aware that your child has provided us with\n" +
                "                  personal information, please contact us so that\n" +
                "                  we will be able to do necessary actions.\n" +
                "                </p> <p><strong>Changes to This Privacy Policy</strong></p> <p>\n" +
                "                  We may update our Privacy Policy from\n" +
                "                  time to time. Thus, you are advised to review this page\n" +
                "                  periodically for any changes. We will\n" +
                "                  notify you of any changes by posting the new Privacy Policy on\n" +
                "                  this page.\n" +
                "                </p> <p>This policy is effective as of 2021-03-08</p> <p><strong>Contact Us</strong></p> <p>\n" +
                "                  If you have any questions or suggestions about our\n" +
                "                  Privacy Policy, do not hesitate to contact us at hostage@es.aau.dk.\n" +
                "Data subjects have the right to submit a complaint about how their personal data is processed\n" +
                "to the Danish Data Protection Agency at dt@datatilsynet.dk or by post to Datatilsynet/the \n" +
                "Danish Data Protection Agency, Borgergade 28, 5., 1300 Copenhagen K.\n" +
                "Please contact our Data Protection Officer if you have any question about the processing of \n" +
                "your personal data at dpo@aau.dk\n" +
                "However, before contacting the Danish Data Protection Agency, we recommend that you \n" +
                "contact Aalborg University’s data protection officer who may be able to solve the matter.\n" +
                =======
        "                  Aalborg University, Denmark built the HosTaGe app as" +
                "                  an Open Source app. This SERVICE is provided by" +
                "                  Aalborg University, Denmark at no cost and is intended for use as" +
                "                  is." +
                "                </p> <p>" +
                "                  This page is used to inform visitors regarding our" +
                "                  policies with the collection, use, and disclosure of Personal" +
                "                  Information if anyone decided to use our Service." +
                "                </p> <p>" +
                "                  If you choose to use our Service, then you agree to" +
                "                  the collection and use of information in relation to this" +
                "                  policy. The Personal Information that we collect is" +
                "                  used for providing and improving the Service. We will not use or share your information with" +
                "                  anyone except as described in this Privacy Policy." +
                "                 </p> <p>" +
                "                  The users can consent for the following:" +
                "                  <ul>  " +
                "                   <li> The right to be forgotten – to get your data deleted. The right to have incorrect data corrected. </li>  " +
                "                   <li> The right to know what data a controller have on you. </li>  " +
                "                   <li> The right to have your data shared with another organization, if requested be the subject. </li>  " +
                "                   <li> The right to not be profiled, the right to object.</li>  " +
                "                  </ul>  " +
                "                </p> <p>" +
                "                  The terms used in this Privacy Policy have the same meanings" +
                "                  as in our Terms and Conditions, which is accessible at" +
                "                  HosTaGe unless otherwise defined in this Privacy Policy." +
                "                  The Privacy Policy of Aalborg University can be accessed at <a href=\"https://www.en.aau.dk/privacy-policy-cookies#370625\" target=\"_blank\" rel=\"noopener noreferrer\">https://www.en.aau.dk/privacy-policy-cookies#370625</a> " +
                "                </p> <p><strong>Information Collection and Use</strong></p> <p>" +
                "                  For a better experience, while using our Service, we" +
                "                  may require you to provide us with certain personally" +
                "                  identifiable information. The information that" +
                "                  we request will be retained by us and used as described in this privacy policy." +
                "                </p> <div><p>" +
                "                    On app startup, we ask for user consent for accessing the location permission. " +
                "                    This information is required to access the network related information, necessary for the app main functionality." +
                "                  </p> <p>" +
                "                </p> <div><p>" +
                "                    The app does use third party services that may collect" +
                "                    information used to identify you." +
                "                  </p> <p>" +
                "                    Link to privacy policy of third party service providers used" +
                "                    by the app" +
                "                  </p> <ul><li> <a href=\"https://www.google.com/policies/privacy/\" target=\"_blank\" rel=\"noopener noreferrer\">Google Play Services</a></li></ul></div>" +
                "                  <p><strong>Log Data</strong></p> <p>" +
                "                  We want to inform you that whenever you" +
                "                  use our Service, in a case of an error in the app" +
                "                  we collect data and information (through third party" +
                "                  products) on your phone called Log Data. This Log Data may" +
                "                  include information such as your device Internet Protocol" +
                "                  (“IP”) address, device name, operating system version, the" +
                "                  configuration of the app when utilizing our Service," +
                "                  the time and date of your use of the Service, and other" +
                "                  statistics. The logs will be purged periodically every 3 months." +
                "                </p> <p><strong>Cookies</strong></p> <p>" +
                "                  Cookies are files with a small amount of data that are" +
                "                  commonly used as anonymous unique identifiers. These are sent" +
                "                  to your browser from the websites that you visit and are" +
                "                  stored on your device's internal memory." +
                "                </p> <p>" +
                "                  This Service does not use these “cookies” explicitly. However," +
                "                  the app may use third party code and libraries that use" +
                "                  “cookies” to collect information and improve their services." +
                "                  You have the option to either accept or refuse these cookies" +
                "                  and know when a cookie is being sent to your device. If you" +
                "                  choose to refuse our cookies, you may not be able to use some" +
                "                  portions of this Service." +
                "                </p> <p><strong>Service Providers</strong></p> <p>" +
                "                  We may employ third-party companies and" +
                "                  individuals due to the following reasons:" +
                "                </p> <ul><li>To facilitate our Service;(Google Services for hosting the app information)</li> <li>To provide the Service on our behalf;</li> <li>To perform Service-related services; or</li> <li>To assist us in analyzing how our Service is used.</li></ul> <p>" +
                "                  We want to inform users of this Service" +
                "                  that these third parties have access to your Personal" +
                "                  Information. The reason is to perform the tasks assigned to" +
                "                  them on our behalf. However, they are obligated not to" +
                "                  disclose or use the information for any other purpose." +
                "                </p> <p><strong>Security</strong></p> <p>" +
                "                  We value your trust in providing us your" +
                "                  Personal Information, thus we are striving to use commercially" +
                "                  acceptable means of protecting it. But remember that no method" +
                "                  of transmission over the internet, or method of electronic" +
                "                  storage is 100% secure and reliable, and we cannot" +
                "                  guarantee its absolute security." +
                "                </p> <p><strong>Links to Other Sites</strong></p> <p>" +
                "                  This Service may contain links to other sites. If you click on" +
                "                  a third-party link, you will be directed to that site. Note" +
                "                  that these external sites are not operated by us." +
                "                  Therefore, we strongly advise you to review the" +
                "                  Privacy Policy of these websites. We have" +
                "                  no control over and assume no responsibility for the content," +
                "                  privacy policies, or practices of any third-party sites or" +
                "                  services." +
                "                </p> <p><strong>Children’s Privacy</strong></p> <p>" +
                "                  These Services do not address anyone under the age of 13." +
                "                  We do not knowingly collect personally" +
                "                  identifiable information from children under 13 years of age. In the case" +
                "                  we discover that a child under 13 has provided" +
                "                  us with personal information, we immediately" +
                "                  delete this from our servers. If you are a parent or guardian" +
                "                  and you are aware that your child has provided us with" +
                "                  personal information, please contact us so that" +
                "                  we will be able to do necessary actions." +
                "                </p> <p><strong>Changes to This Privacy Policy</strong></p> <p>" +
                "                  We may update our Privacy Policy from" +
                "                  time to time. Thus, you are advised to review this page" +
                "                  periodically for any changes. We will" +
                "                  notify you of any changes by posting the new Privacy Policy on" +
                "                  this page." +
                "                </p> <p>This policy is effective as of 2021-03-08</p> <p><strong>Contact Us</strong></p> <p>" +
                "                  If you have any questions or suggestions about our" +
                "                  Privacy Policy, do not hesitate to contact us at hostage@es.aau.dk." +
                "Data subjects have the right to submit a complaint about how their personal data is processed" +
                "to the Danish Data Protection Agency at dt@datatilsynet.dk or by post to Datatilsynet/the " +
                "Danish Data Protection Agency, Borgergade 28, 5., 1300 Copenhagen K." +
                "Please contact our Data Protection Officer if you have any question about the processing of " +
                "your personal data at dpo@aau.dk" +
                "However, before contacting the Danish Data Protection Agency, we recommend that you " +
                "contact Aalborg University’s data protection officer who may be able to solve the matter." +
                >>>>>>>ver - 2.4 (major)
                "\n" +
                "    </body>\n" +
                "    </html>\n";

        TextView policy = rootView.findViewById(R.id.policyText);
        policy.setText(HtmlCompat.fromHtml(policyText, 0));
<<<<<<<HEAD


        return rootView;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootView != null) {
            unbindDrawables(rootView);
            rootView = null;

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (rootView != null) {
            unbindDrawables(rootView);
            rootView = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (rootView != null) {
            unbindDrawables(rootView);
            rootView = null;
        }
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }
=======
        policy.setMovementMethod(LinkMovementMethod.getInstance());

        return rootView;
}
>>>>>>>ver-2.4(major)
        }
