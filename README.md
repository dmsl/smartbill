SmartBill
==========

SmartBill is an easy to use application that lets the user compile his/her grocery bill before proceeding to check-out. 
Also it keeps track of the price variation for a specific product from location to location using Google's geolocation mechanisms.

SmartBill uses the ZXing Library for the barcode scanning. (https://code.google.com/p/zxing/)

Source Authors:
===============

+ Kyriakos Georgiou
+ Kyriakos Frangeskos

+ Authors of the ZXing Barcode Scanner library (https://code.google.com/p/zxing/)

Affiliation:
============

Affiliation:#
Data Management Systems Laboratory #
Dept. of Computer Science #
University of Cyprus #
P.O. Box 20537 #
1678 Nicosia, CYPRUS #
Web: http://dmsl.cs.ucy.ac.cy/ #
Email: dmsl@cs.ucy.ac.cy #
Tel: +357-22-892755 #
Fax: +357-22-892701 #

How to use SmartBill:
=====================

You should deploy the android application (or .apk) on any smartphone with Android OS and a with a camera. 
The application is compatible with all the versions of Android OS.

+ Step  1: 
Scan a barcode through the scanner's rectangle (or you can manually enter a barcode through the application's menu).
+ Step 2:
If the product was scanned before it will be added directly to the shopping list and to the grocery bill. If it's a new product, SmartBill will attempt to retrieve its name through the Google API for Shopping and then ask the user for its price.
In case the product was not matched in Google's API for Shopping database, the user will be prompted to manually enter the product's name.
+ Step 3:
The products on the shopping list can be edited at any time by long-presssing them on the list.
+ Step 4:
SmartBill keeps track of previous locations a product was scanned, the user can see those locations and their respective prices by clicking the "View Full Map" button on the bottom right corner.
+ Step 5:
The application is terminated by pressing the "Back" button or through the Menu. The current shopping list and grocery bill will be lost but the scanned products will be permanently saved in the local database.


Source Code Notes:
==================

+ 1:
The API-Key for Google API for Shopping is missing, you will need to retrieve your own and place it in the GoogleShoppingAPIHeler.java.
Instructions for obtaining the API-Key: https://developers.google.com/shopping-search/v1/getting_started#getting-started

+ 2:
The API Key for Google Maps is missing, you will need to retrieve your own and place it in the capture.xml and map.xml files.
Instructions for obtaining the API-Key: https://developers.google.com/maps/documentation/android/mapkey

Bug Reporting:
==============

Please report any bugs you come across at:
+ Kyriakos Georgiou - kgeorg10@cs.ucy.ac.cy
or
+ Kyriakos Frangeskos - kfrang01@cs.cy.ac.cy

Contact Info:
=============

+ Kyriakos Georgiou - kgeorg10@cs.ucy.ac.cy
+ Kyriakos Frangeskos - kfrang01@cs.cy.ac.cy
+ DSML - http://dmsl.cs.ucy.ac.cy/#contact
