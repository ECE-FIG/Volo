# Volo! - An Android App
This idea spawned from HackTX 2019 - hosted by Freetail Hackers @The University of Texas at Austin - with Anqing Chen, Jianchen Gu, and Nicholas Chu

### Features
- Lets certified non-profit organizations add volunteering events to a map for everyone to know
- Supported by MongoDB Stitch backend to keep all the events synced
- Quick zoom to your current location
- Google Maps Integration for navigation from current location (Accessed by long pressing address)

### Dependencies
- Mapbox SDK and its Annotation Plugin for map displays
- [android-floating-action-button](https://github.com/Clans/FloatingActionButton "android-floating-action-button") library by Jerzy Chalupski
- MongoDB Database for Realtime Database and Authentication

### Future Plans
- Polish UX/UI
- Third party auth
- Ability to add event from current and selected location
- Auto search web for events and add them to server periodically
- Filter events by start and stop time

### Known Bugs
- Handling multiple events at one location (Events overlap each other)
