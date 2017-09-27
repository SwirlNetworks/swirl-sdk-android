# Swirl Proximity Library
The Swirl platform is designed as a complete proximity platform detecting a variety of proximity signals like geofence, wifi and beacons and managing presence tracking, notification and content delivery.  The Swirl library is a component of this system which enables signal detection and content delivery on mobile devices.

![](./docs/images/sdk3-overview.png)

### Features
* Simple Interface, Easy to integrate
* Small footprint, native implementations
* Geofence: circular and arbitrary polygons
* Wifi: Enables access points to be used like beacons or geofences
* Beacon: SecureCast™, Eddystone (URL, UID, EID, TLM) and iBeacon, easy to extend
* Content: Support for interstitials, deep links and custom content types (optional)

## Documentation and Resources
There are a number of additional documents and resources available to facilitate the integration and use of the Swirl Proximity Library.

#### For Implementation
* [Swirl Proximity Developer Guide for Android](./docs/swirl-developer-guide-android.md)
* [Swirl Proximity Library Reference for Android](https://swirlnetworks.github.io/swirl-sdk-android/docs/reference-guide/index.html)
* [Example Source Code for Android](./examples/)

#### For Testing
* [BeaconManager in the Play Store](https://play.google.com/store/apps/details?id=com.swirl.configurator) which can be used to configure and deploy beacons.
* [Explorer in the Play Store](https://play.google.com/store/apps/details?id=com.swirl.demo) which can be used for testing signals and content.
* [Customer Supoprt: support@swirl.com](mailto:support@swirl.com) 
  Support and Testing Services. Swirl appreciates publishers willing to share their integrated application for Swirl testing. This is an added service that we provide you to ensure that your app has been properly integrated with our SDK. Please contact your Swirl Account Manager to schedule this testing.

## Release Notes
**Version:** `3.5`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Date:** `09/15/17`
<p>
Bug fixes, custom event streaming and custom content integrations and samples.

**Version:** `3.4` &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Date:** `05/15/17`
<p>
Improved handling and control for apps with No Title/No Actionbar themes.  Seamless Google proximity beacon API support.

**Version:** `3.3` &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Date:** `02/01/17`
<p>
Replaced up-front device registration with just-in-time device registration (scalability).

**Version:** `3.2.2` &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Date:** `01/12/17`
<p>
Minor bug fixes.

**Version:** `3.2.1` &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Date:** `01/10/17`
<p>
Minor bug fixes.

**Version:** `3.2` &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Date:** `01/06/17`
<p>
Adds support for realtime streaming or events; improved support for eddystone; added improved security for API requests; added support for enter/exit zone signals, other bug fixes.  

**Version:** `3.1` &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Date:** `10/21/16`
<p>
Support for Google Nearby APIs (NearbyManager) added to library and examples.

**Version:** `3.0` &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Date:** `9/22/16`
<p>
This is the initial release of **Swirl-Release.aar** which represents a completely rearchitected and rewritten code base for Android.

## License
We have included our standard commercial [license](LICENSE.md), but this repository is public currently for technical reasons ONLY and its accessibility should not be considered a grant of any rights.  Please see [Swirl](https://www.swirl.com) for more details or email us at [sales@swirl.com](mailto:sales@swirl.com) if you are interested in using our products.

