# UR Call

Native Kotlin/Compose app. Presence + WebRTC voice calling, exclusive sa app na 'to lang.

## Bago mo i-build (importante, 2 hakbang na hindi ko kasama sa zip)

**1. Palitan mo ang `app/google-services.json`**
Placeholder lang yung nandito ngayon, hindi 'to totoong Firebase project — babagsak yung
build/login pag hindi mo pinalitan. Gawa ka ng bagong Firebase project (console.firebase.google.com),
i-enable ang **Realtime Database** at **Authentication (Anonymous)**, i-download yung
`google-services.json` doon, tapos palitan yung nasa `app/` folder.

**2. (Optional pero kailangan para 100% gumana kahit anong network) Maglagay ng TURN server**
Buksan `app/src/main/java/com/urcall/app/webrtc/WebRTCClient.kt`, hanapin yung
`TURN_SERVER_URL / TURN_USERNAME / TURN_PASSWORD` sa taas — dito mo ilalagay credentials
mo galing sa libreng TURN provider (hal. metered.ca, may free 20GB/month tier). Kung
walang TURN, may STUN pa rin (Google, libre) pero minsan hindi maka-connect kapag parehong
strict ang network ng dalawang tumatawag.

## Paano i-build (via GitHub Actions, kagaya ng workflow mo sa UR Scanner)
1. I-unzip, i-push sa bagong GitHub repo mo.
2. Kukuha ng APK sa Actions tab pagkatapos ng push sa `main` branch — check "Artifacts".

## Ano ang kasama dito (v1)
- Contacts screen na hawig sa design mo (numbered list, glowing call pill, +, bottom nav)
- Add contact screen (sa pamamagitan ng "URCall ID")
- Presence detection (`PresenceManager.kt`) — real-time "online kapag naka-on ang data"
- Signaling gamit Firebase Realtime Database (`SignalingManager.kt`) — walang kailangang
  hiwalay na server
- WebRTC voice call engine (`WebRTCClient.kt`)
- Call screen (basic — end call button gumagana na)

## Susunod na idadagdag (hindi pa kasama, sabihin mo lang kailan tayo susunod)
- Incoming call notification/ringing screen (ngayon, "silent" pa lang pag tinawagan ka)
- Contact list na galing na sa totoong Firebase data (hardcoded pa yung 5 sample names)
- Mute / speaker toggle sa call screen
- Background service para tumunog pa rin kahit naka-minimize ang app
