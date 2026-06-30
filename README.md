# WAGLO v2 — WhatsApp Group Link Organizer

[![Build](https://github.com/YOUR_USERNAME/YOUR_REPO/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR_USERNAME/YOUR_REPO/actions)

Production-ready Android app that passively reads WhatsApp public group invite links
visible on your screen and organises them with smart classification, search, and export.

## Architecture
```
UI Layer       : Activities + ViewBinding
ViewModel      : AndroidViewModel + LiveData + Coroutines
Repository     : GroupLinkRepository (single source of truth)
Engine         : LinkParser, LinkValidator, LinkNormalizer,
                 DuplicateDetector, LinkScoringEngine
Database       : SQLite via DatabaseHelper (v2, WAL, indexed)
Classifier     : Rule-based, 11 categories
```

## Build locally
```bash
chmod +x download-wrapper.sh && ./download-wrapper.sh
chmod +x gradlew
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

## Privacy
- No internet permission
- No messages sent or read
- No groups auto-joined
- All data stays on device (SQLite)
