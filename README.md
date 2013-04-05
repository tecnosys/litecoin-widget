It's called "Litecoin Widget", but it's been stretching that definition lately . The most recent version is 9, released on 2013-04-03.

The widget will show you prices for BTC, LTC, NMC, and PPC. Prices come mostly from vircurex and btc-e, but also mtgox and bitfloor for BTC.

Prices are quoted in terms of BTC (for alt coins), and the old-world currency of your choice (for all coins).

Many things are estimated, since not all of these pairs trade and I am too lazy to implement direct API calls for umpteen different currencies. Anytime a price was estimated, it is marked with an asterisk. Please be aware, these numbers could be based on cached information even if you refresh. Old-world currency rates are cached for up to 1 hour. The BTC/USD exchange rate is cached for up to five minutes. Also, accuracy is limited as the BTC rate differs across exchanges, and the widget will choose whichever cached BTC rate it can find (that's at least 5 minutes fresh) in order to reduce extra network traffic.

It's on the app store as "Litecoin Widget" by The Mad Geniuses.

https://play.google.com/store/apps/details?id=org.phauna.litecoinwidget&feature=nav_result#?t=W251bGwsMSwxLDMsIm9yZy5waGF1bmEubGl0ZWNvaW53aWRnZXQiXQ..

Source is freely available here:

https://github.com/ogunden/litecoin-widget

Please post feature requests, bugs, etc here:

https://bitcointalk.org/index.php?topic=158777.msg1677458#msg1677458

Forks/pull requests welcome.

Donations also welcome :
LTC: La4tLJuU3EJnuvSR2KhjgBREwGXXVHKcS2
BTC: 1PBiV4z3VX21U3few3N1JeiuA5p4fvJU5q

Enjoy!

new in version 9:
- support for estimated values in 8 different old-world currencies, courtesy of rate-exchange.appspot.com. old-world currency exchange rates are cached for up to an hour.
- ability to choose between three text colors; hopefully this helps those who were having trouble seeing text on a light background
- fix crash in some old versions of android (due to SharedPreferences.Editor.apply)

new in version 8:
- when exchange is unreachable, continue to display previous result (and time) instead of overwriting with 0
- add estimated dollar amounts to coins which don't trade in dollars. If a price is an estimate, it is marked with an asterisk

new in version 7:
- fiiiiiine.. added mtgox support >:-)

new in version 6:
- when you click, only refresh the widget you clicked on (previously it refreshed all widgets)
- add support for namecoin and ppcoin
- fixed crash when api calls to the exchanges timed out or otherwise failed
- network updates moved to background thread
- shorter exchange names in widget (e.g. v'rex), making for prettier, more consistent coin graphi
