# SmsGateway

## Deskripsi
SmsGateway adalah aplikasi Android berbasis Kotlin untuk mengirim dan menerima SMS, dengan integrasi webhook dan Firebase Cloud Messaging.

## Fitur
- Kirim SMS masuk ke Backend (webhook)
- Terima perintah kirim SMS dari Backend via FCM
- Kelola FCM Token
- Logging aktivitas
- Retry mekanisme untuk pengiriman SMS & webhook

## Build & Jalankan
1. Android Studio + JDK 17
2. Clone repo atau ekstrak ZIP
3. Buka di Android Studio
4. Konfigurasi WebhookPreferences
5. (Opsional) Tambah `google-services.json` untuk FCM
6. Jalankan di perangkat fisik atau emulator

## Struktur Data JSON
Lihat dokumentasi teknis untuk format SMS-INB, SMS-OUTB, SMS-OUTB-STATUS, FCM-TOKEN.

## Lisensi
Proprietary / internal use.
