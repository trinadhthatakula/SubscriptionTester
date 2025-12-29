# Play Console Setup Guide 🛠️

> **STOP:** Do not write a single line of code until you have completed this setup. Your app will always return `Item Unavailable` if these steps are incomplete.

## 1. Create App & Products

Before you can test, the Play Store needs to know what you are selling.

### A. Create the App

1. Go to [Google Play Console](https://play.google.com/console).

2. Create a new App (or select an existing one).

3. **Important:** The `applicationId` in your `build.gradle` **MUST** match the package name here exactly.

### B. Create Subscription (Recurring)

1. Navigate to **Monetize > Products > Subscriptions**.

2. Click **Create subscription**.

3. **Product ID:** `premium_sub` (Must match code).

4. Create a **Base Plan** (e.g., Monthly, Auto-renewing).

5. Set a price and **Activate** the base plan.

### C. Create In-App Product (One-Time)

1. Navigate to **Monetize > Products > In-app products**.

2. Click **Create product**.

3. **Product ID:** `premium_user` (Must match code).

4. Set Price and click **Save** & **Activate**.

## 2. The Testing Environment

> [!WARNING]
> You cannot test billing using a standard "Debug" build signed with the default Android keystore.

### Upload a Signed Build

Google Play requires a signed APK/AAB to verify identity.

1. Generate a Signed Bundle using your **Release Keystore**.

2. Go to **Testing > Internal testing**.

3. Create a new release and upload your signed AAB.

4. **Note:** You do NOT need to submit for review. Just upload and save.

## 3. Granting Access (CRITICAL) 💀

**This is where 99% of developers get stuck.**

### A. Add Email to Testers List

1. In **Internal testing**, click the **Testers** tab.

2. Create an email list and add your Google account (the one on your phone).

3. Save changes.

### B. THE MAGIC LINK (Required)

Look for the "How testers join your test" section on the Testers tab.

1. Copy the **"Join on Web"** link.

2. Open it on your phone or PC.

3. Click the **ACCEPT INVITE** button.

> **Failure to do this will result in the Play Store API returning an empty list.**

## 4. License Testing Setup

Configure your account to make "fake" purchases without charging your credit card.

1. Go to **Setup > License testing** (Left sidebar).

2. Add your email address to the "License testers" list.

3. Set **License test response** to `RESPOND_NORMALLY`.

4. Save changes.

*Now, when you buy an item in the app, the payment sheet will say "Test Card, always approves".*

## ✅ Final Pre-Flight Checklist

- [ ] App ID matches `build.gradle`

- [ ] APK is signed with Release Key

- [ ] APK is uploaded to Internal Testing track

- [ ] Product IDs match exactly (`premium_sub`)

- [ ] **I have clicked "Accept Invite" on the web link**